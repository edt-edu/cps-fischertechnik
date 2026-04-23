package fr.inria.mbdo.mission.switchs;

import com.palantir.javapoet.*;
import fr.inria.mbdo.mission.common.MissionState;
import fr.inria.mbdo.mission.generators.TransformationContext;
import org.eclipse.syson.sysml.*;
import org.eclipse.syson.sysml.util.SysmlSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;

public class FieldSpecGeneratorSwitch extends SysmlSwitch<List<FieldSpec>> {
    private static final Logger logger = LoggerFactory.getLogger(FieldSpecGeneratorSwitch.class);

    public TransformationContext context;

    public FieldSpecGeneratorSwitch(TransformationContext transformationContext) {
        this.context = transformationContext;
    }

    @Override
    public List<FieldSpec> caseStateDefinition(StateDefinition object) {
        List<FieldSpec> fields = new ArrayList<>();

        fields.add(FieldSpec.builder(MissionState.class, "currentState")
                .addModifiers(Modifier.PRIVATE)
                .build());

        for (ReferenceUsage ref : object.getOwnedReference())
        {
            // Add attribute
            fields.add(FieldSpec.builder(context.resolveType(ref.getDefinition().getFirst()), ref.getName())
                    .addModifiers(Modifier.PRIVATE)
                    .build()
            );
        }
        return fields;
    }
}

