package io.github.mbdo.factoryscada.frontend.controller;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryScadaConfiguration;
import io.github.mbdo.factoryscada.domains.factoryscada.dtos.FactoryScadaInstance;
import io.github.mbdo.factoryscada.frontend.dto.PlcConnectionStatusDto;
import io.github.mbdo.factoryscada.service.FactoryScada;
import io.github.mbdo.factoryscada.socket.Protocol;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@MessageMapping("/plc")
public class PlcController {

	
    private final BeanFactory beanFactory;

    protected FactoryScada factoryScada;
    protected FactoryScadaInstance factoryScadaInstance;
    protected FactoryScadaConfiguration factoryScadaConfiguration;
    
    @Autowired
    public PlcController(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @PostConstruct
    private void init() {
        this.factoryScada = beanFactory.getBean(FactoryScada.class);
        this.factoryScadaInstance = factoryScada.getFactoryScadaInstance();
        this.factoryScadaConfiguration = factoryScada.getFactoryScadaConfiguration();
    }
    
    @MessageMapping("/{plcName}/plc-connection")
    //@SendTo("/topic/{plcName}/plc-connection-status")
    public void getPlcConnectionStatus(
        	@DestinationVariable("plcName") String plcName) {
        log.info("Received request on /plc/"+plcName+"/plc-connection");
        Protocol controller = factoryScadaInstance.controllers().get(plcName);
        PlcConnectionStatusDto topicDto;
        if (controller != null) {
        	topicDto =  new PlcConnectionStatusDto(plcName,
        			controller.isSendChannelConnected(),
        			controller.isReceivedChannelConnected()
        			);
        } else {
        	topicDto = new PlcConnectionStatusDto();
        }
        // send message to front end
        this.factoryScada.getTemplate().convertAndSend("/topic/"+plcName+"/plc-connection-status", topicDto);
    }
    
    @MessageMapping("**")
    public void handleUnmappedMessage(
    		@Header("simpDestination") String destination,
    		@Payload String messageContent
    		) {
    	log.error("Unmapped message destination={} payload={}", destination, messageContent);
    }
}
