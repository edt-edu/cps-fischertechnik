package fr.inria.mbdo.mission.generators;

import com.palantir.javapoet.JavaFile;
import fr.inria.mbdo.mission.switchs.FieldSpecGeneratorSwitch;
import fr.inria.mbdo.mission.switchs.GeneratorSwitch;
import fr.inria.mbdo.mission.switchs.MethodSpecGeneratorSwitch;
import fr.inria.mbdo.mission.switchs.TypeSpecGeneratorSwitch;
import org.eclipse.emf.ecore.EObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class GlobalGenerator {
    private static final Logger logger = LoggerFactory.getLogger(GlobalGenerator.class);

    private final TransformationContext context;

    private static GeneratorSwitch generatorSwitch;
//    private static TypeSpecGeneratorSwitch typeSpecGeneratorSwitch;
//    private static FieldSpecGeneratorSwitch fieldSpecGeneratorSwitch;
//    private static MethodSpecGeneratorSwitch methodSpecGeneratorSwitch;


    public GlobalGenerator(TransformationContext sharedContext) {
        this.context = sharedContext;
        generatorSwitch = new GeneratorSwitch(context);
    }

    public List<JavaFile> generate(EObject rootSource) {
        return generatorSwitch.doSwitch(rootSource);
    }
}
