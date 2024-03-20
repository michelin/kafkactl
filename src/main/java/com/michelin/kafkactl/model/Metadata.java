package com.michelin.kafkactl.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.micronaut.core.annotation.ReflectiveAccess;
import java.util.Date;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Object metadata.
 */
@Getter
@Setter
@Builder
@ToString
@ReflectiveAccess
@NoArgsConstructor
@AllArgsConstructor
public class Metadata {
    private String name;
    private String namespace;
    private String cluster;
    private Map<String, String> labels;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Date creationTimestamp;
}
