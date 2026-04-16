package fr.inria.mbdo.mission;


import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.nio.file.Path;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

public class SysmlMissionGeneratorTest {
	@TempDir
    Path tempDir;

    @Test
    void import_CB_from_CBVGRMission() throws Exception {
        SysmlMissionGenerator generator = new SysmlMissionGenerator();

        // files form /src/test/resources
        File sysmlFile = new File(
            getClass().getClassLoader().getResource("CBVGRMission/CB.sysml").toURI()
        );
        assertTrue(sysmlFile.exists());

        ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
        
        generator.importSysml(sysmlFile, resourceSet);
        
        //generator.generate(sysmlFile.getAbsolutePath(), targetDir);

        //verify(generator, times(1)).importSysml(any(), any());
    }
    
    @Test
    void import_VGR_from_CBVGRMission() throws Exception {
        SysmlMissionGenerator generator = new SysmlMissionGenerator();
        // files form /src/test/resources
        File sysmlFile = new File(
            getClass().getClassLoader().getResource("CBVGRMission/VGR.sysml").toURI()
        );
        assertTrue(sysmlFile.exists());

        ResourceSet resourceSet = new SysMLResourceSetProvider().createSysMLResourceSet(true);
        
        generator.importSysml(sysmlFile, resourceSet);
        
        assertTrue(resourceSet.getResources().size() == 3);

    }
    
    
    
}
