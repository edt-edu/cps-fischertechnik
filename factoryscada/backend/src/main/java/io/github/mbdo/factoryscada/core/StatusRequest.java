package io.github.mbdo.factoryscada.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mbdo.factoryscada.socket.Protocol;
import io.github.mbdo.factoryscada.socket.exception.ProtocolException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public abstract class StatusRequest<T extends AbstractMachine> implements Command {

    protected final T machine;
    protected final GenericMachineStatusRequestDTO<T> abstractDTO;

    /**
     * Constructor used when the CommandDTO is already available
     * @param machine
     * @param abstractDTO
     */
    public StatusRequest(T machine, GenericMachineStatusRequestDTO<T> abstractDTO) {
        this.machine = machine;
        this.abstractDTO = abstractDTO;
    }
    
    /**
     * Constructor used when the CommandDTO need to be build
     * Requires to implement GenericMachineCommandDTO<T> buildDTO()
     * @param machine
     */
    public StatusRequest(T machine) {
        this.machine = machine;
        this.abstractDTO = buildDTO();
    }

    @Override
    public void execute() {
        ObjectMapper mapper = new ObjectMapper();
        Protocol protocol = this.machine.getProtocol();
        try {
            protocol.send(mapper.writeValueAsString(abstractDTO));
            log.info("Request sent to controller");
        } catch (JsonProcessingException e) {
            log.error("Cannot send Request to controller; Conversion error {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (ProtocolException e) {
            log.error("Communication error with controller {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    abstract public GenericMachineStatusRequestDTO<T> buildDTO();
}
