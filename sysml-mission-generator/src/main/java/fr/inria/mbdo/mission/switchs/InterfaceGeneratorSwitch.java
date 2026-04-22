package fr.inria.mbdo.mission.switchs;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import fr.inria.mbdo.mission.generators.TransformationContext;
import org.eclipse.emf.common.util.EList;
import org.eclipse.syson.sysml.*;
import org.eclipse.syson.sysml.Package;
import org.eclipse.syson.sysml.util.SysmlSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static fr.inria.mbdo.mission.utils.StringUtils.toUpperFirst;
import static fr.inria.mbdo.mission.utils.SymlUtils.getParentJavaPackageQualifiedName;

public class InterfaceGeneratorSwitch extends SysmlSwitch<List<String>> {
    private static final Logger logger = LoggerFactory.getLogger(InterfaceGeneratorSwitch.class);

    private final TransformationContext context;

    public InterfaceGeneratorSwitch(TransformationContext transformationContext) {
        this.context = transformationContext;
    }

    @Override
    public List<String> casePackage(Package object) {
        List<String> result = new ArrayList<>();
        result.addAll(doSwitchForAllOwnedElements(object)); // look into children, as this is a package
        return result;
    }

    @Override
    public List<String> casePartDefinition(PartDefinition object) {
        logger.debug("traversing PartDefinition {}", object.getName());

        context.addIndirectTypeToGenerate(object);

        logger.info("Added new entry to types: "+context.resolveType(object) + " with key " + object.getQualifiedName());

        TypeSpec.Builder partDefInterfaceBuilder = TypeSpec.interfaceBuilder(object.getName())
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("From $L\n$L", object.getQualifiedName(), getDocString(object.getDocumentation()));

        for (AttributeUsage ownedAttribute : object.getOwnedAttribute()) {

            // attribute type

            MethodSpec.Builder getterBuilder = MethodSpec
                    .methodBuilder("get" + toUpperFirst(ownedAttribute.getName()))
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
            logger.debug("computing Java type for attribute {}", ownedAttribute.getQualifiedName());
            if (ownedAttribute.getType().isEmpty()) {
                logger.error("Missing attribute type for {}", ownedAttribute.getQualifiedName());
            }
            if (ownedAttribute.getType().size() > 1) {
                logger.error("Multiple type attribute not supported for {}", ownedAttribute.getQualifiedName());
            }
            Type attributeType = ownedAttribute.getType().getFirst();
            // possibly asks to generate the java class for the type
            context.addIndirectTypeToGenerate(attributeType);
            TypeName typeName = context.resolveType(attributeType);
            getterBuilder.returns(typeName);

            MethodSpec.Builder setterBuilder = MethodSpec
                    .methodBuilder("set" + toUpperFirst(ownedAttribute.getName()))
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(typeName, ownedAttribute.getName());

            partDefInterfaceBuilder.addMethod(setterBuilder.build());
            partDefInterfaceBuilder.addMethod(getterBuilder.build());
        }

        for (ActionUsage action : object.getOwnedAction()) {
            if (action instanceof PerformActionUsage) {
                PerformActionUsage pau = (PerformActionUsage) action;
                logger.error(pau.getType().toString());
                if (pau.getType().size() == 0) {
                    logger.error("Missing Action definition for PerformActionUsage {}", pau.getQualifiedName());
                } else {
                    if (pau.getType().size() > 1) {
                        logger.error(
                                "Too many Type associated to PerformActionUsage {}, only the first one will be used",
                                pau.getQualifiedName());
                    }
                    switch (pau.getType().get(0)) {
                        case ActionDefinition ad -> {
                            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(ad.getName())
                                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                    .addJavadoc("From $L\n$L", ad.getQualifiedName(), getDocString(ad.getDocumentation()));
                            for (Feature param : ad.getParameter()) {
                                Type attributeType = param.getType().getFirst();
                                // possibly asks to generate the java class for the type
                                context.addIndirectTypeToGenerate(attributeType);
                                TypeName typeName = context.resolveType(attributeType);
                                methodBuilder.addParameter(typeName, param.getName());
                            }
                            partDefInterfaceBuilder.addMethod(methodBuilder.build());
                        }
                        default -> throw new IllegalArgumentException("Unexpected value: " + pau.getType().get(0));
                    }
                }
            } else {
                logger.warn("ignored action {} {} in PartDefinition {}", action.getName(),
                        action.eClass().getName(), object.getName());
            }
        }

        // store javafile created for this partdef
        JavaFile javaFile = JavaFile.builder(context.getPackagePrefix() + "." + getParentJavaPackageQualifiedName(object),
                partDefInterfaceBuilder.build()).build();
        context.elementToJavaFile.put(object, javaFile);

        return new ArrayList<>(doSwitchForAllOwnedElements(object));
    }

    @Override
    public List<String> caseEnumerationDefinition(EnumerationDefinition object) {
        List<String> result = new ArrayList<>();
        logger.debug("traversing EnumerationDefinition {}", object.getName());
        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(object.getName());
        for (EnumerationUsage ev : object.getEnumeratedValue()) {
            enumBuilder.addEnumConstant(ev.getName());
        }
        JavaFile javaFile = JavaFile
                .builder(context.getPackagePrefix() + "." + getParentJavaPackageQualifiedName(object), enumBuilder.build())
                .build();
        context.elementToJavaFile.put(object, javaFile);
        return result;
    }

    /* Element is a top level inheritance */
    @Override
    public List<String> caseElement(Element object) {
        // look into owned children
        return new ArrayList<>(doSwitchForAllOwnedElements(object));
    }

    private String getDocString(EList<Documentation> docs) {
        return docs.stream().map(d -> d.getBody()).collect(Collectors.joining("\n"));
    }

    private List<String> doSwitchForAllOwnedElements(Element object) {
        List<String> result = new ArrayList<>();
        /* look into owned children */
        for (Element ownedElement : object.getOwnedElement()) {
            result.addAll(doSwitch(ownedElement));
        }
        return result;
    }
}
