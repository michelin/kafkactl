package com.michelin.kafkactl.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.micronaut.core.annotation.ReflectiveAccess;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Status.
 */
@Data
@Builder
@ReflectiveAccess
@NoArgsConstructor
@AllArgsConstructor
public class Status {
    @Builder.Default
    private String apiVersion = "v1";
    @Builder.Default
    private String kind = "Status";
    private StatusPhase status;
    private String message;
    private String reason;
    private StatusDetails details;
    private int code;

    /**
     * Status phase.
     */
    public enum StatusPhase {
        SUCCESS,
        FAILED;

        /**
         * Build status phase from string.
         * This is because Ns4Kafka returns capitalised status phases.
         *
         * @param key the key
         * @return the status phase
         */
        @JsonCreator
        public static StatusPhase fromString(String key) {
            for (StatusPhase type : StatusPhase.values()) {
                if (type.name().equalsIgnoreCase(key)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * Status details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusDetails {
        private String name;
        private String kind;
        private List<String> causes;
    }
}
