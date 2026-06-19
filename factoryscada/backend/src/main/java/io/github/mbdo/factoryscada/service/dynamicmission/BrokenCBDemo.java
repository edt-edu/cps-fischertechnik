package io.github.mbdo.factoryscada.service.dynamicmission;

import io.github.mbdo.factoryscada.mqtt.MqttPublisherService;
import io.github.mbdo.factoryscada.service.FactoryScada;
import io.github.mbdo.factoryscada.service.MachineNameMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BrokenCBDemo extends Demo {

  @Autowired
  public BrokenCBDemo(FactoryScada factoryScada, MachineNameMappingService machineNameMapping, MqttPublisherService mqttPublisher) {
    super(factoryScada, machineNameMapping, mqttPublisher);
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
  public void start() {
    setCbBroken(true);
    super.start();
  }
}
