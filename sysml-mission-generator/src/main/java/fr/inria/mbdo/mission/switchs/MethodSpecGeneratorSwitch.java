package fr.inria.mbdo.mission.switchs;

import com.palantir.javapoet.*;
import fr.inria.mbdo.mission.common.MachineMissionStrategy;
import fr.inria.mbdo.mission.common.MissionConfiguration;
import fr.inria.mbdo.mission.common.MissionState;
import fr.inria.mbdo.mission.generators.TransformationContext;
import fr.inria.mbdo.mission.utils.StringUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.syson.sysml.*;
import org.eclipse.syson.sysml.Package;
import org.eclipse.syson.sysml.util.SysmlSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.inria.mbdo.mission.utils.StringUtils.toUpperFirst;

public class MethodSpecGeneratorSwitch extends SysmlSwitch<List<MethodSpec>> {
    private static final Logger logger = LoggerFactory.getLogger(MethodSpecGeneratorSwitch.class);

    public TransformationContext context;

    public MethodSpecGeneratorSwitch(TransformationContext transformationContext) {
        this.context = transformationContext;
    }

    @Override
    public List<MethodSpec> casePackage(Package object) {
        return doSwitchForAllOwnedElements(object);
    }

    @Override
    public List<MethodSpec> casePartDefinition(PartDefinition object) {
        List<MethodSpec> methods = new ArrayList<>();

        for (AttributeUsage ownedAttribute : object.getOwnedAttribute()) {
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

            methods.add(getterBuilder.build());
            methods.add(setterBuilder.build());
        }

        for (ActionUsage ownedAction : object.getOwnedAction()) {
             List<MethodSpec> tmp = doSwitch(ownedAction);
            if(tmp != null) {
                methods.addAll(tmp);

            }else {
                logger.debug("methods for action {} are null", ownedAction.getQualifiedName());
            }
        }
        // methods.addAll(object.getOwnedAction().stream().flatMap(a->doSwitch(a).stream()).toList());

        return methods;
    }

    @Override
    public List<MethodSpec> caseActionUsage(ActionUsage object) {
        logger.debug("computing Java type for action usage {}", object.getQualifiedName());
        return List.of();
    }

    @Override
    public List<MethodSpec> casePerformActionUsage(PerformActionUsage object) {
        logger.debug("computing Java type for performed action usage {}", object.getQualifiedName());
        List<MethodSpec> methods = new ArrayList<>();

        if (object.getType().size() == 0) {
            logger.error("Missing Action definition for PerformActionUsage {}", object.getQualifiedName());
        } else {
            if (object.getType().size() > 1) {
                logger.error(
                        "Too many Type associated to PerformActionUsage {}, only the first one will be used",
                        object.getQualifiedName());
            }
            switch (object.getType().get(0)) {
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
                    methods.add(methodBuilder.build());
                }
                default -> throw new IllegalArgumentException("Unexpected value: " + object.getType().get(0));
            }
        }

        return  methods;
    }

    @Override
    public List<MethodSpec> caseStateDefinition(StateDefinition object) {
        List<MethodSpec> methods = new ArrayList<>();

        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addParameter(MissionConfiguration.class, "configuration")
                .addJavadoc("This constructor sets the default state for the state machine.");

        for (ReferenceUsage ref : object.getOwnedReference())
        {
            // Add attribute initialization in constructor
            constructorBuilder.addStatement(ref.getName() + " = configuration.getMachine(\"" + ref.getName() +"\")");

            // Add attribute getter
            methods.add(MethodSpec.methodBuilder("get"+ StringUtils.toUpperFirst(ref.getName()))
                    .addModifiers(Modifier.PROTECTED)
                    .addStatement("return " + ref.getName())
                    .returns(context.resolveType(ref.getDefinition().getFirst()))
                    .build()
            );
        }

        /* Process entry action and default transition if any */
        if (object.getEntryAction() != null && object.getEntryAction().getName() != null) {
            constructorBuilder.addStatement(object.getEntryAction().getName() + "()");
        }

        methods.add(constructorBuilder.build());

        return methods;
    }

    @Override
    public List<MethodSpec> caseStateUsage(StateUsage object) {
        logger.debug("[state generation] Found state usage: " + object.getName());

        MethodSpec.Builder enterMethodBuilder = MethodSpec.methodBuilder("enter")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class);

        MethodSpec.Builder exitMethodBuilder = MethodSpec.methodBuilder("exit")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addStatement("return");

//        String stateKey = object.getName();
//        for (TransitionUsage transition : transitions.get(stateKey)) {
//            for (ActionUsage au: transition.getTriggerAction()){
//                logger.debug("[state generation] Found trigger action: " + au.getName());
//            }
//
//            if(transition.getGuardExpression() != null) {
//                logger.debug("[state generation] Found guard expression: " + transition.getGuardExpression());
//
//            }
//            enterMethodBuilder.addStatement("// TODO: support for transition");
//        }

        return List.of(enterMethodBuilder.build(),  exitMethodBuilder.build());
    }

    private String getDocString(EList<Documentation> docs) {
        return docs.stream().map(d -> d.getBody()).collect(Collectors.joining("\n"));
    }

    /**
     * Look into owned children elements
     * @param object
     * @return List of JavaFile generated from the traversal
     */
    private List<MethodSpec> doSwitchForAllOwnedElements(Element object) {
        return object.getOwnedElement().stream()
                .flatMap(o -> doSwitch(o).stream())
                .toList();
    }
}

