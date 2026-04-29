package fr.inria.mbdo.mission.switchs;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import fr.inria.mbdo.mission.common.MissionConfiguration;
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
import java.util.List;
import java.util.stream.Collectors;

import static fr.inria.mbdo.mission.utils.StringUtils.toUpperFirst;

public class CodeBlockSpecGeneratorSwitch extends SysmlSwitch<List<CodeBlock>> {
    private static final Logger logger = LoggerFactory.getLogger(CodeBlockSpecGeneratorSwitch.class);

    public TransformationContext context;

    public CodeBlockSpecGeneratorSwitch(TransformationContext transformationContext) {
        this.context = transformationContext;
    }



    /**
     * Look into owned children elements
     * @param object
     * @return List of JavaFile generated from the traversal
     */
    private List<CodeBlock> doSwitchForAllOwnedElements(Element object) {
        return object.getOwnedElement().stream()
                .flatMap(o -> doSwitch(o).stream())
                .toList();
    }
}

