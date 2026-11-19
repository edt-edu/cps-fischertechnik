package io.github.mbdo.factoryscada.docgen;

import static io.github.mbdo.factoryscada.docgen.DocGenSupport.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.mbdo.factoryscada.frontend.RestDoc;
import lombok.extern.slf4j.Slf4j;

/**
 * Generates an AsciiDoc documentation of the REST (HTTP) API exposed by this backend.
 * <p>
 * Runs once at application startup (as an {@link ApplicationRunner}) and introspects the Spring
 * application context to collect the handler methods of the {@link RestController} beans, i.e. their methods
 * annotated with {@link RequestMapping} or one of its variants ({@code @GetMapping}, {@code @PostMapping}...);
 * the class-level {@code @RequestMapping} prefix is prepended. For each endpoint, it documents the HTTP method,
 * the path, the path and query parameters, the request body and the response, with the {@link RestDoc}
 * descriptions. Request and response types are documented as JSON schemas.
 * <p>
 * The result is written to {@code docgen/rest-endpoints.adoc} (relative to the working directory).
 * <p>
 * The output is deterministic for a given application: controllers are sorted by class name, endpoints by path
 * then HTTP method, and JSON schema properties by name, so that regenerating the file only produces git
 * differences when the API actually changes.
 * <p>
 * The WebSocket (STOMP) protocol is documented by {@link WebSocketDocGenerator}.
 */
@Slf4j
@Component
public class RestDocGenerator implements ApplicationRunner {

    @Autowired
    private ApplicationContext context;

    @Value("${spring.application.name}")
    private String applicationName;

    /** A REST endpoint: a handler method with its resolved HTTP methods and full path */
    private record Endpoint(Class<?> controller, Method method, String httpMethods, String path) {

        String anchor() {
            return "rest_" + toAsciiDocAnchor(httpMethods + " " + path);
        }
    }

    private static final Comparator<Endpoint> ENDPOINT_ORDER = Comparator.comparing(Endpoint::path)
            .thenComparing(Endpoint::httpMethods)
            .thenComparing(Endpoint::method, METHOD_ORDER);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Map<String, List<Endpoint>> endpointsByController = collectEndpoints();

        StringBuilder doc = new StringBuilder("= REST API of " + applicationName + " backend\n\n");
        doc.append("All paths are relative to the HTTP base URL of the backend (e.g. `http://localhost:9000`).\n\n");
        doc.append(generateOverview(endpointsByController));
        doc.append(generateEndpointsDetails(endpointsByController));

