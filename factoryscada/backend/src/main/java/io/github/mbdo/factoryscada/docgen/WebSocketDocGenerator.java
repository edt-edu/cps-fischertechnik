package io.github.mbdo.factoryscada.docgen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class WebSocketDocGenerator implements ApplicationRunner {

    @Autowired
    private ApplicationContext context;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Map<String, Object> beans = context.getBeansWithAnnotation(Controller.class);
        log.info("Found controller beans: " + beans.keySet());
        StringBuilder doc = new StringBuilder("= WebSocket Topics\n\n");

        for (Object bean : beans.values()) {
            Class<?> clazz = AopUtils.getTargetClass(bean);

            MessageMapping classMapping =clazz.getAnnotation(MessageMapping.class);

            String classMappingPath = classMapping != null ? String.join(", ", classMapping.value()) : "";

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(MessageMapping.class) ) {
                    MessageMapping mapping = method.getAnnotation(MessageMapping.class);
                    String mappingPath = classMappingPath + String.join(", ", mapping.value());
                    String sendToPath = "";
                    if(method.isAnnotationPresent(SendTo.class)) {
                        SendTo sendTo = method.getAnnotation(SendTo.class);
                        sendToPath = String.join(", ", sendTo.value());
                    }
                   

                    doc.append("=== ").append(mappingPath).append("\n\n");
                    doc.append("declared in `").append(clazz.getName()).append("`\n\n");
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
                        doc.append("\n==== Response Payload\n\n");
                        doc.append("[source,json]\n----\n");
                        doc.append(buildJsonSchema(method.getReturnType()));
                        doc.append("----\n\n");
                    } else {
                        doc.append("\nNOTE: This endpoint does not send a response back via a topic.\n\n");
                    }
                }
            }
        }

        // Write to file
        Files.write(Paths.get("docgen/websocket-topics.adoc"), doc.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String buildJsonSchema(Class<?> clazz) {
        StringBuilder json = new StringBuilder("{\n");
        for (Field field : clazz.getDeclaredFields()) {
            json.append("  \"").append(field.getName()).append("\": \"")
                .append(field.getType().getSimpleName()).append("\",\n");
        }
        if (json.lastIndexOf(",") > 0) {
            json.deleteCharAt(json.lastIndexOf(","));
        }
        json.append("}\n");
        return json.toString();
    }
}
