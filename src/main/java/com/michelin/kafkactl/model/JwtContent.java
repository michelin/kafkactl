package com.michelin.kafkactl.model;

import io.micronaut.core.annotation.ReflectiveAccess;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Jwt content.
 */
@Data
@ReflectiveAccess
@NoArgsConstructor
public class JwtContent {
    private List<RoleBinding> roleBindings = new ArrayList<>();

    /**
     * Role binding.
     */
    @Data
    @ReflectiveAccess
    @NoArgsConstructor
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
