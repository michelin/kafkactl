package com.michelin.kafkactl.model;

import io.micronaut.core.annotation.ReflectiveAccess;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Jwt content.
 */
@Data
@Builder
@ReflectiveAccess
@NoArgsConstructor
@AllArgsConstructor
public class JwtContent {
    private String sub;
    private Long exp;
    @Builder.Default
    private List<String> roles = new ArrayList<>();
    @Builder.Default
    private List<RoleBinding> roleBindings = new ArrayList<>();

    /**
     * Role binding.
     */
    @Data
    @Builder
    @ReflectiveAccess
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleBinding {
        private String namespace;
        private List<Verb> verbs;
        private List<String> resources;

        /**
         * Verb.
         */
        public enum Verb {
            GET,
            POST,
            PUT,
            DELETE
        }
    }

}
