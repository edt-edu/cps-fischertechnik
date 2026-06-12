package io.github.mbdo.factoryscada.frontend.controller;

import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.conveyorbelt.ConveyorBeltMachine;
import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryScadaConfiguration;
import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryScadaInstance;
import io.github.mbdo.factoryscada.domains.mission.dsl.dtos.FactoryMissionsParallelized_dto;
import io.github.mbdo.factoryscada.domains.mission.dsl.dtos.Node_dto;
import io.github.mbdo.factoryscada.service.FactoryScada;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;


@Slf4j
@Controller
@MessageMapping("/factoryMission")
public class MissionOrchestratorController {

    private final BeanFactory beanFactory;

    protected FactoryScada factoryScada;
    protected FactoryScadaInstance factoryScadaInstance;
    protected FactoryScadaConfiguration factoryScadaConfiguration;
    
    @Autowired
    public MissionOrchestratorController(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @PostConstruct
    private void init() {
        this.factoryScada = beanFactory.getBean(FactoryScada.class);
        this.factoryScadaInstance = factoryScada.getFactoryScadaInstance();
        this.factoryScadaConfiguration = factoryScada.getFactoryScadaConfiguration();
    }

    @MessageMapping("/mission-configuration")
    @SendTo("/topic/mission-configuration")
    public FactoryMissionsParallelized_dto getFactoryMissionsConfiguration() {
        log.info("Received WS request on /factoryMission/mission-configuration");
        return factoryScada.getMissionsParallelized_dto();
    }

    @MessageMapping("/actual-command-executing")
    @SendTo("/topic/actual-command-executing")
    public List<Node_dto> getFactoryActualMissions() {
        log.info("Received WS request on /factoryMission/actual-command-executing");
        return factoryScada.getExecuterVisitor().getIsCurrentlyVisiting();
    }

    @MessageMapping("/command/start/{missionName}")
    public String startMission(
        @DestinationVariable("missionName") String missionName
            //@Valid @Payload MissionCommandDTO missionCommandDTO
    ) {
        String decodedMissionName = UriUtils.decode(missionName, StandardCharsets.UTF_8);
        log.info("Received WS request on /factoryMission/command/start/"+missionName);
        return factoryScada.getExecuterVisitor().startMission(decodedMissionName);
    }

    @MessageMapping("/command/stop")
    public String stopMission(
            //@Valid @Payload MissionCommandDTO missionCommandDTO
    ) {
        log.info("Received WS request on /factoryMission/command/stop/");
        return factoryScada.getExecuterVisitor().stopMission();
    }
    
    @MessageMapping("/status")
    @SendTo("/topic/mission-status")
    public String getMissionStatus(
    ) {
        // TODO implement me
        return "";
        //return executeMissionCommand(machineName, "moveNbSteps", moveStepsDTO);
    }

    @MessageMapping("/list")
    @SendTo("/topic/mission-configuration")
    public FactoryMissionsParallelized_dto getMissionList() {
        log.debug("Received WS request on /factoryMission/list/");
        return factoryScada.getExecuterVisitor().getFactoryMissions();
    }



    @MessageMapping("**")
    public void handleUnmappedMessage(
    		@Header("simpDestination") String destination,
    		@Payload String messageContent
    		) {
    	log.error("Unmapped message destination={} payload={}", destination, messageContent);
    }
}
