package io.github.mbdo.factoryscada.docgen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Set;

import org.springframework.aop.support.AopUtils;
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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;

import io.github.mbdo.factoryscada.frontend.WebSocketPublish;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WebSocketDocGenerator implements ApplicationRunner {

    @Autowired
    private ApplicationContext context;

    @Value("${spring.application.name}")
    private String applicationName;


    /** List of incoming topics for a given class and method */
    protected HashMap<String, HashMap<String,Set<String>>> methodToIncomingTopicMap = new HashMap<String, HashMap<String,Set<String>>>();
    /** List of outgoing topics for a given class and method */
    protected HashMap<String, HashMap<String,Set<String>>> methodToOutgoingTopicMap = new HashMap<String, HashMap<String,Set<String>>>();

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
                    doc.append("|"+className+"."+methodName+"()\n");
                    doc.append("|<<incoming_"+toAsciiDocAnchor(topic)+","+topic+">>\n");
                    if(methodToOutgoingTopicMap.containsKey(className) ){
                        Set<String> topics = methodToOutgoingTopicMap.get(className).get(methodName);
                        if(topics !=null) {
                            doc.append("|"+topics.stream()
                                .map(stopic -> "<<outgoing_"+this.toAsciiDocAnchor(stopic)+","+stopic+">>")
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
                    doc.append("|"+className+"."+methodMap.getKey()+"()\n");
                    doc.append("| \n");
                    doc.append("|"+methodMap.getValue().stream().collect(Collectors.joining(", "))+"\n");
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
        Map<String, Object> beans = context.getBeansWithAnnotation(Controller.class);
        log.info("Found controller beans: " + beans.keySet());
        StringBuilder doc = new StringBuilder("== Incoming Topics\n\n");

        for (Object bean : beans.values()) {
            Class<?> clazz = AopUtils.getTargetClass(bean);

            MessageMapping classMapping =clazz.getAnnotation(MessageMapping.class);

            String classMappingPath = classMapping != null ? String.join(", ", classMapping.value()) : "";

            for (Method method : clazz.getMethods()) {
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
                    if(method.isAnnotationPresent(SendTo.class)) {
                        doc.append("*Send To:* `").append(sendToPath).append("`\n\n");
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

                        }
                    }
                    if(method.isAnnotationPresent(SendTo.class)) {
                        if(isSimpleType(method.getReturnType())) {
                            doc.append("\n==== Response Payload\n\n");
                            doc.append(method.getReturnType()+"\n\n");
                        } else {
                            doc.append("\n==== Response Payload\n\n");
                            doc.append("[source,json]\n----\n");
                            doc.append(buildJsonSchema(method.getReturnType()));
                            doc.append("----\n\n");
                        }
                    } else {
                        doc.append("\nNOTE: This endpoint does not send a response back via a topic.\n\n");
                    }
                }
            }
        }
        return doc.toString();
    }

    /**
     * Generate documentation section about Topics the backend sends messages to ( @WebSocketPublish or @SendTo)
     * Important: in order to correctly detect the outgoing topic do not directly use templace.convertAndSend and replace it with a method anotated  with @@WebSocketPublish
     * @return String
     */
    private String generateOutgoingTopicsAsciidoc() {
        StringBuilder doc = new StringBuilder("== Outgoing Topics\n\n");

        // report methods with @WebSocketPublish
        Map<String, Object> beans = context.getBeansWithAnnotation(Component.class);

        for (Object bean : beans.values()) {
            Class<?> clazz = AopUtils.getTargetClass(bean);

            for (Method method : clazz.getDeclaredMethods()) {
                WebSocketPublish wsPub = method.getAnnotation(WebSocketPublish.class);
                if (wsPub != null) {
                    String topic = wsPub.value();
                    String desc = wsPub.description();
                    Class<?> payloadClass = wsPub.payload();
                    
                    doc.append("\n=== Outgoing Topic: `").append(topic).append("`\n");
                    this.addTopicInMap(this.methodToOutgoingTopicMap,clazz.getSimpleName(), method.getName(), topic);
                    if (!desc.isEmpty()) {
                        doc.append("_").append(desc).append("_\n\n");
                    }
                    if (payloadClass != Void.class) {
                        doc.append("Payload: `").append(payloadClass.getSimpleName()).append("`\n");
                        // render DTO fields here
                         doc.append(buildJsonSchema(payloadClass));
                    } else {
                        doc.append("Payload: `undocumented`\n");
                    }
                }
            }
        }

        // report @SendTo in 
        beans = context.getBeansWithAnnotation(Controller.class);
        for (Object bean : beans.values()) {
            Class<?> clazz = AopUtils.getTargetClass(bean);

            MessageMapping classMapping =clazz.getAnnotation(MessageMapping.class);

            String classMappingPath = classMapping != null ? String.join(", ", classMapping.value()) : "";

            for (Method method : clazz.getMethods()) {
                if (method.isAnnotationPresent(MessageMapping.class) && method.isAnnotationPresent(SendTo.class)) {
                    MessageMapping mapping = method.getAnnotation(MessageMapping.class);
                    String mappingPath = classMappingPath + String.join(", ", mapping.value());
                    String sendToPath = "";
                    SendTo sendTo = method.getAnnotation(SendTo.class);
                    sendToPath = String.join(", ", sendTo.value());
                    
                    doc.append("[[outgoing_"+toAsciiDocAnchor(sendToPath)+"]]\n");
                    doc.append("=== Outgoing Topic: `").append(sendToPath).append("`\n\n");
                    doc.append("declared in `").append(clazz.getName()).append(".").append(method.getName()).append("()`\n\n");

                    this.addTopicInMap(this.methodToOutgoingTopicMap, clazz.getSimpleName(), method.getName(), sendToPath);
                    doc.append("*Request Mapping:* `@MessageMapping(\"").append(mappingPath).append("\")`\n\n");
                    doc.append("*Send To:* `").append(sendToPath).append("`\n\n");

                    doc.append("==== Parameters\n\n");
                    for (Parameter param : method.getParameters()) {
                        String type = param.getType().getSimpleName();
                        String name = param.getName();
                        if (param.isAnnotationPresent(DestinationVariable.class)) {
                            name = param.getAnnotation(DestinationVariable.class).value();
                        }
                        doc.append("- `").append(name).append("` (").append(type).append(")\n");
                    }
                    if(method.isAnnotationPresent(SendTo.class)) {
                        if(method.getReturnType().isPrimitive()) {
                            doc.append("\n==== Response Payload\n\n");
                            doc.append(method.getReturnType()+"\n\n");
                        } else {
                            doc.append("\n==== Response Payload\n\n");
                            doc.append("[source,json]\n----\n");
                            doc.append(buildJsonSchema(method.getReturnType()));
                            doc.append("----\n\n");
                        }
                    } else {
                        doc.append("\nNOTE: This endpoint does not send a response back via a topic.\n\n");
                    }
                }
            }
        }

        return doc.toString();
    }



    private String buildJsonSchema(Class<?> clazz) {
        ObjectMapper mapper = new ObjectMapper();
        // configure mapper, if necessary, then create schema generator
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

    private boolean isSimpleType(Class<?> clazz) {
    return clazz.isPrimitive() ||
           clazz == String.class ||
           Number.class.isAssignableFrom(clazz) ||
           clazz == Boolean.class ||
           clazz == Character.class ||
           clazz.isEnum();
    }

    private void addTopicInMap(HashMap<String, HashMap<String,Set<String>>> map, String className, String methodName, String topic) {
        if(!map.containsKey(className)) {
            map.put(className, new HashMap<String,Set<String>>());
        }
        HashMap<String, Set<String>> methodMap =  map.get(className);
        if(!methodMap.containsKey(methodName)) {
            methodMap.put(methodName, new HashSet<>());
        }
        methodMap.get(methodName).add(topic);
    }

}
