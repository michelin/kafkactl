package com.michelin.kafkactl.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Output.
 */
@Getter
@AllArgsConstructor
public enum Output {
    YAML("yaml"),
    YML("yml"),
    TABLE("table");

    private final String name;

    @Override
    public String toString() {
        return name;
    }
}
