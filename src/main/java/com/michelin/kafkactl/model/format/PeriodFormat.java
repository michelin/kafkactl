package com.michelin.kafkactl.model.format;

import static io.micronaut.core.util.StringUtils.EMPTY_STRING;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;

/**
 * Period format.
 */
@AllArgsConstructor
public class PeriodFormat implements OutputFormatStrategy {
    private String jsonPointer;

    @Override
    public String display(JsonNode node) {
        String output;
        JsonNode cell = node.at(this.jsonPointer);

        try {
            long ms = Long.parseLong(cell.asText());
            long days = TimeUnit.MILLISECONDS.toDays(ms);
            long hours = TimeUnit.MILLISECONDS.toHours(ms - TimeUnit.DAYS.toMillis(days));
            long minutes = TimeUnit.MILLISECONDS.toMinutes(
                ms - TimeUnit.DAYS.toMillis(days) - TimeUnit.HOURS.toMillis(hours));
            output = days > 0 ? (days + "d") : "";
            output += hours > 0 ? (hours + "h") : "";
            output += minutes > 0 ? (minutes + "m") : "";
        } catch (NumberFormatException e) {
            output = EMPTY_STRING;
        }

        return output;
    }
}
