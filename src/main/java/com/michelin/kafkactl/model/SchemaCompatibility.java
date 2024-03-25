package com.michelin.kafkactl.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Schema compatibility.
 */
@Getter
@AllArgsConstructor
public enum SchemaCompatibility {
    GLOBAL("global"),
    BACKWARD("backward"),
    BACKWARD_TRANSITIVE("backward-transitive"),
    FORWARD("forward"),
    FORWARD_TRANSITIVE("forward-transitive"),
    FULL("full"),
    FULL_TRANSITIVE("full-transitive"),
    NONE("none");

    private final String name;

    @Override
    public String toString() {
        return name;
    }
}
