package io.github.mbdo.factoryscada.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mbdo.factoryscada.domain.CommandStatus;
import io.github.mbdo.factoryscada.frontend.mapper.CommandStatusMapper;
import io.github.mbdo.factoryscada.service.FactoryScada;
import io.github.mbdo.factoryscada.socket.Protocol;
import io.github.mbdo.factoryscada.socket.exception.ProtocolException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Data
public abstract class AbstractCommand<T extends AbstractMachine> implements Command {

    protected final T machine;
    protected final GenericMachineCommandDTO<T> abstractDTO;

    /**
     * Constructor used when the CommandDTO is already available
     * @param machine
     * @param abstractDTO
     */
    public AbstractCommand(T machine, GenericMachineCommandDTO<T> abstractDTO) {
        this.machine = machine;
        this.abstractDTO = abstractDTO;
    }
    
    /**
     * Constructor used when the CommandDTO need to be build
     * Requires to implement GenericMachineCommandDTO<T> buildDTO()
     * @param machine
     */
    public AbstractCommand(T machine) {
        this.machine = machine;
        this.abstractDTO = buildDTO();
    }

    @Override
    public void execute() {
        ObjectMapper mapper = new ObjectMapper();
        Protocol protocol = this.machine.getProtocol();
        try {
            String rawJson = mapper.writeValueAsString(abstractDTO);
            protocol.send(rawJson);
            log.debug("Updating idle state for machine {} to false", machine.getName());
            machine.setIdle(false);

            // updating scada log and report to front
            CommandStatus status = machine.factoryScada.getMachineLastCommandStatusMap().getOrDefault(machine.getName(), new CommandStatus());
            status.setCurrentCommandRawJSON(rawJson);
            machine.factoryScada.getMachineLastCommandStatusMap().put(machine.getName(), status);
            // publish to frontend
            machine.factoryScada.getTemplate().convertAndSend("/topic/"+machine.getName()+"/command-status",
                    CommandStatusMapper.INSTANCE.commandStatusToCommandStatusDTO(status));
            machine.factoryScada.addLogsForFrontend(rawJson);

            log.info("Command sent to controller");
        } catch (JsonProcessingException e) {
            log.error("Cannot send command to controller; Conversion error {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (ProtocolException e) {
            log.error("Communication error with controller {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    abstract public GenericMachineCommandDTO<T> buildDTO();
}
