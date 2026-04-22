package fr.inria.mbdo.mission.switchs;

import com.palantir.javapoet.*;
import fr.inria.mbdo.mission.common.MachineMissionStrategy;
import fr.inria.mbdo.mission.common.MissionConfiguration;
import fr.inria.mbdo.mission.common.MissionState;
import fr.inria.mbdo.mission.generators.TransformationContext;
import fr.inria.mbdo.mission.utils.StringUtils;
import org.eclipse.syson.sysml.*;
import org.eclipse.syson.sysml.util.SysmlSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.inria.mbdo.mission.utils.SymlUtils.getParentJavaPackageQualifiedName;

public class MissionGeneratorSwitch extends SysmlSwitch<List<String>> {
    private static final Logger logger = LoggerFactory.getLogger(MissionGeneratorSwitch.class);

    public TransformationContext context;

    public MissionGeneratorSwitch(TransformationContext transformationContext) {
        this.context = transformationContext;
    }

    @Override
    public List<String> caseStateDefinition(StateDefinition object) {

        TypeSpec.Builder missionStrategyBuilder = TypeSpec.classBuilder(object.getName())
                .addJavadoc("Class defining one mission...")
                .addSuperinterface(ParameterizedTypeName.get(MachineMissionStrategy.class));

        logger.debug("traversing StateDefinition " + object.getName());

        /* Define Mission class attributes here */
        List<FieldSpec> fieldSpecs = new ArrayList<>();

        FieldSpec currentStateFieldSpec = FieldSpec.builder(MissionState.class, "currentState")
                .addModifiers(Modifier.PRIVATE)
                .build();

        fieldSpecs.add(currentStateFieldSpec);

        /* Define Mission class methods here */
        List<MethodSpec> methods = new ArrayList<>();

        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addParameter(MissionConfiguration.class, "configuration")
                .addJavadoc("This constructor sets the default state for the state machine.");

        for (ReferenceUsage ref : object.getOwnedReference())
        {
            // Add attribute
            fieldSpecs.add(FieldSpec.builder(context.resolveType(ref.getDefinition().getFirst()), ref.getName())
                    .addModifiers(Modifier.PRIVATE)
                    .build()
            );

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

        /* Process all transitions defined for states */
        Map<String, List<TransitionUsage>> transitions = new HashMap<>();

        for (TransitionUsage transition : object.getOwnedTransition()) {
            logger.debug("\t[1] Found transition from " + transition.getSource().getName() + " to "
                    + transition.getTarget().getName());

            ActionUsage from = transition.getSource();
            ActionUsage to = transition.getTarget();

            if (from.getName() == null) {

                // default transition
                constructorBuilder
                        .addStatement("currentState = new " + to.getName() + "()")
                        .addStatement("currentState.enter()");
                // TODO add defult transition action
            } else {
                String stateKey = from.getName();
                List<TransitionUsage> transitionsForState = transitions.containsKey(stateKey)
                        ? transitions.get(stateKey)
                        : new ArrayList<>();
                transitionsForState.add(transition);
                transitions.put(stateKey, transitionsForState);
            }
        }

        /*
         * Process all actions defined in the state definition that are not StateUsage
         */
        for (Element element : object.getAction()) {
            // logger.debug("\t\t[1] traversing action usage " + element.getName());
        }

        methods.add(constructorBuilder.build());

        /* Generate all states with their transitions */
        List<TypeSpec> states = generateStates(object, transitions);

        missionStrategyBuilder
                .addMethods(methods)
                .addFields(fieldSpecs)
                .addTypes(states);

        // store javafile created for this partdef
        JavaFile javaFile = JavaFile.builder(context.getPackagePrefix() + "." + getParentJavaPackageQualifiedName(object),
                missionStrategyBuilder.build()).build();
        context.elementToJavaFile.put(object, javaFile);



        return new ArrayList<>();
    }

    /* Element is a top level inheritance */
    @Override
    public List<String> caseElement(Element object) {
        // look into owned children
        return new ArrayList<>(doSwitchForAllOwnedElements(object));
    }

    public List<String> doSwitchForAllOwnedElements(Element object) {
        List<String> result = new ArrayList<>();
        /* look into owned children */
        for (Element ownedElement : object.getOwnedElement()) {
            result.addAll(doSwitch(ownedElement));
        }
        return result;
    }

    private List<TypeSpec> generateStates(StateDefinition object, Map<String, List<TransitionUsage>> transitions) {
        List<TypeSpec> result = new ArrayList<>();

        // For all states: build an inner class
        for (StateUsage stateUsage : object.getOwnedState()) {

            logger.debug("[state generation] Found state usage: " + stateUsage.getName());

            MethodSpec.Builder enterMethodBuilder = MethodSpec.methodBuilder("enter")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class);

            MethodSpec.Builder exitMethodBuilder = MethodSpec.methodBuilder("exit")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addStatement("return");

            String stateKey = stateUsage.getName();
            for (TransitionUsage transition : transitions.get(stateKey)) {
                for (ActionUsage au: transition.getTriggerAction()){
                    logger.debug("[state generation] Found trigger action: " + au.getName());
                }

                if(transition.getGuardExpression() != null) {
                    logger.debug("[state generation] Found guard expression: " + transition.getGuardExpression());

                }
                enterMethodBuilder.addStatement("// TODO: support for transition");
            }

            TypeSpec stateUsageSpec = TypeSpec.classBuilder(stateUsage.getName())
                    .addSuperinterface(ParameterizedTypeName.get(MissionState.class))
                    .addMethod(enterMethodBuilder.build())
                    .addMethod(exitMethodBuilder.build())
                    .build();

            result.add(stateUsageSpec);
        }

        return result;
    }
}