        writeDoc(Paths.get("docgen/rest-endpoints.adoc"), doc.toString());
    }

    /** Collect the endpoints of every REST controller, per controller simple class name (sorted for a stable output) */
    private Map<String, List<Endpoint>> collectEndpoints() {
        Map<String, List<Endpoint>> endpointsByController = new TreeMap<>();
        List<Class<?>> controllerClasses = sortedBeanClasses(context, RestController.class);
        log.info("Found REST controller classes: " + controllerClasses);
        for (Class<?> clazz : controllerClasses) {
            RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(clazz, RequestMapping.class);
            String classPath = classMapping != null ? String.join(", ", classMapping.path()) : "";
            List<Endpoint> endpoints = new ArrayList<>();
            for (Method method : sortedMethods(clazz.getMethods())) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null || method.isAnnotationPresent(ExceptionHandler.class)) {
                    continue;
                }
                String httpMethods = mapping.method().length == 0
                        ? "ANY"
                        : Arrays.stream(mapping.method()).map(RequestMethod::name).sorted().collect(Collectors.joining(", "));
                String methodPath = String.join(", ", mapping.path());
                endpoints.add(new Endpoint(clazz, method, httpMethods, classPath + methodPath));
            }
            endpoints.sort(ENDPOINT_ORDER);
            if (!endpoints.isEmpty()) {
                endpointsByController.put(clazz.getSimpleName(), endpoints);
            }
        }
        return endpointsByController;
    }

    /** Generate the overview: one table of endpoints per controller */
    private String generateOverview(Map<String, List<Endpoint>> endpointsByController) {
        StringBuilder doc = new StringBuilder("== Endpoints overview\n\n");
        for (Entry<String, List<Endpoint>> controllerEntry : endpointsByController.entrySet()) {
            doc.append("=== ").append(controllerEntry.getKey()).append("\n\n");
            doc.append("[cols=\"1,3,4,2,2\", options=\"header\"]\n");
            doc.append("|===\n");
            doc.append("|Method |Path |Description |Request body |Response\n\n");
            for (Endpoint endpoint : controllerEntry.getValue()) {
                RestDoc restDoc = endpoint.method().getAnnotation(RestDoc.class);
                Type requestBody = findRequestBodyType(endpoint.method());
                Type response = responseType(endpoint.method());
                doc.append("|").append(endpoint.httpMethods()).append("\n");
                doc.append("|<<").append(endpoint.anchor()).append(",").append(toSplittaString(endpoint.path())).append(">>\n");
                doc.append("|").append(restDoc != null ? toTableCell(restDoc.description()) : "").append("\n");
                doc.append("|").append(requestBody != null ? "`" + simpleTypeName(requestBody) + "`" : "").append("\n");
                doc.append("|").append(isVoid(response) ? "" : "`" + simpleTypeName(response) + "`").append("\n\n");
            }
            doc.append("|===\n\n");
        }
        return doc.toString();
    }

    /** Generate one section per endpoint, grouped by controller */
    private String generateEndpointsDetails(Map<String, List<Endpoint>> endpointsByController) {
        StringBuilder doc = new StringBuilder("== Endpoints\n\n");
        for (List<Endpoint> endpoints : endpointsByController.values()) {
            for (Endpoint endpoint : endpoints) {
                Method method = endpoint.method();
                doc.append("[[").append(endpoint.anchor()).append("]]\n");
                doc.append("=== `").append(endpoint.httpMethods()).append(" ").append(endpoint.path()).append("`\n\n");
                doc.append("Declared in `").append(endpoint.controller().getName()).append(".").append(method.getName()).append("()`\n\n");
                RestDoc restDoc = method.getAnnotation(RestDoc.class);
                if (restDoc != null && !restDoc.description().isEmpty()) {
                    doc.append("_").append(restDoc.description()).append("_\n\n");
                }

                List<String> parameters = new ArrayList<>();
                for (Parameter param : method.getParameters()) {
                    PathVariable pathVariable = AnnotatedElementUtils.findMergedAnnotation(param, PathVariable.class);
                    RequestParam requestParam = AnnotatedElementUtils.findMergedAnnotation(param, RequestParam.class);
                    if (pathVariable != null) {
                        String name = pathVariable.name().isEmpty() ? param.getName() : pathVariable.name();
                        parameters.add("- `" + name + "` (" + param.getType().getSimpleName() + ", path variable)");
                    } else if (requestParam != null) {
                        String name = requestParam.name().isEmpty() ? param.getName() : requestParam.name();
                        parameters.add("- `" + name + "` (" + param.getType().getSimpleName() + ", query parameter"
                                + (requestParam.required() ? "" : ", optional") + ")");
                    }
                }
                if (!parameters.isEmpty()) {
                    doc.append("==== Parameters\n\n").append(String.join("\n", parameters)).append("\n\n");
                }

                Type requestBody = findRequestBodyType(method);
                if (requestBody != null) {
                    doc.append("==== Request Body\n\n");
                    appendPayload(doc, requestBody);
                }

                doc.append("==== Response\n\n");
                if (restDoc != null && !restDoc.responseDescription().isEmpty()) {
                    doc.append("_").append(restDoc.responseDescription()).append("_\n\n");
                }
                Type response = responseType(method);
                if (isVoid(response)) {
                    doc.append("No body.\n\n");
                } else {
                    appendPayload(doc, response);
                }
            }
        }
        return doc.toString();
    }

    /** Return the (generic) type of the parameter annotated with {@link RequestBody}, or null if there is none */
    private static Type findRequestBodyType(Method method) {
        for (Parameter param : method.getParameters()) {
            if (param.isAnnotationPresent(RequestBody.class)) {
                return param.getParameterizedType();
            }
        }
        return null;
    }

    /** Return the type of the response body: the generic return type, unwrapped from {@link ResponseEntity} if needed */
    private static Type responseType(Method method) {
        Type type = method.getGenericReturnType();
        if (type instanceof ParameterizedType parameterized && parameterized.getRawType() == ResponseEntity.class) {
            return parameterized.getActualTypeArguments()[0];
        }
        return type;
    }

    private static boolean isVoid(Type type) {
        return type == Void.class || type == void.class;
    }
}
