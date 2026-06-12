package io.github.mbdo.factoryscada.core;

import java.util.function.BiConsumer;

public interface MqttMessageRouter {
    void subscribe(String topicFilter, BiConsumer<String, String> callback);
}
