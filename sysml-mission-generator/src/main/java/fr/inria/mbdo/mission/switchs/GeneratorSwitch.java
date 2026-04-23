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

import static fr.inria.mbdo.mission.utils.SymlUtils.getParentJavaPackageQualifiedName;

public class GeneratorSwitch extends SysmlSwitch<List<JavaFile>> {
    private static final Logger logger = LoggerFactory.getLogger(GeneratorSwitch.class);

    private final TransformationContext context;

    private final TypeSpecGeneratorSwitch typeSpecGeneratorSwitch;

    public GeneratorSwitch(TransformationContext transformationContext) {
        this.context = transformationContext;
        typeSpecGeneratorSwitch = new TypeSpecGeneratorSwitch(context);
    }

    @Override
    public List<JavaFile> caseDefinition(Definition object) {
        List<TypeSpec> typeSpecs = typeSpecGeneratorSwitch.doSwitch(object);

        return typeSpecs.stream().map(ts->
                JavaFile.builder(context.getPackagePrefix() + "." + getParentJavaPackageQualifiedName(object),
                        ts).build()).collect(Collectors.toList());
    }

    @Override
    public List<JavaFile> caseElement(Element object) {
        return doSwitchForAllOwnedElements(object);
    }

    /**
     * Look into owned children elements
     * @param object
     * @return List of JavaFile generated from the traversal
     */
    private List<JavaFile> doSwitchForAllOwnedElements(Element object) {
        return object.getOwnedElement().stream()
                .flatMap(o -> doSwitch(o).stream())
                .toList();
    }
}
