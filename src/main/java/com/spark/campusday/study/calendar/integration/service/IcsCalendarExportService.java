package com.spark.campusday.study.calendar.integration.service;

import com.spark.campusday.study.calendar.dto.CalendarEventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IcsCalendarExportService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter UTC_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private final CalendarExportEventService exportEvents;
    @Qualifier("studyClock")
    private final Clock clock;

    public byte[] export(String studentId, LocalDate from, LocalDate to) {
        List<CalendarEventResponse> events = exportEvents.events(studentId, from, to);
        StringBuilder calendar = new StringBuilder();
        line(calendar, "BEGIN:VCALENDAR");
        line(calendar, "VERSION:2.0");
        line(calendar, "PRODID:-//Campus Day//Study Calendar//EN");
        line(calendar, "CALSCALE:GREGORIAN");
        line(calendar, "METHOD:PUBLISH");
        line(calendar, "X-WR-CALNAME:Campus Day Study");
        for (CalendarEventResponse event : events) {
            appendEvent(calendar, event);
        }
        line(calendar, "END:VCALENDAR");
        return calendar.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendEvent(StringBuilder calendar, CalendarEventResponse event) {
        line(calendar, "BEGIN:VEVENT");
        line(calendar, "UID:" + uid(exportEvents.eventKey(event)));
        line(calendar, "DTSTAMP:" + UTC_DATE_TIME.format(Instant.now(clock)));
        if (event.allDay() || event.startTime() == null) {
            line(calendar, "DTSTART;VALUE=DATE:" + DATE.format(event.date()));
            line(calendar, "DTEND;VALUE=DATE:" + DATE.format(event.date().plusDays(1)));
        } else {
            LocalTime endTime = event.endTime() == null
                    ? event.startTime().plusMinutes(30) : event.endTime();
            LocalDate endDate = endTime.isAfter(event.startTime())
                    ? event.date() : event.date().plusDays(1);
            line(calendar, "DTSTART:" + utc(event.date(), event.startTime()));
            line(calendar, "DTEND:" + utc(endDate, endTime));
        }
        line(calendar, "SUMMARY:" + escape(event.title()));
        line(calendar, "DESCRIPTION:" + escape(description(event)));
        line(calendar, "CATEGORIES:" + escape(event.type().name()));
        line(calendar, "STATUS:CONFIRMED");
        line(calendar, "END:VEVENT");
    }

    private String utc(LocalDate date, LocalTime time) {
        ZonedDateTime value = ZonedDateTime.of(date, time, clock.getZone());
        return UTC_DATE_TIME.format(value.toInstant());
    }

    private String description(CalendarEventResponse event) {
        StringBuilder description = new StringBuilder("Campus Day");
        if (event.subject() != null && !event.subject().isBlank()) {
            description.append("\nSubject: ").append(event.subject());
        }
        if (event.itemType() != null && !event.itemType().isBlank()) {
            description.append("\nType: ").append(event.itemType());
        }
        if (event.description() != null && !event.description().isBlank()) {
            description.append("\n\n").append(event.description());
        }
        return description.toString();
    }

    private String uid(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest) + "@campusday.local";
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
    }

    private void line(StringBuilder target, String value) {
        StringBuilder folded = new StringBuilder();
        int bytesOnLine = 0;
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            int characterBytes = character.getBytes(StandardCharsets.UTF_8).length;
            if (bytesOnLine > 0 && bytesOnLine + characterBytes > 73) {
                folded.append("\r\n ");
                bytesOnLine = 1;
            }
            folded.append(character);
            bytesOnLine += characterBytes;
            offset += Character.charCount(codePoint);
        }
        target.append(folded).append("\r\n");
    }
}
