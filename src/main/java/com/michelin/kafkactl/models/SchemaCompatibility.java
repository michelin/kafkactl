package com.michelin.kafkactl.models;

public enum SchemaCompatibility {
    GLOBAL,
    BACKWARD,
    BACKWARD_TRANSITIVE,
    FORWARD,
    FORWARD_TRANSITIVE,
    FULL,
    FULL_TRANSITIVE,
    NONE
}
