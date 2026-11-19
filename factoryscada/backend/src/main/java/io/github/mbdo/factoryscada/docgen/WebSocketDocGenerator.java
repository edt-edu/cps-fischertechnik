package io.github.mbdo.factoryscada.docgen;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Handle;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import static io.github.mbdo.factoryscada.docgen.DocGenSupport.*;

import io.github.mbdo.factoryscada.frontend.WebSocketDoc;
import io.github.mbdo.factoryscada.frontend.WebSocketPublish;
import lombok.extern.slf4j.Slf4j;

/**
 * Generates an AsciiDoc documentation of the WebSocket (STOMP) protocol exposed by this backend.
 * <p>
 * Runs once at application startup (as an {@link ApplicationRunner}) and introspects the Spring
 * application context to collect:
 * <ul>
 *   <li><b>Incoming topics</b>: methods of {@link Controller} beans annotated with {@link MessageMapping}
 *       (the class-level {@code @MessageMapping} prefix is prepended), with their parameters,
 *       request payload and optional {@link SendTo} response;</li>
 *   <li><b>Outgoing topics</b>: methods of {@link Component} beans annotated with {@link WebSocketPublish},
 *       and {@code @MessageMapping} methods that also declare a {@link SendTo} destination.</li>
 * </ul>
 * Payload types are documented as JSON schemas. The document starts with an overview table
 * linking each implementing method to its incoming and outgoing topics.
 * <p>
 * The result is written to {@code docgen/websocket-endpoints-topics.adoc} (relative to the working directory).
 * <p>
 * The output is deterministic for a given application: beans, methods, overview entries and JSON schema
 * properties are all sorted, so that regenerating the file only produces git differences when the
 * protocol actually changes (reflection and bean scanning order are otherwise not stable across builds).
 * <p>
 * The bytecode of each {@code @MessageMapping} method is scanned (with Spring's bundled ASM) to detect direct
 * calls to {@link WebSocketPublish} methods, including calls made inside lambdas or method references
 * declared in the handler. The corresponding topics are reported as "Publishes to" of the incoming topic.
 * Calls made indirectly (e.g. through a service) are not followed.
 * <p>
 * Note: messages sent directly with {@code SimpMessagingTemplate.convertAndSend} are not detected;
 * wrap such calls in a method annotated with {@link WebSocketPublish} so they get documented.
 * <p>
 * The REST (HTTP) API is documented by {@link RestDocGenerator}.
 */
@Slf4j
@Component
public class WebSocketDocGenerator implements ApplicationRunner {

    @Autowired
    private ApplicationContext context;

    @Value("${spring.application.name}")
    private String applicationName;

    /** In the overview, lists of producers or triggers longer than this are replaced by a count */
    private static final int MAX_OVERVIEW_LIST_SIZE = 3;

    /** Overview data of an incoming topic */
    private record IncomingEntry(String topic, String description, String requestPayload, Set<String> outputTopics) {
    }

    /** Incoming topics per controller simple class name, computed by generateIncomingTopicsAsciidoc() (sorted for a stable output) */
    protected Map<String, List<IncomingEntry>> incomingEntriesByController = new TreeMap<>();
    /** Producers per outgoing topic, computed by generateOutgoingTopicsAsciidoc() (sorted for a stable output) */
    protected Map<String, List<TopicProducer>> producersByTopic = new TreeMap<>();
    /** Incoming topics whose handler directly calls a @WebSocketPublish method, per published topic (sorted for a stable output) */
    protected Map<String, Set<String>> topicToTriggeringIncomingMap = new TreeMap<>();

    @Override
    public void run(ApplicationArguments args) throws Exception {
       StringBuilder doc = new StringBuilder("= WebSocket Protocols of "+applicationName+" backend\n\n");
       String incomingAdoc = generateIncomingTopicsAsciidoc();
       String outgoingAdoc = generateOutgoingTopicsAsciidoc();
       // doc.append(generateAllTopicsMermaid());
       doc.append(generateAllTopicsOverview());
       doc.append(incomingAdoc);
       doc.append(outgoingAdoc);

        writeDoc(Paths.get("docgen/websocket-endpoints-topics.adoc"), doc.toString());
    }

   

