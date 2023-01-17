package com.michelin.kafkactl;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.convert.format.MapFormat;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Introspected
@ConfigurationProperties("kafkactl")
public class KafkactlConfig {
    private String version;
    private String configPath;
    private String api;
    private String userToken;
    private String currentNamespace;
    private List<Context> contexts;

    @MapFormat(transformation = MapFormat.MapTransformation.FLAT)
    private Map<String, List<String>> tableFormat;

    @Getter
    @Setter
    @Introspected
    public static class Context {
        private String name;

        @JsonProperty("context")
        private ApiContext definition;

        @Getter
        @Setter
        @Introspected
        public static class ApiContext {
            private String api;
            private String userToken;
            private String namespace;
        }
    }
}
