package com.ampznetwork.herobrine.component.core;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class JacksonProvider {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper(JsonFactory.builder()
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                .build()
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .enable(JsonParser.Feature.ALLOW_YAML_COMMENTS)
                .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
                .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
                .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)).registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
    }
}
