package com.michelin.kafkactl.model;

import io.micronaut.core.annotation.Introspected;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Api resource.
 */
@Getter
@Setter
@Builder
@Introspected
public class ApiResource {
    private String kind;
    private boolean namespaced;
    private boolean synchronizable;
    private String path;
    private List<String> names;
}
