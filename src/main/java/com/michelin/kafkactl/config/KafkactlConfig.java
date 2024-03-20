package com.michelin.kafkactl.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.michelin.kafkactl.service.SystemService;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.convert.format.MapFormat;
import io.micronaut.core.util.StringUtils;
import java.io.File;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Kafkactl config class.
 */
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

    /**
     * Get the current user config directory.
     *
     * @return The config directory
     */
    public String getConfigDirectory() {
        if (StringUtils.isNotEmpty(SystemService.getEnv(KAFKACTL_CONFIG))) {
            String parent = new File(SystemService.getEnv(KAFKACTL_CONFIG)).getParent();
            return parent != null ? parent : ".";
        }

        return SystemService.getProperty("user.home") + "/.kafkactl";
    }

    /**
     * Get the current user config full path.
     *
     * @return The config path
     */
    public String getConfigPath() {
        return StringUtils.isNotEmpty(SystemService.getEnv(KAFKACTL_CONFIG))
            ? SystemService.getEnv(KAFKACTL_CONFIG) : SystemService.getProperty("user.home")
            + "/.kafkactl/config.yml";
    }

    /**
     * Context class.
     */
    @Getter
    @Setter
    @Builder
    @Introspected
    public static class Context {
        private String name;

        @JsonProperty("context")
        private ApiContext definition;

        /**
         * ApiContext class.
         */
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
}
