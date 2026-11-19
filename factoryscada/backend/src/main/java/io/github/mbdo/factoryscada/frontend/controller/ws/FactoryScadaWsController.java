package io.github.mbdo.factoryscada.frontend.controller.ws;

import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryScadaConfiguration;
import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryScadaInstance;
import io.github.mbdo.factoryscada.frontend.WebSocketDoc;
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
public class FactoryScadaWsController {

    private final BeanFactory beanFactory;

    protected FactoryScadaInstance factoryScadaInstance;
    protected FactoryScadaConfigurationProvider factoryScadaConfigurationProvider;
    protected Map<String, Map<String, String>> commandPlaceholder;

    @Autowired
    public FactoryScadaWsController(BeanFactory beanFactory) {
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
    @WebSocketDoc(description = "Returns a map of command placeholders for all machines, which can be used to fill in the command templates.",
                responseDescription = "A map of command placeholders for all machines.")
    public Map<String, Map<String, String>> getCommandPlaceholder() {
        log.info("Received WS request on /factory/placeholder");
        return commandPlaceholder;
    }

    @MessageMapping("/configuration")
    @SendTo("/topic/factory-configuration")
    @WebSocketDoc(description = "Returns the configuration for the factory SCADA system.",
                  responseDescription = "The factory SCADA configuration. Including the list of controllers and machines, their names,  the protocols used to communicate with them, and machine logical/physical name mappings.")
    public FactoryScadaConfiguration getFactoryConfiguration() {
        log.info("Received WS request on /factory/configuration");
        return factoryScadaConfigurationProvider.getFactoryScadaConfiguration();
    }

    @MessageMapping("/instance")
    @SendTo("/topic/factory-instance")
    @WebSocketDoc(description = "Returns the instance of the factory SCADA system.",
                  responseDescription = "The factory SCADA instance, including status of the controllers and machines. Machines also provide their declaredcommand placeholders.")
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
