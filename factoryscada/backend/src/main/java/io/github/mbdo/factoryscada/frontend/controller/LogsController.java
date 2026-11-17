package io.github.mbdo.factoryscada.frontend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import io.github.mbdo.factoryscada.frontend.WebSocketPublisher;
import io.github.mbdo.factoryscada.service.FactoryScada;

/**
 * Controller used to load the frontend logs on scada pages.
 * The logs are sent through {@link WebSocketPublisher} so the outgoing topic is documented by the WebSocketDocGenerator.
 */
@Controller
public class LogsController {

    @Autowired
    private WebSocketPublisher webSocketPublisher;

    @Autowired
    private FactoryScada factoryscada;

    @MessageMapping("/logs/request")
    public void requestFrontendLogs() {
        String logs = String.join("\n", factoryscada.getFrontendLogsList().snapshot());
        webSocketPublisher.sendFrontendLogs(logs);
    }
}
