package fr.inria.mbdo.mission;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.ECrossReferenceAdapter;
import org.eclipse.syson.application.configuration.SysONDefaultLibrariesConfiguration;
import org.eclipse.syson.application.libraries.SysONLibraryLoader;
import org.eclipse.syson.sysml.SysmlPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Provider of SysML ready ResourceSets.
 *
 * <p>Any SysML resource using a {@link SysmlPackage#eNS_URI} nsURI can be loaded in this ResourceSetSet.
 * The standard libraries can be loaded. They will be using the same resources URIs than in a regular SysON {@link org.eclipse.sirius.web.application.editingcontext.EditingContext}.</p>
 *
 * @author Arthur Daussy
 */
public class SysMLResourceSetProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SysMLResourceSetProvider.class);

    /**
     * Creates a new {@link ResourceSet} ready to load SysML resources.
     *
     * @param includeStandardLib
     *         holds true if the standard libraries needs to be loaded
     * @return a new {@link ResourceSet}
     */
    public ResourceSet createSysMLResourceSet(boolean includeStandardLib) {

        ResourceSetImpl resourceSet = new ResourceSetImpl();
        resourceSet.getPackageRegistry().put(SysmlPackage.eNS_URI, SysmlPackage.eINSTANCE);

        resourceSet.eAdapters().add(new ECrossReferenceAdapter());

        if (includeStandardLib) {
            SysONLibraryLoader libLoader = new SysONLibraryLoader();
            libLoader.loadLibraryResources(resourceSet, SysONDefaultLibrariesConfiguration.KERML);
            libLoader.loadLibraryResources(resourceSet, SysONDefaultLibrariesConfiguration.SYSML);

        }
        return resourceSet;
    }

}
