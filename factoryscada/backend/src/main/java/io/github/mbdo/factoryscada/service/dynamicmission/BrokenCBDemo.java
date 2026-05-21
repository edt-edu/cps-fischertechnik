package io.github.mbdo.factoryscada.service.dynamicmission;

import io.github.mbdo.factoryscada.service.FactoryScada;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BrokenCBDemo extends Demo {

  @Autowired
  public BrokenCBDemo(FactoryScada factoryScada) {
    super(factoryScada);
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
