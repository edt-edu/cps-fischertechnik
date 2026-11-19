package io.github.mbdo.factoryscada.frontend.controller.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import io.github.mbdo.factoryscada.frontend.WebSocketDoc;
import io.github.mbdo.factoryscada.frontend.WebSocketPublisher;
import io.github.mbdo.factoryscada.service.FactoryScada;

/**
 * Controller used to load the frontend logs on scada pages.
 * The logs are sent through {@link WebSocketPublisher} so the outgoing topic is documented by the WebSocketDocGenerator.
 */
@Controller
public class LogsWsController {

    @Autowired
    private WebSocketPublisher webSocketPublisher;

    @Autowired
    private FactoryScada factoryscada;

    @MessageMapping("/logs/request")
    @WebSocketDoc (description = "Requests all the logs currently available on the backend. The logs include events about commands received with their answers, machine status event and erros and mission events.",
                  indirectOutputs = { "/topic/frontend-logs" })
    public void requestFrontendLogs() {
        String logs = String.join("\n", factoryscada.getFrontendLogsList().snapshot());
        webSocketPublisher.sendFrontendLogs(logs);
    }
}
