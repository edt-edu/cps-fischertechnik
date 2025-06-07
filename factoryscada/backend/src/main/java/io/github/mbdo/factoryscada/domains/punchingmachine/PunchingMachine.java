package io.github.mbdo.factoryscada.domains.punchingmachine;

import java.util.List;

import io.github.mbdo.factoryscada.core.AbstractMachine;
import io.github.mbdo.factoryscada.core.GenericMachineCommandDTO;
import io.github.mbdo.factoryscada.domains.punchingmachine.command.PunchCommand;
import io.github.mbdo.factoryscada.domains.punchingmachine.command.StopCommand;
import io.github.mbdo.factoryscada.socket.Protocol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PunchingMachine extends AbstractMachine {
  public PunchingMachine(String name, Protocol protocol, List<String> rawCommandNames) {
    super(name, protocol, rawCommandNames);
  }

  public static String getType() {
    return "punchingMachine";
  }

  public void stop(@Valid @NotNull final GenericMachineCommandDTO<PunchingMachine> stopDTO) {
    log.info("Stop punching machine {}", stopDTO);
    new StopCommand(this, stopDTO).execute();
  }

  public void punch(@Valid @NotNull final GenericMachineCommandDTO<PunchingMachine> punchDTO) {
    log.info("Punching with punching machine {}", punchDTO);
    new PunchCommand(this, punchDTO).execute();
  }
}
