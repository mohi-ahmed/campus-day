package com.spark.campusday.study.calendar.integration.controller;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.study.calendar.integration.dto.CalendarAuthorizationResponse;
import com.spark.campusday.study.calendar.integration.dto.CalendarIntegrationStatusResponse;
import com.spark.campusday.study.calendar.integration.dto.CalendarSyncRequest;
import com.spark.campusday.study.calendar.integration.dto.CalendarSyncResponse;
import com.spark.campusday.study.calendar.integration.exception.CalendarIntegrationException;
import com.spark.campusday.study.calendar.integration.service.IcsCalendarExportService;
import com.spark.campusday.study.calendar.integration.service.StudyCalendarIntegrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/study/calendar/integrations")
@RequiredArgsConstructor
public class StudyCalendarIntegrationController {
    private static final MediaType CALENDAR_MEDIA_TYPE =
            MediaType.parseMediaType("text/calendar;charset=UTF-8");

    private final StudyCalendarIntegrationService integrations;
    private final IcsCalendarExportService icsExport;

    @GetMapping("/status")
    public CalendarIntegrationStatusResponse status(
            @RequestParam @NotBlank String studentId) {
        return integrations.status(studentId);
    }

    @PostMapping("/google/authorization-url")
    public CalendarAuthorizationResponse googleAuthorizationUrl(
            @RequestParam @NotBlank String studentId) {
        return integrations.beginGoogleAuthorization(studentId);
    }

    @GetMapping(value = "/google/callback", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> googleCallback(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error) {
        if (error != null && !error.isBlank()) {
            return callbackPage(false);
        }
        try {
            integrations.completeGoogleAuthorization(state, code);
            return callbackPage(true);
        } catch (InvalidRequestException | CalendarIntegrationException exception) {
            return callbackPage(false);
        }
    }

    @PostMapping("/google/sync")
    public CalendarSyncResponse syncGoogle(
            @RequestParam @NotBlank String studentId,
            @Valid @RequestBody CalendarSyncRequest request) {
        return integrations.syncGoogle(studentId, request);
    }

    @DeleteMapping("/google")
    public ResponseEntity<Void> disconnectGoogle(
            @RequestParam @NotBlank String studentId) {
        integrations.disconnectGoogle(studentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/apple.ics", produces = "text/calendar")
    public ResponseEntity<byte[]> appleCalendar(
            @RequestParam @NotBlank String studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] calendar = icsExport.export(studentId, from, to);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("campus-day-study.ics", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(CALENDAR_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(calendar.length)
                .body(calendar);
    }

    private ResponseEntity<String> callbackPage(boolean success) {
        String event = success
                ? "campus-day-calendar-connected"
                : "campus-day-calendar-error";
        String title = success ? "Google Calendar connected" : "Connection failed";
        String message = success
                ? "You can close this window and return to Campus Day."
                : "Google Calendar was not connected. Close this window and try again.";
        String html = """
                <!doctype html>
                <html lang="en">
                <head><meta charset="utf-8"><meta name="viewport" content="width=device-width">
                <title>%s</title></head>
                <body style="font-family:system-ui;padding:32px;color:#17231f">
                <h1 style="font-size:24px">%s</h1><p>%s</p>
                <script>if(window.opener){window.opener.postMessage({type:'%s'},'*');window.close();}</script>
                </body></html>
                """.formatted(title, title, message, event);
        return ResponseEntity.status(success ? 200 : 400)
                .contentType(MediaType.TEXT_HTML)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(html);
    }
}
