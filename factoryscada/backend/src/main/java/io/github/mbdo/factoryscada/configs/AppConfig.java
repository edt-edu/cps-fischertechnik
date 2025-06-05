package io.github.mbdo.factoryscada.configs;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_FLOAT_AS_INT;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

@Configuration
public class AppConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder
                .featuresToEnable()
                .featuresToDisable(
                        ACCEPT_FLOAT_AS_INT,
                        FAIL_ON_EMPTY_BEANS,
                        FAIL_ON_UNKNOWN_PROPERTIES,
                        WRITE_DATES_AS_TIMESTAMPS
                );
    }

}
