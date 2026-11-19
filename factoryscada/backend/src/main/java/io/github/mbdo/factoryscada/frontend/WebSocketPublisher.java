package io.github.mbdo.factoryscada.frontend;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import io.github.mbdo.factoryscada.domain.CommandStatus;
import io.github.mbdo.factoryscada.domain.MachineStatus;
import io.github.mbdo.factoryscada.domains.mission.dsl.dtos.Node_dto;
import io.github.mbdo.factoryscada.frontend.dto.CommandStatusDTO;
import io.github.mbdo.factoryscada.frontend.dto.MachineStatusDTO;
import io.github.mbdo.factoryscada.frontend.dto.MissionCommandResponseDTO;
import io.github.mbdo.factoryscada.frontend.dto.MissionExtensionConfigurationDTO;
import io.github.mbdo.factoryscada.frontend.dto.MissionLogsDTO;
import io.github.mbdo.factoryscada.frontend.dto.PlcConnectionStatusDto;
import io.github.mbdo.factoryscada.frontend.mapper.CommandStatusMapper;
import io.github.mbdo.factoryscada.frontend.mapper.MachineStatusMapper;
import io.github.mbdo.factoryscada.service.FactoryScada;
import lombok.extern.slf4j.Slf4j;

/**
 * Class gathering all manually sent websocket messages
 * it helps generating the protocol documentation with WebsocketDocGenerator by
 * avoiding direct call to convertAndSend
 */
@Slf4j
@Component
public class WebSocketPublisher {

    @Autowired
    private SimpMessagingTemplate template;

    public FactoryScada factoryscada;

    @WebSocketPublish(value = "/topic/controller-feedbacks", payload = String.class, description = "Broadcasts raw feedback from the controller")
    public void sendControllerFeedback(String message) {
        template.convertAndSend("/topic/controller-feedbacks", message);
    }

    @WebSocketPublish(value = "/topic/frontend-logs", payload = String.class, description = "All logs currenly archived by the backend, including command events, machine status events and mission events")
    public void sendFrontendLogs(String message) {
        template.convertAndSend("/topic/frontend-logs", message);
    }

    @WebSocketPublish(value = "/topic/{controllerName}/plc-connection-status", payload = PlcConnectionStatusDto.class, description = "Broadcasts PLC connection status updates")
    public void sendPlcStatus(String controllerName, boolean send, boolean recv) {
        PlcConnectionStatusDto dto = new PlcConnectionStatusDto(controllerName, send, recv);
        template.convertAndSend("/topic/" + controllerName + "/plc-connection-status", dto);
    }

    @WebSocketPublish(value = "/topic/{machineName}/command-status", payload = CommandStatusDTO.class, description = "Broadcasts Command status updates")
    public void sendCommandStatus(String machineName, CommandStatus commandStatus) {
        template.convertAndSend("/topic/" + machineName + "/command-status",
                CommandStatusMapper.INSTANCE.commandStatusToCommandStatusDTO(commandStatus));
    }

    @WebSocketPublish(value = "/topic/{machineName}/machine-status", payload = MachineStatusDTO.class, description = "Broadcasts Machine status updates")
    public void sendMachineStatus(String machineName, MachineStatus machineStatus) {
        template.convertAndSend("/topic/" + machineName + "/machine-status",
                MachineStatusMapper.INSTANCE.machineStatusToMachineStatusDTO(machineStatus));

    }

    @WebSocketPublish(value = "/topic/actual-command-executing", payload = Node_dto[].class, description = "Broadcasts the list of mission nodes currently executed by the mission DSL executer")
    public void sendActualCommandExecuting(List<Node_dto> currentlyExecutingNodes) {
        template.convertAndSend("/topic/actual-command-executing", currentlyExecutingNodes);
    }

    @WebSocketPublish(value = "/topic/mission-extension/configuration", payload = MissionExtensionConfigurationDTO.class, description = "Broadcasts the mission extension configuration (available and active missions, recent mission changes)")
    public void sendMissionExtensionConfiguration(MissionExtensionConfigurationDTO configuration) {
        template.convertAndSend("/topic/mission-extension/configuration", configuration);
    }

    @WebSocketPublish(value = "/topic/mission-extension/logs", payload = MissionLogsDTO.class, description = "Broadcasts the logs of the machine missions")
    public void sendMissionExtensionLogs(MissionLogsDTO logs) {
        template.convertAndSend("/topic/mission-extension/logs", logs);
    }

    @WebSocketPublish(value = "/topic/mission-extension/command-response", payload = MissionCommandResponseDTO.class, description = "Result of a mission extension command (start, stop, override...)")
    public void sendMissionExtensionCommandResponse(MissionCommandResponseDTO response) {
        template.convertAndSend("/topic/mission-extension/command-response", response);
    }

}
