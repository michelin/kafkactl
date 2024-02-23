package com.michelin.kafkactl.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.ReflectiveAccess;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resource.
 */
@Data
@Builder
@ReflectiveAccess
@NoArgsConstructor
@AllArgsConstructor
public class Resource {
    private String apiVersion;
    private String kind;
    private Metadata metadata;
    @JsonInclude(value = JsonInclude.Include.NON_ABSENT)
    private Map<String, Object> spec;
    private Object status;
}
