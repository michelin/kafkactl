package com.michelin.kafkactl.model.format;

import static io.micronaut.core.util.StringUtils.EMPTY_STRING;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import java.text.ParseException;
import java.util.Date;
import lombok.AllArgsConstructor;
import org.ocpsoft.prettytime.PrettyTime;

/**
 * Ago format.
 */
@AllArgsConstructor
public class AgoFormat implements OutputFormatStrategy {
    private String jsonPointer;

    @Override
    public String display(JsonNode node) {
        String output;
        JsonNode cell = node.at(this.jsonPointer);

        try {
            StdDateFormat sdf = new StdDateFormat();
            Date d = sdf.parse(cell.asText());
            output = new PrettyTime().format(d);
        } catch (ParseException e) {
            output = EMPTY_STRING;
        }

        return output;
    }
}
