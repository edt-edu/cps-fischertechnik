package fr.inria.mbdo.mission;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SysmlXMIJsonGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SysmlXMIJsonGenerator.class);

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            LOGGER.error("""
                    Usage: SysmlXMIJsonGenerator <xmiFilePath> <targetFilePath>
                        <xmiFilePath> : Path to the XMI file
                        <targetFilePath> : Path to output file
                    """);
            return;
        }

        String xmiFilePath = args[0];
        String targetFilePath = args[1];
        new SysmlXMIJsonGenerator().toJson(xmiFilePath, targetFilePath);
    }

    public void toJson(String xmiFilePath, String targetFilePath) throws IOException {
        File xmiFile = new File(xmiFilePath);
        if (!xmiFile.exists()) {
            LOGGER.error("File not found: {}", xmiFilePath);
            return;
        }

        final Path targetPath = Path.of(targetFilePath);
        if (!targetPath.getFileName().toString().endsWith(".json")) {
            LOGGER.error("That target file should have the extension .json: {}", targetFilePath);
            return;
        }

        Document document = parse(xmiFile);
        Map<String, Object> jsonTree = convert(document.getDocumentElement());
        String json = JsonWriter.toJson(jsonTree);

        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(targetPath, json, StandardCharsets.UTF_8);

        LOGGER.info("Wrote JSON structure to {}", targetPath);
    }

    private Document parse(File xmiFile) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setExpandEntityReferences(false);
            factory.setXIncludeAware(false);
            return factory.newDocumentBuilder().parse(xmiFile);
        } catch (ParserConfigurationException | SAXException exception) {
            throw new IOException("Failed to parse XMI file: " + xmiFile.getAbsolutePath(), exception);
        }
    }

    private Map<String, Object> convert(Element element) {
        String typeName = effectiveType(element);
        Map<String, Object> node = new LinkedHashMap<>();

        NamedNodeMap attributes = element.getAttributes();
        for (int index = 0; index < attributes.getLength(); index++) {
            Node attribute = attributes.item(index);
            String attributeName = attribute.getNodeName();
            if (isNamespaceDeclaration(attributeName) || isXsiType(attribute)) {
                continue;
            }
            node.put(attributeName, attribute.getNodeValue());
        }

        List<Object> children = new ArrayList<>();
        NodeList childNodes = element.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index++) {
            Node child = childNodes.item(index);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                children.add(convert((Element) child));
            }
        }

        if (!children.isEmpty()) {
            node.put("children", children);
        }

        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put(typeName, node);
        return wrapper;
    }

    private String effectiveType(Element element) {
        String xsiType = element.getAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "type");
        if (xsiType != null && !xsiType.isBlank()) {
            return xsiType;
        }
        return element.getNodeName();
    }

    private boolean isNamespaceDeclaration(String attributeName) {
        return attributeName.startsWith("xmlns");
    }

    private boolean isXsiType(Node attribute) {
        return XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI.equals(attribute.getNamespaceURI())
                && "type".equals(attribute.getLocalName());
    }

    private static final class JsonWriter {

        private JsonWriter() {
        }

        static String toJson(Object value) {
            StringBuilder builder = new StringBuilder();
            appendValue(builder, value, 0);
            return builder.toString();
        }

        private static void appendValue(StringBuilder builder, Object value, int indentLevel) {
            if (value instanceof Map<?, ?> map) {
                appendObject(builder, map, indentLevel);
                return;
            }

            if (value instanceof List<?> list) {
                appendArray(builder, list, indentLevel);
                return;
            }

            if (value instanceof String stringValue) {
                builder.append('"').append(escape(stringValue)).append('"');
                return;
            }

            if (value instanceof Number || value instanceof Boolean) {
                builder.append(value);
                return;
            }

            if (value == null) {
                builder.append("null");
                return;
            }

            builder.append('"').append(escape(value.toString())).append('"');
        }

        private static void appendObject(StringBuilder builder, Map<?, ?> map, int indentLevel) {
            builder.append('{');
            if (!map.isEmpty()) {
                builder.append('\n');
                int index = 0;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    indent(builder, indentLevel + 1);
                    builder.append('"').append(escape(String.valueOf(entry.getKey()))).append('"').append(':')
                            .append(' ');
                    appendValue(builder, entry.getValue(), indentLevel + 1);
                    if (++index < map.size()) {
                        builder.append(',');
                    }
                    builder.append('\n');
                }
                indent(builder, indentLevel);
            }
            builder.append('}');
        }

        private static void appendArray(StringBuilder builder, List<?> list, int indentLevel) {
            builder.append('[');
            if (!list.isEmpty()) {
                builder.append('\n');
                for (int index = 0; index < list.size(); index++) {
                    indent(builder, indentLevel + 1);
                    appendValue(builder, list.get(index), indentLevel + 1);
                    if (index + 1 < list.size()) {
                        builder.append(',');
                    }
                    builder.append('\n');
                }
                indent(builder, indentLevel);
            }
            builder.append(']');
        }

        private static void indent(StringBuilder builder, int indentLevel) {
            builder.append("  ".repeat(Math.max(0, indentLevel)));
        }

        private static String escape(String value) {
            return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }
}