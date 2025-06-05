package io.github.mbdo.factoryscada.socket.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocketNotification implements Serializable {
    private String timestamp;
    private String topicName;
    private MessageResponse message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageResponse implements Serializable {
        private Integer commandId;
        private Integer requestId;
        private String jsonType;
        private String status;
        private String info;
    }
}
