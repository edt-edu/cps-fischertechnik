package fr.inria.mbdo.mission.generators;

import fr.inria.mbdo.mission.switchs.InterfaceGeneratorSwitch;
import fr.inria.mbdo.mission.switchs.MissionGeneratorSwitch;
import org.eclipse.emf.ecore.EObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalGenerator {
    private static final Logger logger = LoggerFactory.getLogger(GlobalGenerator.class);

    MissionGeneratorSwitch missionsGeneratorSwitch;
    InterfaceGeneratorSwitch interfaceGeneratorSwitch;
    TransformationContext context;

    public GlobalGenerator(TransformationContext sharedContext) {
        this.context = sharedContext;

        this.missionsGeneratorSwitch = new MissionGeneratorSwitch(context);
        this.interfaceGeneratorSwitch = new InterfaceGeneratorSwitch(context);
    }

    public TransformationContext getContext() {
        return context;
    }

    public void generate(EObject rootSource) {
        interfaceGeneratorSwitch.doSwitch(rootSource);
        missionsGeneratorSwitch.doSwitch(rootSource);
    }
}
