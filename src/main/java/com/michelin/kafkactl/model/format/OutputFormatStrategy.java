package com.michelin.kafkactl.model.format;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Output format strategy.
 */
public interface OutputFormatStrategy {
    String display(JsonNode node);
}
