package com.michelin.kafkactl.model.format;

import static io.micronaut.core.util.StringUtils.EMPTY_STRING;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;

/**
 * Default format.
 */
@AllArgsConstructor
public class DefaultFormat implements OutputFormatStrategy {
    private String jsonPointer;

    @Override
    public String display(JsonNode node) {
        String output;
        JsonNode cell = node.at(this.jsonPointer);

        if (cell.isArray()) {
            List<String> children = new ArrayList<>();
            cell.elements().forEachRemaining(jsonNode -> children.add(jsonNode.asText()));
            output = String.join(",", children);
        } else {
            output = cell.getNodeType().equals(JsonNodeType.NULL) ? EMPTY_STRING : cell.asText();
        }

        return output;
    }
}
