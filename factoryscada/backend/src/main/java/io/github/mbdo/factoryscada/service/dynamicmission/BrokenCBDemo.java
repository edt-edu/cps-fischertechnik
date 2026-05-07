package io.github.mbdo.factoryscada.service.dynamicmission;

import io.github.mbdo.factoryscada.domains.dynamicmission.DynamicMission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@Slf4j
public class BrokenCBDemo implements DynamicMission {
  private final Demo demo;

  @Autowired
  public BrokenCBDemo(Demo demo) {
    this.demo = demo;
  }

  @Override
  public String getName() {
    return "Broken CB Demo";
  }

  @Override
  public String getDescription() {
    return "Like the demo, but the conveyor belt cannot move tokens, thus the VGRs need to do that";
  }

  @Override
  public Collection<String> getInvolvedMachineNames() {
    return demo.getInvolvedMachineNames();
  }

  @Override
  public void start() {
    demo.setCbBroken(true);
    demo.start();
  }

  @Override
  public void stop() {
    demo.stop();
    demo.setCbBroken(false);
  }

  @Override
  public boolean isActive() {
    return demo.isActive();
  }
}
