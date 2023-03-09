package com.michelin.kafkactl;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.convert.format.MapFormat;
import io.micronaut.core.util.StringUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Introspected
@ConfigurationProperties("kafkactl")
public class KafkactlConfig {
    public static final String KAFKACTL_CONFIG = "KAFKACTL_CONFIG";
    private String version;
    private String api;
    private String userToken;
    private String currentNamespace;
    private List<Context> contexts;

    @MapFormat(transformation = MapFormat.MapTransformation.FLAT)
    private Map<String, List<String>> tableFormat;

    @Getter
    @Setter
    @Builder
    @Introspected
    public static class Context {
        private String name;

        @JsonProperty("context")
        private ApiContext definition;

        @Getter
        @Setter
        @Builder
        @Introspected
        public static class ApiContext {
            private String api;
            private String userToken;
            private String namespace;
        }
    }

    /**
     * Get the current user config directory
     * @return The config directory
     */
    public String getConfigDirectory() {
        if (StringUtils.isNotEmpty(System.getenv(KAFKACTL_CONFIG))) {
            String parent = new File(System.getenv(KAFKACTL_CONFIG)).getParent();
            return parent != null ? parent : ".";
        }

        return System.getenv("user.home") + "/.kafkactl";
    }

    /**
     * Get the current user config full path
     * @return The config path
     */
    public String getConfigPath() {
        return StringUtils.isNotEmpty(System.getenv(KAFKACTL_CONFIG)) ?
                System.getenv(KAFKACTL_CONFIG) : System.getenv("user.home") + "/.kafkactl/config.yml";
    }
}
