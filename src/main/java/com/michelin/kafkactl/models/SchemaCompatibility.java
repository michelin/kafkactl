package com.michelin.kafkactl.models;

import lombok.Getter;

public enum SchemaCompatibility {
    GLOBAL("global"),
    BACKWARD("backward"),
    BACKWARD_TRANSITIVE("backward-transitive"),
    FORWARD("forward"),
    FORWARD_TRANSITIVE("forward-transitive"),
    FULL("full"),
    FULL_TRANSITIVE("full-transitive"),
    NONE("none");

    @Getter
    private final String name;

    SchemaCompatibility(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
