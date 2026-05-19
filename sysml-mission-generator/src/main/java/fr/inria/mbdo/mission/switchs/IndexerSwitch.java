package fr.inria.mbdo.mission.switchs;

import fr.inria.mbdo.mission.generators.SymbolIndex;
import org.eclipse.syson.sysml.*;
import org.eclipse.syson.sysml.util.SysmlSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexerSwitch extends SysmlSwitch<Void> {

    private static final Logger logger = LoggerFactory.getLogger(IndexerSwitch.class);

    private final SymbolIndex.Builder indexBuilder;

    public IndexerSwitch(SymbolIndex.Builder indexBuilder) {
        this.indexBuilder = indexBuilder;
    }

    @Override
    public Void casePartDefinition(PartDefinition object) {
        indexBuilder.addPart(object);
        return null;
    }

    @Override
    public Void caseEnumerationDefinition(EnumerationDefinition object) {
        indexBuilder.addEnum(object);
        return null;
    }

    @Override
    public Void caseItemDefinition(ItemDefinition object) {
        indexBuilder.addItem(object);
        return null;
    }

    @Override
    public Void caseStateDefinition(StateDefinition object) {
        indexBuilder.addState(object);
        return null;
    }

    @Override
    public Void caseActionDefinition(ActionDefinition object) {
        logger.debug("[Action]\tTraversing action def: {}", object.getQualifiedName());
        indexBuilder.addAction(object);
        return null;
    }

    @Override
    public Void caseAttributeDefinition(AttributeDefinition object) {
        logger.debug("[Attribute]\tTraversing attribute def: {}", object.getQualifiedName());
        indexBuilder.addAttribute(object);
        return null;
    }

    @Override
    public Void caseElement(Element object) {
        return doSwitchForAllOwnedElements(object);
    }

    /**
     * Look into owned children elements
     * @param object
     * @return List of JavaFile generated from the traversal
     */
    private Void doSwitchForAllOwnedElements(Element object) {
        object.getOwnedElement().forEach(this::doSwitch);
        return null;
    }
}
