package io.github.mbdo.factoryscada.service;

import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryScadaConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Service that allow to read in the factory configuration file the mapping between physical machine name/id and logical names used within the factory
 * A logical name can be used for example in the mission systems.
 * A given physical machine can have several logical names (in different mission systems for example
 */
@Slf4j
@Service
public class MachineNameMappingService {

    private final FactoryScadaConfigurationProvider factoryScadaConfigurationProvider;

    private final HashMap<String, String> logicalNameToMachineName = new HashMap<>();
    private final HashMap<String, List<String>> machineNameToLogicalNames = new HashMap<>();

    public MachineNameMappingService(FactoryScadaConfigurationProvider factoryScadaConfigurationProvider) {
        this.factoryScadaConfigurationProvider = factoryScadaConfigurationProvider;
        loadMappings();
    }

    private void loadMappings() {
        final List<FactoryScadaConfiguration.MachineNameMapping> mappings = factoryScadaConfigurationProvider.getFactoryScadaConfiguration().machineNameMappings();
        if(mappings == null || mappings.isEmpty()) {
            log.warn("No Machine name mapping found, please verify your factory configuration file");
            return;
        }
        for(FactoryScadaConfiguration.MachineNameMapping mapping : factoryScadaConfigurationProvider.getFactoryScadaConfiguration().machineNameMappings()){
            logicalNameToMachineName.put(mapping.logicalName(), mapping.machineName());
            if(!machineNameToLogicalNames.containsKey(mapping.machineName())){
                machineNameToLogicalNames.put(mapping.machineName(), new ArrayList<>());
            }
            machineNameToLogicalNames.get(mapping.machineName()).add(mapping.logicalName());
        }
    }

    public String getMachineForLogicalName(String logicalName) {
        String res = logicalNameToMachineName.get(logicalName);
        if(res == null){
            log.warn("Logical name {} not found, using logical name instead, please verify your configuration file", logicalName);
            res = logicalName;
        }
        return res;
    }

    public List<String> getLogicalNamesForMachine(String machineName) {
        List<String> res = machineNameToLogicalNames.get(machineName);
        if(res == null){
            log.warn("Machine name {} not found, using machine name instead, please verify your configuration file", machineName);
            res = List.of(machineName);
        }
        return res;
    }
}
