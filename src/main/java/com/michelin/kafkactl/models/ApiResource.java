package com.michelin.kafkactl.models;

import io.micronaut.core.annotation.Introspected;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Introspected
public class ApiResource {
    private String kind;
    private boolean namespaced;
    private boolean synchronizable;
    private String path;
    private List<String> names;
}
