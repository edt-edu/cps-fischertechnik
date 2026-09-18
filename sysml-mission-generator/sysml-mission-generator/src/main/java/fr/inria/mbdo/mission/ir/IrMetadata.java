package fr.inria.mbdo.mission.ir;

/**
 * Carries the identity fields that every IR node derives from its source SysML element.
 * Constructed in {@code ToIrSwitch} by extracting values from EMF objects;
 * the IR package itself has no dependency on {@code org.eclipse.syson}.
 */
public record IrMetadata(String namespace, String name, String documentation, String sourceUri) {
}
