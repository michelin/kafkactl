package com.michelin.kafkactl.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@ReflectiveAccess
@NoArgsConstructor
@AllArgsConstructor
public class Status {
    private String apiVersion = "v1";
    private String kind = "Status";
    private StatusPhase status;
    private String message;
    private String reason;
    private StatusDetails details;
    private int code;

    @Data
    @Builder
    @NoArgsConstructor
    public static class StatusDetails {
        private String name;
        private String kind;
        private List<String> causes;

        @JsonCreator
        public StatusDetails(@JsonProperty("name") String name, @JsonProperty("kind") String kind, @JsonProperty("causes") List<String> causes) {
            this.name = name;
            this.kind = kind;
            this.causes = causes;
        }
    }

    public enum StatusPhase {
        Success,
        Failed
    }
}
