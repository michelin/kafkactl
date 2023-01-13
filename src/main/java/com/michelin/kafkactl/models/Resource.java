package com.michelin.kafkactl.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.*;

import java.util.Map;

@Data
@Getter
@Builder
@ReflectiveAccess
@NoArgsConstructor
@AllArgsConstructor
public class Resource {
    private String apiVersion;
    private String kind;
    private ObjectMeta metadata;
    @JsonInclude(value = JsonInclude.Include.NON_ABSENT)
    private Map<String,Object> spec;
    private Object status;
}