    /**
     * Generate the overview: one table of incoming topics per controller, and one table of outgoing topics.
     * Requires incomingEntriesByController, producersByTopic and topicToTriggeringIncomingMap, that are computed by
     * generateIncomingTopicsAsciidoc() and generateOutgoingTopicsAsciidoc()
     * @return String
     */
    private String generateAllTopicsOverview() {
        StringBuilder doc = new StringBuilder("== Topics overview\n\n");

        doc.append("=== Incoming messages — commands and requests\n\n");
        for (Entry<String, List<IncomingEntry>> controllerEntry : incomingEntriesByController.entrySet()) {
            doc.append("==== ").append(controllerEntry.getKey()).append("\n\n");
            doc.append("[cols=\"2,3,1,2\", options=\"header\"]\n");
            doc.append("|===\n");
            doc.append("|Incoming topic |Description |Request payload |Output topics\n\n");
            for (IncomingEntry entry : controllerEntry.getValue()) {
                doc.append("|<<incoming_").append(toAsciiDocAnchor(entry.topic())).append(",").append(toSplittaString(entry.topic())).append(">>\n");
                doc.append("|").append(toTableCell(entry.description())).append("\n");
                doc.append("|").append(entry.requestPayload().isEmpty() ? "" : "`" + entry.requestPayload() + "`").append("\n");
                doc.append("|").append(entry.outputTopics().stream()
                        .map(this::toOutgoingTopicLink)
                        .collect(Collectors.joining(" +\n"))).append("\n\n");
            }
            doc.append("|===\n\n");
        }

        doc.append("=== Outgoing messages — subscriptions and notifications\n\n");
        doc.append("[cols=\"2,3,1,3\", options=\"header\"]\n");
        doc.append("|===\n");
        doc.append("|Outgoing topic |Description |Payload |Trigger / producer\n\n");
        for (Entry<String, List<TopicProducer>> topicEntry : producersByTopic.entrySet()) {
            String topic = topicEntry.getKey();
            List<TopicProducer> producers = topicEntry.getValue();

            // description of the topic (@WebSocketPublish), or else of the @SendTo replies
            String description = producers.stream()
                    .filter(TopicProducer::isPublisher)
                    .map(TopicProducer::description)
                    .filter(desc -> !desc.isEmpty())
                    .distinct()
                    .collect(Collectors.joining(" "));
            if (description.isEmpty()) {
                description = producers.stream()
                        .map(TopicProducer::description)
                        .filter(desc -> !desc.isEmpty())
                        .distinct()
                        .collect(Collectors.joining(" "));
            }
            String payloads = producers.stream()
                    .map(TopicProducer::payload)
                    .filter(payload -> payload != Void.class && payload != void.class)
                    .map(payload -> "`" + payload.getSimpleName() + "`")
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(", "));

            // long lists are replaced by a count, with a link to the topic details
            String detailsLink = "<<outgoing_" + toAsciiDocAnchor(topic) + ",see details>>";
            List<String> triggers = new ArrayList<>();
            if (producers.size() > MAX_OVERVIEW_LIST_SIZE) {
                triggers.add(producers.size() + " producers (" + detailsLink + ")");
            } else {
                for (TopicProducer producer : producers) {
                    triggers.add(producer.isPublisher()
                            ? "`" + producer.shortDeclaration() + "`"
                            : "`@SendTo` reply of " + producer.shortDeclaration());
                }
            }
            Set<String> triggeringIncomingTopics = topicToTriggeringIncomingMap.get(topic);
            if (triggeringIncomingTopics != null) {
                if (triggeringIncomingTopics.size() > MAX_OVERVIEW_LIST_SIZE) {
                    triggers.add("triggered by " + triggeringIncomingTopics.size() + " incoming topics (" + detailsLink + ")");
                } else {
                    triggers.add("triggered by: " + triggeringIncomingTopics.stream()
                            .map(t -> "<<incoming_" + toAsciiDocAnchor(t) + "," + toSplittaString(t) + ">>")
                            .collect(Collectors.joining(", ")));
                }
            }

            doc.append("|<<outgoing_").append(toAsciiDocAnchor(topic)).append(",").append(toSplittaString(topic)).append(">>\n");
            doc.append("|").append(toTableCell(description)).append("\n");
            doc.append("|").append(payloads).append("\n");
            doc.append("|").append(String.join(" +\n", triggers)).append("\n\n");
        }
        doc.append("|===\n\n");

