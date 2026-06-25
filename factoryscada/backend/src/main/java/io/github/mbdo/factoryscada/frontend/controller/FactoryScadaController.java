package io.github.mbdo.factoryscada.frontend.controller;

import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryScadaConfiguration;
import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryScadaInstance;
import io.github.mbdo.factoryscada.service.FactoryScada;
import io.github.mbdo.factoryscada.service.FactoryScadaConfigurationProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@MessageMapping("/factory")
public class FactoryScadaController {

    private final BeanFactory beanFactory;

    protected FactoryScadaInstance factoryScadaInstance;
    protected FactoryScadaConfigurationProvider factoryScadaConfigurationProvider;
    protected Map<String, Map<String, String>> commandPlaceholder;

    @Autowired
    public FactoryScadaController(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @PostConstruct
    private void init() {
        FactoryScada factoryScada = beanFactory.getBean(FactoryScada.class);
        this.factoryScadaInstance = factoryScada.getFactoryScadaInstance();
        this.factoryScadaConfigurationProvider = beanFactory.getBean(FactoryScadaConfigurationProvider.class);
        this.commandPlaceholder = factoryScada.getCommandPlaceholder();
    }

    @MessageMapping("/placeholder")
    @SendTo("/topic/command-placeholder")
    public Map<String, Map<String, String>> getCommandPlaceholder() {
        log.info("Received WS request on /factory/placeholder");
        return commandPlaceholder;
    }

    @MessageMapping("/configuration")
    @SendTo("/topic/factory-configuration")
    public FactoryScadaConfiguration getFactoryConfiguration() {
        log.info("Received WS request on /factory/configuration");
        return factoryScadaConfigurationProvider.getFactoryScadaConfiguration();
    }

    @MessageMapping("/instance")
    @SendTo("/topic/factory-instance")
    public FactoryScadaInstance getFactoryInstance() {
        log.info("Received WS request on /factory/factory/instance");
        return factoryScadaInstance;
    }

    @MessageMapping("**")
    public void handleUnmappedMessage(
    		@Header("simpDestination") String destination,
    		@Payload String messageContent
    		) {
    	log.error("Unmapped message destination={} payload={}", destination, messageContent);
    }
}
