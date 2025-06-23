package io.github.mbdo.factoryscada.frontend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import io.github.mbdo.factoryscada.domain.CommandStatus;
import io.github.mbdo.factoryscada.domain.MachineStatus;
import io.github.mbdo.factoryscada.frontend.dto.CommandStatusDTO;
import io.github.mbdo.factoryscada.frontend.dto.PlcConnectionStatusDto;
import io.github.mbdo.factoryscada.frontend.mapper.CommandStatusMapper;
import io.github.mbdo.factoryscada.frontend.mapper.MachineStatusMapper;

/**
 * Class gathering all manually sent websocket messages
 * it helps generating the protocol documentation with WebsocketDocGenerator by avoiding direct call to convertAndSend
 */
@Component
public class WebSocketPublisher {

    @Autowired
    private SimpMessagingTemplate template;

    @WebSocketPublish(
        value = "/topic/controller-feedbacks",
        payload = String.class,
        description = "Broadcasts raw feedback from the controller"
    )
    public void sendControllerFeedback(String message) {
        template.convertAndSend("/topic/controller-feedbacks", message);
    }

    @WebSocketPublish(
        value = "/topic/{controllerName}/plc-connection-status",
        payload = PlcConnectionStatusDto.class,
        description = "Broadcasts PLC connection status updates"
    )
    public void sendPlcStatus(String controllerName, boolean send, boolean recv) {
        PlcConnectionStatusDto dto = new PlcConnectionStatusDto(controllerName, send, recv);
        template.convertAndSend("/topic/" + controllerName + "/plc-connection-status", dto);
    }

    @WebSocketPublish(
        value = "/topic/{machineName}/command-status",
        payload = CommandStatusDTO.class,
        description = "Broadcasts Command status updates"
    )
    public void sendCommandStatus(String machineName, CommandStatus commandStatus) {
        template.convertAndSend("/topic/"+machineName+"/command-status", 
                            CommandStatusMapper.INSTANCE.commandStatusToCommandStatusDTO(commandStatus));
    }

    @WebSocketPublish(
        value = "/topic/{machineName}/machine-status",
        payload = CommandStatusDTO.class,
        description = "Broadcasts Machine status updates"
    )
    public void sendMachineStatus(String machineName, MachineStatus machineStatus) {
                    template.convertAndSend("/topic/"+machineName+"/machine-status", 
                            MachineStatusMapper.INSTANCE.machineStatusToMachineStatusDTO(machineStatus));
                    
    }

}
