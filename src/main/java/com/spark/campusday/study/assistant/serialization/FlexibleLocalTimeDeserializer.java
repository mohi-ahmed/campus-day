package com.spark.campusday.study.assistant.serialization;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Accepts the ISO local-time values used by Campus Day as well as the equivalent
 * UTC/offset time values that Gemini may emit for structured output.
 */
public final class FlexibleLocalTimeDeserializer extends StdDeserializer<LocalTime> {

    public FlexibleLocalTimeDeserializer() {
        super(LocalTime.class);
    }

    @Override
    public LocalTime deserialize(JsonParser parser, DeserializationContext context)
            throws JacksonException {
        String value = parser.getValueAsString();
        if (value == null) {
            return (LocalTime) context.handleUnexpectedToken(LocalTime.class, parser);
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        try {
            if (hasOffset(normalized)) {
                return OffsetTime.parse(normalized, DateTimeFormatter.ISO_OFFSET_TIME)
                        .toLocalTime();
            }
            return LocalTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_TIME);
        } catch (DateTimeParseException exception) {
            return (LocalTime) context.handleWeirdStringValue(
                    LocalTime.class,
                    value,
                    "Expected an ISO time such as HH:mm, HH:mm:ss, or HH:mm:ssZ");
        }
    }

    private boolean hasOffset(String value) {
        return value.endsWith("Z")
                || value.lastIndexOf('+') > 0
                || value.lastIndexOf('-') > 0;
    }
}
