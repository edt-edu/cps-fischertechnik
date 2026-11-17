package io.github.mbdo.factoryscada.docgen;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

import org.springframework.aop.support.AopUtils;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;

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
 */
@Slf4j
@Component
public class WebSocketDocGenerator implements ApplicationRunner {

    @Autowired
    private ApplicationContext context;

    @Value("${spring.application.name}")
    private String applicationName;


    /**
     * Deterministic method order: getMethods()/getDeclaredMethods() return methods in no particular order
     * (it may change between compilations), so sort by name then by parameter types (for overloads).
     */
    private static final Comparator<Method> METHOD_ORDER = Comparator.comparing(Method::getName)
            .thenComparing(m -> Arrays.stream(m.getParameterTypes()).map(Class::getName).collect(Collectors.joining(",")));

    /** List of incoming topics for a given class and method (sorted maps/sets for a stable output) */
    protected Map<String, Map<String, Set<String>>> methodToIncomingTopicMap = new TreeMap<>();
    /** List of outgoing topics for a given class and method (sorted maps/sets for a stable output) */
    protected Map<String, Map<String, Set<String>>> methodToOutgoingTopicMap = new TreeMap<>();
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

        // Write to file
        Path outputPath = Paths.get("docgen/websocket-endpoints-topics.adoc");
        Files.createDirectories(outputPath.getParent()); // Ensure parent directory exists
        Files.write(outputPath, doc.toString().getBytes(StandardCharsets.UTF_8));     
        log.info("Generated protocol documentation: " + outputPath);   
    }

   

    /**
     * generate the overview as a table
     * requires methodToIncomingTopicMap and methodToOutgoingTopicMap  that are computed by generateOutgoingTopicsAsciidoc() 
     * @return
     */
    private String generateAllTopicsOverview() {
        StringBuilder doc = new StringBuilder("== Topics overview\n\n");
        doc.append("[cols=\"1,1,1\"]\n");
        doc.append("|===\n");
        doc.append("| Implementing class | Incoming topic | Outgoing topic \n\n");

        for (String className : methodToIncomingTopicMap.keySet()) {
            for( Entry<String, Set<String>> methodMap : methodToIncomingTopicMap.get(className).entrySet()) {
                String methodName = methodMap.getKey();
                for (String topic : methodMap.getValue()) {
                    doc.append("|"+className+".&#8203;"+methodName+"()\n");
                    doc.append("|<<incoming_"+toAsciiDocAnchor(topic)+","+toSplittaString(topic)+">>\n");
                    if(methodToOutgoingTopicMap.containsKey(className) ){
                        Set<String> topics = methodToOutgoingTopicMap.get(className).get(methodName);
                        if(topics !=null) {
                            doc.append("|"+topics.stream()
                                .map(stopic -> "<<outgoing_"+this.toAsciiDocAnchor(stopic)+","+toSplittaString(stopic)+">>")
                                .collect(Collectors.joining(", "))+"\n");
                        } else {
                            doc.append("|  \n");
                        }
                        methodToOutgoingTopicMap.get(className).remove(methodName); //  remove processed outgoing
                    } else {
                        doc.append("|  \n");
                    }
                    doc.append("\n");
                }
            }
        }
        // process remaining outgoing topics
        for (String className : methodToOutgoingTopicMap.keySet()) {
            for( Entry<String, Set<String>> methodMap : methodToOutgoingTopicMap.get(className).entrySet()) {
                    doc.append("|"+className+".&#8203;"+methodMap.getKey()+"()\n");
                    doc.append("| \n");
                    doc.append("|"+methodMap.getValue().stream()
                        .map(stopic -> "<<outgoing_"+this.toAsciiDocAnchor(stopic)+","+toSplittaString(stopic)+">>")
                        .collect(Collectors.joining(", "))+"\n");
                    doc.append("\n");
            }
        }


        doc.append("|===\n\n");
        return doc.toString();
    }

    /**
     * Generate documentation section about Topics your the backend listens to (via @MessageMapping)
     * @return String
     */
    private String generateIncomingTopicsAsciidoc() {
        List<Class<?>> controllerClasses = sortedBeanClasses(Controller.class);
        log.info("Found controller classes: " + controllerClasses);
        StringBuilder doc = new StringBuilder("== Incoming Topics\n\n");

        for (Class<?> clazz : controllerClasses) {
            MessageMapping classMapping =clazz.getAnnotation(MessageMapping.class);

            String classMappingPath = classMapping != null ? String.join(", ", classMapping.value()) : "";

            for (Method method : sortedMethods(clazz.getMethods())) {
                if (method.isAnnotationPresent(MessageMapping.class) ) {
                    MessageMapping mapping = method.getAnnotation(MessageMapping.class);
                    String mappingPath = classMappingPath + String.join(", ", mapping.value());
                    String sendToPath = "";
                    if(method.isAnnotationPresent(SendTo.class)) {
                        SendTo sendTo = method.getAnnotation(SendTo.class);
                        sendToPath = String.join(", ", sendTo.value());
                    }
                   

                    doc.append("[[incoming_"+toAsciiDocAnchor(mappingPath)+"]]\n");
                    doc.append("===  Incoming Topic: `").append(mappingPath).append("`\n\n");
                    doc.append("Declared in `").append(clazz.getName()).append(".").append(method.getName()).append("()`\n\n");
                    this.addTopicInMap(this.methodToIncomingTopicMap, clazz.getSimpleName(), method.getName(), mappingPath);
                    WebSocketDoc wsDoc = method.getAnnotation(WebSocketDoc.class);
                    if (wsDoc != null && !wsDoc.description().isEmpty()) {
                        doc.append("_").append(wsDoc.description()).append("_\n\n");
                    }
                    if(method.isAnnotationPresent(SendTo.class)) {
                        doc.append("*Send To:* `").append(sendToPath).append("`\n\n");
                    }
                    Set<String> publishedTopics = findPublishedTopics(method);
                    if (!publishedTopics.isEmpty()) {
                        for (String publishedTopic : publishedTopics) {
                            this.addTopicInMap(this.methodToOutgoingTopicMap, clazz.getSimpleName(), method.getName(), publishedTopic);
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
                            this.addTopicInMap(this.methodToOutgoingTopicMap, clazz.getSimpleName(), method.getName(), indirectOutput);
                            this.topicToTriggeringIncomingMap.computeIfAbsent(indirectOutput, k -> new TreeSet<>()).add(mappingPath);
                        }
                        doc.append("*Indirect outputs:* ")
                           .append(indirectOutputs.stream()
                                .map(t -> toOutgoingTopicLink(t, clazz.getName() + "." + method.getName() + "()"))
                                .collect(Collectors.joining(", ")))
                           .append("\n\n");
                    }
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
    private record TopicProducer(String declaration, String role, boolean isPublisher, String description, Class<?> payload) {
    }

    /**
     * Generate documentation section about Topics the backend sends messages to ( @WebSocketPublish or @SendTo).
     * There is one section per topic, listing all its producers, the incoming topics that trigger it and its payload.
     * Important: in order to correctly detect the outgoing topic do not directly use template.convertAndSend and replace it with a method annotated with @WebSocketPublish
     * Requires topicToTriggeringIncomingMap, computed by generateIncomingTopicsAsciidoc()
     * @return String
     */
    private String generateOutgoingTopicsAsciidoc() {
        // collect the producers of each topic (sorted map for a stable output)
        Map<String, List<TopicProducer>> producersByTopic = new TreeMap<>();

        // methods with @WebSocketPublish
        for (Class<?> clazz : sortedBeanClasses(Component.class)) {
            for (Method method : sortedMethods(clazz.getDeclaredMethods())) {
                WebSocketPublish wsPub = method.getAnnotation(WebSocketPublish.class);
                if (wsPub != null) {
                    String topic = wsPub.value();
                    this.addTopicInMap(this.methodToOutgoingTopicMap, clazz.getSimpleName(), method.getName(), topic);
                    producersByTopic.computeIfAbsent(topic, k -> new ArrayList<>())
                        .add(new TopicProducer(clazz.getName() + "." + method.getName() + "()",
                            "`@WebSocketPublish` method", true, wsPub.description(), wsPub.payload()));
                }
            }
        }

        // @MessageMapping methods with @SendTo
        for (Class<?> clazz : sortedBeanClasses(Controller.class)) {
            MessageMapping classMapping = clazz.getAnnotation(MessageMapping.class);
            String classMappingPath = classMapping != null ? String.join(", ", classMapping.value()) : "";

            for (Method method : sortedMethods(clazz.getMethods())) {
                if (method.isAnnotationPresent(MessageMapping.class) && method.isAnnotationPresent(SendTo.class)) {
                    String mappingPath = classMappingPath + String.join(", ", method.getAnnotation(MessageMapping.class).value());
                    WebSocketDoc wsDoc = method.getAnnotation(WebSocketDoc.class);
                    for (String topic : method.getAnnotation(SendTo.class).value()) {
                        this.addTopicInMap(this.methodToOutgoingTopicMap, clazz.getSimpleName(), method.getName(), topic);
                        producersByTopic.computeIfAbsent(topic, k -> new ArrayList<>())
                            .add(new TopicProducer(clazz.getName() + "." + method.getName() + "()",
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
                if (!producer.isPublisher() && !producer.description().isEmpty()) {
                    doc.append(": _").append(producer.description()).append("_");
                }
                doc.append("\n");
            }
            doc.append("\n");

            Set<String> triggeringIncomingTopics = topicToTriggeringIncomingMap.get(topic);
            if (triggeringIncomingTopics != null) {
                doc.append("*Triggered by incoming topics:* ")
                   .append(triggeringIncomingTopics.stream()
                        .map(t -> "<<incoming_" + toAsciiDocAnchor(t) + "," + toSplittaString(t) + ">>")
                        .collect(Collectors.joining(", ")))
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



    private String buildJsonSchema(Class<?> clazz) {
        // Sort properties alphabetically: otherwise their order depends on reflection order, which is not stable
        ObjectMapper mapper = JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .build();
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        try {
            JsonSchema schema = schemaGen.generateSchema(clazz);
            return mapper.writerWithDefaultPrettyPrinter()
                                      .writeValueAsString(schema)+"\n";
        
        } catch (JsonProcessingException e) {
            log.error("Error generating json schema for clazz "+clazz.getName(), e);
        }
        return "";
    }

    public String toAsciiDocAnchor(String input) {
        if (input == null) return "";

        // Convert to lowercase
        String anchor = input.toLowerCase();

        // Replace all sequences of non-alphanumeric characters with a dash
        anchor = anchor.replaceAll("[^a-z0-9]+", "-");

        // Remove leading/trailing dashes
        anchor = anchor.replaceAll("(^-+)|(-+$)", "");

        return anchor;
    }

    /**
     * Make a topic path splittable on several lines when displayed (keeping its case),
     * by appending an invisible (zero width) space after each "/".
     */
    public String toSplittaString(String input) {
        if (input == null) return "";
        return input.replace("/", "/&#8203;");
    }

    /**
     * Return an AsciiDoc link to the section of the given outgoing topic. If no {@link WebSocketPublish} or
     * {@code @SendTo} declares this topic (so there is no section to link to), log a warning and return the plain topic.
     */
    private String toOutgoingTopicLink(String topic, String declaredIn) {
        if (!getDeclaredOutgoingTopics().contains(topic)) {
            log.warn("Unknown outgoing topic '" + topic + "' in @WebSocketDoc of " + declaredIn
                    + ": it is not declared by any @WebSocketPublish or @SendTo");
            return "`" + topic + "` (undeclared topic)";
        }
        return "<<outgoing_" + toAsciiDocAnchor(topic) + "," + toSplittaString(topic) + ">>";
    }

    /** Topics declared by @WebSocketPublish methods or @SendTo handlers (computed once) */
    private Set<String> declaredOutgoingTopics;

    private Set<String> getDeclaredOutgoingTopics() {
        if (declaredOutgoingTopics == null) {
            declaredOutgoingTopics = new TreeSet<>();
            for (Class<?> clazz : sortedBeanClasses(Component.class)) {
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
     * Append the description of a payload type: its name for simple types,
     * otherwise its JSON schema in an AsciiDoc source block.
     */
    private void appendPayload(StringBuilder doc, Class<?> payloadClass) {
        doc.append("`").append(payloadClass.getSimpleName()).append("`\n\n");
        if (!isSimpleType(payloadClass)) {
            doc.append("[source,json]\n----\n");
            doc.append(buildJsonSchema(payloadClass));
            doc.append("----\n\n");
        }
    }

    private boolean isSimpleType(Class<?> clazz) {
    return clazz.isPrimitive() ||
           clazz == String.class ||
           Number.class.isAssignableFrom(clazz) ||
           clazz == Boolean.class ||
           clazz == Character.class ||
           clazz.isEnum();
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

    private void addTopicInMap(Map<String, Map<String, Set<String>>> map, String className, String methodName, String topic) {
        map.computeIfAbsent(className, k -> new TreeMap<>())
           .computeIfAbsent(methodName, k -> new TreeSet<>())
           .add(topic);
    }

    /**
     * Return the target classes of the beans annotated with the given annotation, sorted by class name.
     * (getBeansWithAnnotation follows bean registration order, which depends on classpath scanning order)
     */
    private List<Class<?>> sortedBeanClasses(Class<? extends Annotation> annotation) {
        return context.getBeansWithAnnotation(annotation).values().stream()
                .<Class<?>>map(AopUtils::getTargetClass)
                .sorted(Comparator.comparing(Class::getName))
                .collect(Collectors.toList());
    }

    private static List<Method> sortedMethods(Method[] methods) {
        return Arrays.stream(methods).sorted(METHOD_ORDER).collect(Collectors.toList());
    }

}
