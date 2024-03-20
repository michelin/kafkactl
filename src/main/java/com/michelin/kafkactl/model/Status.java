package com.michelin.kafkactl.model;

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
        Success,
        Failed
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