        return doc.toString();
    }

    /**
     * Generate documentation section about Topics your the backend listens to (via @MessageMapping)
     * @return String
     */
    private String generateIncomingTopicsAsciidoc() {
        List<Class<?>> controllerClasses = sortedBeanClasses(context, Controller.class);
        log.info("Found controller classes: " + controllerClasses);
        StringBuilder doc = new StringBuilder("== Incoming Topics\n\n");

        for (Class<?> clazz : controllerClasses) {
            MessageMapping classMapping =clazz.getAnnotation(MessageMapping.class);

            String classMappingPath = classMapping != null ? String.join(", ", classMapping.value()) : "";

            for (Method method : sortedMethods(clazz.getMethods())) {
                if (method.isAnnotationPresent(MessageMapping.class) ) {
                    MessageMapping mapping = method.getAnnotation(MessageMapping.class);
                    String mappingPath = classMappingPath + String.join(", ", mapping.value());
                    doc.append("[[incoming_"+toAsciiDocAnchor(mappingPath)+"]]\n");
                    doc.append("===  Incoming Topic: `").append(mappingPath).append("`\n\n");
                    doc.append("Declared in `").append(clazz.getName()).append(".").append(method.getName()).append("()`\n\n");
                    WebSocketDoc wsDoc = method.getAnnotation(WebSocketDoc.class);
                    if (wsDoc != null && !wsDoc.description().isEmpty()) {
                        doc.append("_").append(wsDoc.description()).append("_\n\n");
                    }
                    if(method.isAnnotationPresent(SendTo.class)) {
                        doc.append("*Send To:* ")
                           .append(Arrays.stream(method.getAnnotation(SendTo.class).value())
                                .map(this::toOutgoingTopicLink)
                                .collect(Collectors.joining(", ")))
                           .append("\n\n");
                    }
                    Set<String> publishedTopics = findPublishedTopics(method);
                    if (!publishedTopics.isEmpty()) {
                        for (String publishedTopic : publishedTopics) {
                            this.topicToTriggeringIncomingMap.computeIfAbsent(publishedTopic, k -> new TreeSet<>()).add(mappingPath);
                        }
                        doc.append("*Publishes to:* ")
                           .append(publishedTopics.stream()
                                .map(t -> "<<outgoing_" + toAsciiDocAnchor(t) + "," + toSplittaString(t) + ">>")
                                .collect(Collectors.joining(", ")))
                           .append("\n\n");
                    }
                    Set<String> indirectOutputs = new TreeSet<>(wsDoc != null ? Arrays.asList(wsDoc.indirectOutputs()) : List.of());
                    if (!indirectOutputs.isEmpty()) {
                        for (String indirectOutput : indirectOutputs) {
                            this.topicToTriggeringIncomingMap.computeIfAbsent(indirectOutput, k -> new TreeSet<>()).add(mappingPath);
                        }
                        doc.append("*Indirect outputs:* \n\n")
                           .append(indirectOutputs.stream()
                                .map(t -> "- " + toCheckedOutgoingTopicLink(t, clazz.getName() + "." + method.getName() + "()"))
                                .collect(Collectors.joining("\n")))
                           .append("\n\n");
                    }
                    // overview data
                    Set<String> outputTopics = new TreeSet<>();
                    if (method.isAnnotationPresent(SendTo.class)) {
                        outputTopics.addAll(Arrays.asList(method.getAnnotation(SendTo.class).value()));
                    }
                    outputTopics.addAll(publishedTopics);
                    outputTopics.addAll(indirectOutputs);
                    String requestPayload = Arrays.stream(method.getParameters())
                            .filter(param -> param.isAnnotationPresent(Payload.class))
                            .map(param -> param.getType().getSimpleName())
                            .findFirst().orElse("");
                    incomingEntriesByController.computeIfAbsent(clazz.getSimpleName(), k -> new ArrayList<>())
                            .add(new IncomingEntry(mappingPath, wsDoc != null ? wsDoc.description() : "", requestPayload, outputTopics));

                    doc.append("==== Parameters\n\n");
                    for (Parameter param : method.getParameters()) {
                        String type = param.getType().getSimpleName();
                        String name = param.getName();
                        if (param.isAnnotationPresent(DestinationVariable.class)) {
                            name = param.getAnnotation(DestinationVariable.class).value();
                        }
                        doc.append("- `").append(name).append("` (").append(type).append(")\n");
                        if (param.isAnnotationPresent(Payload.class) && !isSimpleType(param.getType())) {
                            doc.append("\n==== Request Payload\n\n");
                            doc.append("[source,json]\n----\n");
                            doc.append(buildJsonSchema(param.getType()));
                            doc.append("----\n\n");

                        } else {
                            doc.append("\n");
                        }
                    }
                    if(method.isAnnotationPresent(SendTo.class)) {
                        doc.append("\n==== Response Payload\n\n");
                        appendPayload(doc, method.getReturnType());
                    } else if (publishedTopics.isEmpty() && indirectOutputs.isEmpty()) {
                        doc.append("\nNOTE: No direct output topic detected (no `@SendTo`, no direct `@WebSocketPublish` call). "
                                + "Indirect outputs (via services or machine feedback, e.g. command or machine status updates) are possible.\n\n");
                    }
                }
            }
        }
        return doc.toString();
    }

    /** A method producing messages on an outgoing topic */
    private record TopicProducer(String declaration, String shortDeclaration, String role, boolean isPublisher, String description, Class<?> payload) {
    }

    /**
     * Generate documentation section about Topics the backend sends messages to ( @WebSocketPublish or @SendTo).
     * There is one section per topic, listing all its producers, the incoming topics that trigger it and its payload.
     * Important: in order to correctly detect the outgoing topic do not directly use template.convertAndSend and replace it with a method annotated with @WebSocketPublish
     * Requires topicToTriggeringIncomingMap, computed by generateIncomingTopicsAsciidoc()
     * @return String
     */
    private String generateOutgoingTopicsAsciidoc() {
        // collect the producers of each topic (in the producersByTopic field, also used by the overview)

        // methods with @WebSocketPublish
        for (Class<?> clazz : sortedBeanClasses(context, Component.class)) {
            for (Method method : sortedMethods(clazz.getDeclaredMethods())) {
                WebSocketPublish wsPub = method.getAnnotation(WebSocketPublish.class);
                if (wsPub != null) {
                    String topic = wsPub.value();
                    producersByTopic.computeIfAbsent(topic, k -> new ArrayList<>())
                        .add(new TopicProducer(clazz.getName() + "." + method.getName() + "()",
                            clazz.getSimpleName() + ".&#8203;" + method.getName() + "()",
                            "`@WebSocketPublish` method", true, wsPub.description(), wsPub.payload()));
                }
            }
        }

        // @MessageMapping methods with @SendTo
        for (Class<?> clazz : sortedBeanClasses(context, Controller.class)) {
            MessageMapping classMapping = clazz.getAnnotation(MessageMapping.class);
            String classMappingPath = classMapping != null ? String.join(", ", classMapping.value()) : "";

            for (Method method : sortedMethods(clazz.getMethods())) {
                if (method.isAnnotationPresent(MessageMapping.class) && method.isAnnotationPresent(SendTo.class)) {
                    String mappingPath = classMappingPath + String.join(", ", method.getAnnotation(MessageMapping.class).value());
                    WebSocketDoc wsDoc = method.getAnnotation(WebSocketDoc.class);
                    for (String topic : method.getAnnotation(SendTo.class).value()) {
                            producersByTopic.computeIfAbsent(topic, k -> new ArrayList<>())
                            .add(new TopicProducer(clazz.getName() + "." + method.getName() + "()",
                                clazz.getSimpleName() + ".&#8203;" + method.getName() + "()",
                                "`@SendTo` reply to <<incoming_" + toAsciiDocAnchor(mappingPath) + "," + toSplittaString(mappingPath) + ">>",
                                false, wsDoc != null ? wsDoc.responseDescription() : "", method.getReturnType()));
                    }
                }
            }
        }

        // render one section per topic
        StringBuilder doc = new StringBuilder("== Outgoing Topics\n\n");
        for (Entry<String, List<TopicProducer>> entry : producersByTopic.entrySet()) {
            String topic = entry.getKey();
            List<TopicProducer> producers = entry.getValue();

            doc.append("[[outgoing_").append(toAsciiDocAnchor(topic)).append("]]\n");
            doc.append("=== Outgoing Topic: `").append(topic).append("`\n\n");
            producers.stream()
                .filter(TopicProducer::isPublisher) // @SendTo descriptions are rendered with their producer
                .map(TopicProducer::description)
                .filter(desc -> !desc.isEmpty())
                .distinct()
                .forEach(desc -> doc.append("_").append(desc).append("_\n\n"));

            doc.append("==== Producers\n\n");
            for (TopicProducer producer : producers) {
                doc.append("- `").append(producer.declaration()).append("`: ").append(producer.role());
                doc.append("\n");
            }
            doc.append("\n");

            Set<String> triggeringIncomingTopics = topicToTriggeringIncomingMap.get(topic);
            if (triggeringIncomingTopics != null) {
                doc.append("*Triggered by incoming topics:* \n\n")
                   .append(triggeringIncomingTopics.stream()
                        .map(t -> "- <<incoming_" + toAsciiDocAnchor(t) + "," + toSplittaString(t) + ">>")
                        .collect(Collectors.joining("\n")))
                   .append("\n\n");
            }

            doc.append("==== Payload\n\n");
            Map<String, Class<?>> payloads = new TreeMap<>(); // distinct payload types, sorted by name
            for (TopicProducer producer : producers) {
                if (producer.payload() != Void.class && producer.payload() != void.class) {
                    payloads.put(producer.payload().getName(), producer.payload());
                }
            }
            if (payloads.isEmpty()) {
                doc.append("`undocumented`\n\n");
            } else {
                if (payloads.size() > 1) {
                    doc.append("WARNING: the producers of this topic declare different payload types.\n\n");
                }
                for (Class<?> payload : payloads.values()) {
                    appendPayload(doc, payload);
                }
            }
        }

        return doc.toString();
    }

    /**
     * Same as {@link #toOutgoingTopicLink(String)}, but also log a warning if the topic is not declared by any
     * {@link WebSocketPublish} or {@code @SendTo} (used once per declaration, to avoid duplicated warnings).
     */
    private String toCheckedOutgoingTopicLink(String topic, String declaredIn) {
        if (!getDeclaredOutgoingTopics().contains(topic)) {
            log.warn("Unknown outgoing topic '" + topic + "' in @WebSocketDoc of " + declaredIn
                    + ": it is not declared by any @WebSocketPublish or @SendTo");
        }
        return toOutgoingTopicLink(topic);
    }

    /**
     * Return an AsciiDoc link to the section of the given outgoing topic, or the plain topic if it is not declared
     * by any {@link WebSocketPublish} or {@code @SendTo} (there is no section to link to).
     */
    private String toOutgoingTopicLink(String topic) {
        if (!getDeclaredOutgoingTopics().contains(topic)) {
            return "`" + topic + "` (undeclared topic)";
        }
        return "<<outgoing_" + toAsciiDocAnchor(topic) + "," + toSplittaString(topic) + ">>";
    }

    /** Topics declared by @WebSocketPublish methods or @SendTo handlers (computed once) */
    private Set<String> declaredOutgoingTopics;

    private Set<String> getDeclaredOutgoingTopics() {
        if (declaredOutgoingTopics == null) {
            declaredOutgoingTopics = new TreeSet<>();
            for (Class<?> clazz : sortedBeanClasses(context, Component.class)) {
                for (Method method : clazz.getMethods()) {
                    WebSocketPublish wsPub = method.getAnnotation(WebSocketPublish.class);
                    if (wsPub != null) {
                        declaredOutgoingTopics.add(wsPub.value());
                    }
                    SendTo sendTo = method.getAnnotation(SendTo.class);
                    if (sendTo != null && method.isAnnotationPresent(MessageMapping.class)) {
                        declaredOutgoingTopics.addAll(Arrays.asList(sendTo.value()));
                    }
                }
            }
        }
        return declaredOutgoingTopics;
    }

    /**
     * Return the topics of the {@link WebSocketPublish} methods directly called by the given handler method,
     * including calls made in the lambdas declared in this handler and method references to publish methods.
     * Detection is done by scanning the bytecode of the handler's declaring class.
     */
    private Set<String> findPublishedTopics(Method handler) {
        Set<String> topics = new TreeSet<>();
        Class<?> owner = handler.getDeclaringClass();
        scanMethodForPublishedTopics(owner, handler.getName(), Type.getMethodDescriptor(handler), new HashSet<>(), topics);
        return topics;
    }

    /**
     * Scan the bytecode of the method {@code methodName}/{@code methodDesc} of {@code owner}, adding to {@code topics}
     * the topics of the called {@link WebSocketPublish} methods. Synthetic lambda bodies of the same class are scanned recursively.
     */
    private void scanMethodForPublishedTopics(Class<?> owner, String methodName, String methodDesc,
            Set<String> visitedMethods, Set<String> topics) {
        if (!visitedMethods.add(methodName + methodDesc)) {
            return; // already scanned
        }
        String ownerInternalName = Type.getInternalName(owner);
        Set<String[]> lambdaBodies = new LinkedHashSet<>(); // {name, desc} of the lambdas declared in this method
        String resource = ownerInternalName + ".class";
        ClassLoader classLoader = owner.getClassLoader() != null ? owner.getClassLoader() : ClassLoader.getSystemClassLoader();
        try (InputStream in = classLoader.getResourceAsStream(resource)) {
            if (in == null) {
                log.warn("Cannot read bytecode of " + owner.getName() + ", published topics of its handlers are not detected");
                return;
            }
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    if (!name.equals(methodName) || !desc.equals(methodDesc)) {
                        return null;
                    }
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String calleeOwner, String calleeName, String calleeDesc, boolean isInterface) {
                            findWebSocketPublish(classLoader, calleeOwner, calleeName, calleeDesc)
                                .ifPresent(wsPub -> topics.add(wsPub.value()));
                        }

                        @Override
                        public void visitInvokeDynamicInsn(String name, String desc, Handle bootstrapMethod, Object... bootstrapArgs) {
                            // lambdas and method references: the implementation method is given as a Handle argument
                            for (Object arg : bootstrapArgs) {
                                if (arg instanceof Handle implementation) {
                                    Optional<WebSocketPublish> wsPub = findWebSocketPublish(classLoader,
                                            implementation.getOwner(), implementation.getName(), implementation.getDesc());
                                    if (wsPub.isPresent()) {
                                        topics.add(wsPub.get().value()); // method reference such as publisher::sendFrontendLogs
                                    } else if (implementation.getOwner().equals(ownerInternalName)) {
                                        lambdaBodies.add(new String[] { implementation.getName(), implementation.getDesc() });
                                    }
                                }
                            }
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (IOException e) {
            log.warn("Error reading bytecode of " + owner.getName(), e);
            return;
        }
        for (String[] lambdaBody : lambdaBodies) {
            scanMethodForPublishedTopics(owner, lambdaBody[0], lambdaBody[1], visitedMethods, topics);
        }
    }

    /** Return the {@link WebSocketPublish} annotation of the given method (bytecode coordinates), if any. */
    private Optional<WebSocketPublish> findWebSocketPublish(ClassLoader classLoader, String internalOwner, String name, String desc) {
        if (internalOwner.startsWith("java/") || internalOwner.startsWith("[")) {
            return Optional.empty(); // JDK classes and arrays never declare @WebSocketPublish
        }
        try {
            Class<?> calleeClass = Class.forName(Type.getObjectType(internalOwner).getClassName(), false, classLoader);
            return Arrays.stream(calleeClass.getMethods())
                    .filter(m -> m.getName().equals(name) && Type.getMethodDescriptor(m).equals(desc))
                    .map(m -> m.getAnnotation(WebSocketPublish.class))
                    .filter(Objects::nonNull)
                    .findFirst();
        } catch (ClassNotFoundException | LinkageError e) {
            return Optional.empty();
        }
    }

}
