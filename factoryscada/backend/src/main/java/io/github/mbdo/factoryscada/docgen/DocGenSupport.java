package io.github.mbdo.factoryscada.docgen;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;

import lombok.extern.slf4j.Slf4j;

/**
 * Helpers shared by the endpoint documentation generators ({@link WebSocketDocGenerator} and
 * {@link RestDocGenerator}): deterministic ordering of beans and methods, JSON schemas of payloads,
 * and AsciiDoc formatting.
 */
@Slf4j
final class DocGenSupport {

    private DocGenSupport() {
    }

    /**
     * Deterministic method order: getMethods()/getDeclaredMethods() return methods in no particular order
     * (it may change between compilations), so sort by name then by parameter types (for overloads).
     */
    static final Comparator<Method> METHOD_ORDER = Comparator.comparing(Method::getName)
            .thenComparing(m -> Arrays.stream(m.getParameterTypes()).map(Class::getName).collect(Collectors.joining(",")));

    /** Mapper used for JSON schemas, with sorted properties: otherwise their order depends on reflection order, which is not stable */
    private static final ObjectMapper SCHEMA_MAPPER = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    static List<Method> sortedMethods(Method[] methods) {
        return Arrays.stream(methods).sorted(METHOD_ORDER).collect(Collectors.toList());
    }

    /**
     * Return the target classes of the beans annotated with the given annotation, sorted by class name.
     * (getBeansWithAnnotation follows bean registration order, which depends on classpath scanning order)
     */
    static List<Class<?>> sortedBeanClasses(ApplicationContext context, Class<? extends Annotation> annotation) {
        return context.getBeansWithAnnotation(annotation).values().stream()
                .<Class<?>>map(AopUtils::getTargetClass)
                .sorted(Comparator.comparing(Class::getName))
                .collect(Collectors.toList());
    }

    /** Return the pretty printed JSON schema of the given (possibly generic) type, followed by a new line */
    static String buildJsonSchema(Type type) {
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(SCHEMA_MAPPER);
        try {
            JsonSchema schema = schemaGen.generateSchema(SCHEMA_MAPPER.constructType(type));
            return SCHEMA_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(schema) + "\n";
        } catch (JsonProcessingException e) {
            log.error("Error generating json schema for type " + type.getTypeName(), e);
        }
        return "";
    }

    /**
     * Append the description of a payload type: its name for simple types,
     * otherwise its JSON schema in an AsciiDoc source block.
     */
    static void appendPayload(StringBuilder doc, Type payloadType) {
        doc.append("`").append(simpleTypeName(payloadType)).append("`\n\n");
        if (!isSimpleType(payloadType)) {
            doc.append("[source,json]\n----\n");
            doc.append(buildJsonSchema(payloadType));
            doc.append("----\n\n");
        }
    }

    /** Short name of a type, with the short names of its type arguments if it is generic (e.g. {@code List<MachineDTO>}) */
    static String simpleTypeName(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz.getSimpleName();
        }
        if (type instanceof ParameterizedType parameterized) {
            return simpleTypeName(parameterized.getRawType()) + "<"
                    + Arrays.stream(parameterized.getActualTypeArguments())
                        .map(DocGenSupport::simpleTypeName)
                        .collect(Collectors.joining(", "))
                    + ">";
        }
        return type.getTypeName();
    }

    static boolean isSimpleType(Type type) {
        if (!(type instanceof Class<?> clazz)) {
            return false;
        }
        return clazz.isPrimitive() ||
               clazz == String.class ||
               Number.class.isAssignableFrom(clazz) ||
               clazz == Boolean.class ||
               clazz == Character.class ||
               clazz.isEnum();
    }

    static String toAsciiDocAnchor(String input) {
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
     * Make a topic or URL path splittable on several lines when displayed (keeping its case),
     * by appending an invisible (zero width) space after each "/".
     */
    static String toSplittaString(String input) {
        if (input == null) return "";
        return input.replace("/", "/&#8203;");
    }

    /** Escape a text so it can be used in an AsciiDoc table cell */
    static String toTableCell(String text) {
        return text.replace("|", "\\|");
    }

    /** Write the given AsciiDoc content to the given path, creating its parent directories if needed */
    static void writeDoc(Path outputPath, String content) throws IOException {
        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, content.getBytes(StandardCharsets.UTF_8));
        log.info("Generated protocol documentation: " + outputPath);
    }
}
