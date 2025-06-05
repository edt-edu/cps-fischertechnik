package io.github.mbdo.factoryscada.socket.dtos;

import io.github.mbdo.factoryscada.core.enums.PassableType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocketCommand implements Serializable {
    private String timestamp;
    private String topicName;
    private Message message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message implements Serializable {
        private String outputId;
        private String jsonType;
        private String type;
        private String name;
        private List<Parameter> parameters;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameter implements Serializable {
        private PassableType passableType;
        private Object passable;
    }
}
