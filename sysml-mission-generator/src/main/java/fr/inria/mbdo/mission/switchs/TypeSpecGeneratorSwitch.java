package fr.inria.mbdo.mission.switchs;

import com.palantir.javapoet.*;
import fr.inria.mbdo.mission.common.MachineMissionStrategy;
import fr.inria.mbdo.mission.common.MissionState;
import fr.inria.mbdo.mission.generators.TransformationContext;
import org.eclipse.emf.common.util.EList;
import org.eclipse.syson.sysml.*;
import org.eclipse.syson.sysml.util.SysmlSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.stream.Collectors;

public class TypeSpecGeneratorSwitch extends SysmlSwitch<List<TypeSpec>> {

    private static final Logger logger = LoggerFactory.getLogger(TypeSpecGeneratorSwitch.class);

    private final TransformationContext context;

    private final FieldSpecGeneratorSwitch fieldSpecGeneratorSwitch;
    private final MethodSpecGeneratorSwitch methodSpecGeneratorSwitch;

    public TypeSpecGeneratorSwitch(TransformationContext transformationContext) {
        this.context = transformationContext;
        this.fieldSpecGeneratorSwitch = new FieldSpecGeneratorSwitch(transformationContext);
        this.methodSpecGeneratorSwitch = new MethodSpecGeneratorSwitch(transformationContext);
    }

    /**
     * Global switch for all elements
     * @param object the target of the switch.
     * @return
     */
    @Override
    public List<TypeSpec> caseElement(Element object) {
        return doSwitchForAllOwnedElements(object);
    }

    @Override
    public List<TypeSpec> casePartDefinition(PartDefinition object) {
        logger.debug("traversing PartDefinition {}", object.getName());

        context.addIndirectTypeToGenerate(object);
        logger.info("Added new entry to types: "+context.resolveType(object) + " with key " + object.getQualifiedName());

        TypeSpec partDefInterfaceSpec = TypeSpec.interfaceBuilder(object.getName())
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("From $L\n$L", object.getQualifiedName(), getDocString(object.getDocumentation()))
                .addMethods(methodSpecGeneratorSwitch.doSwitch(object))
                .build();

        return List.of(partDefInterfaceSpec);
    }

    @Override
    public List<TypeSpec> caseEnumerationDefinition(EnumerationDefinition object) {

        logger.debug("traversing EnumerationDefinition {}", object.getName());
        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(object.getName())
                .addJavadoc("From $L\n$L", object.getQualifiedName(), getDocString(object.getDocumentation()));

        for (EnumerationUsage ev : object.getEnumeratedValue()) {
            enumBuilder.addEnumConstant(ev.getName());
        }

        return List.of(enumBuilder.build());
    }

    @Override
    public List<TypeSpec> caseStateDefinition(StateDefinition object) {

        TypeSpec missionStrategy = TypeSpec.classBuilder(object.getName())
                .addJavadoc("Class defining one mission...")
                .addSuperinterface(ParameterizedTypeName.get(MachineMissionStrategy.class))
                .addFields(fieldSpecGeneratorSwitch.doSwitch(object))
                .addMethods(methodSpecGeneratorSwitch.doSwitch(object))
                .addTypes(object.getOwnedState().stream().flatMap(su->doSwitch(su).stream()).toList())
                .build();

        return List.of(missionStrategy);
    }

    @Override
    public List<TypeSpec> caseStateUsage(StateUsage object) {
        return List.of(TypeSpec.classBuilder(object.getName())
                .addSuperinterface(ParameterizedTypeName.get(MissionState.class))
                .addMethods(methodSpecGeneratorSwitch.doSwitch(object))
                .build());
    }

    private String getDocString(EList<Documentation> docs) {
        return docs.stream().map(Comment::getBody).collect(Collectors.joining("\n"));
    }

    /**
     * Look into owned children elements
     * @param object
     * @return List of JavaFile generated from the traversal
     */
    private List<TypeSpec> doSwitchForAllOwnedElements(Element object) {
        return object.getOwnedElement().stream()
                .flatMap(o -> doSwitch(o).stream())
                .toList();
    }
}

