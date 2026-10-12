package com.spark.campusday.study.calendar.integration.service;

import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import com.spark.campusday.study.calendar.integration.config.CalendarIntegrationProperties;
import com.spark.campusday.study.calendar.integration.config.GoogleCalendarProperties;
import com.spark.campusday.study.calendar.integration.dto.CalendarSyncRequest;
import com.spark.campusday.study.calendar.integration.exception.GoogleCalendarAuthorizationException;
import com.spark.campusday.study.calendar.integration.model.CalendarIntegrationProvider;
import com.spark.campusday.study.calendar.integration.model.StudyCalendarConnection;
import com.spark.campusday.study.calendar.integration.repository.StudyCalendarConnectionRepository;
import com.spark.campusday.study.calendar.integration.repository.StudyCalendarOAuthStateRepository;
import com.spark.campusday.study.calendar.integration.repository.StudyCalendarSyncItemRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StudyCalendarIntegrationServiceTest {
    private static final String PUBLIC_STUDENT_ID = "2023200000578";
    private static final String OWNER_ID = "mongo-student-id";
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 11);

    private final StudyCalendarConnectionRepository connections =
            mock(StudyCalendarConnectionRepository.class);
    private final StudyCalendarOAuthStateRepository oauthStates =
            mock(StudyCalendarOAuthStateRepository.class);
    private final StudyCalendarSyncItemRepository syncItems =
            mock(StudyCalendarSyncItemRepository.class);
    private final StudentService students = mock(StudentService.class);
    private final CalendarExportEventService exportEvents =
            mock(CalendarExportEventService.class);
    private final GoogleCalendarClient google = mock(GoogleCalendarClient.class);
    private final CalendarTokenCipher tokenCipher = mock(CalendarTokenCipher.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-10-11T02:00:00Z"), ZoneId.of("Asia/Dhaka"));
    private final StudyCalendarIntegrationService service =
            new StudyCalendarIntegrationService(
                    connections, oauthStates, syncItems, students, exportEvents,
                    google, tokenCipher,
                    new GoogleCalendarProperties(
                            "client", "secret", "http://localhost/callback",
                            "https://accounts.google.com/o/oauth2/v2/auth",
                            "https://oauth2.googleapis.com/token",
                            "https://www.googleapis.com/calendar/v3"),
                    new CalendarIntegrationProperties("encryption-key", Duration.ofMinutes(10)),
                    clock);

    @Test
    void skipsAutomaticSyncWhenAuthorizationNeedsReconnect() {
        StudyCalendarConnection connection = connection(false);
        studentOwner();
        when(connections.findByStudentIdAndProvider(
                OWNER_ID, CalendarIntegrationProvider.GOOGLE))
                .thenReturn(Optional.of(connection));

        boolean synced = service.syncGoogleIfConnected(
                PUBLIC_STUDENT_ID, new CalendarSyncRequest(TODAY, TODAY));

        assertFalse(synced);
        verifyNoInteractions(google, tokenCipher, exportEvents, syncItems);
        verify(connections, never()).save(connection);
    }

    @Test
    void revokedRefreshTokenMarksConnectionForReconnect() {
        StudyCalendarConnection connection = connection(true);
        studentOwner();
        when(connections.findByStudentIdAndProvider(
                OWNER_ID, CalendarIntegrationProvider.GOOGLE))
                .thenReturn(Optional.of(connection));
        when(tokenCipher.decrypt("encrypted-refresh")).thenReturn("refresh-token");
        when(google.refresh("refresh-token")).thenThrow(
                new GoogleCalendarAuthorizationException(
                        "Google Calendar authorization was revoked or expired. "
                                + "Reconnect the calendar", true));

        boolean synced = service.syncGoogleIfConnected(
                PUBLIC_STUDENT_ID, new CalendarSyncRequest(TODAY, TODAY));

        assertFalse(synced);
        assertFalse(connection.getAuthorizationValid());
        verify(connections).save(connection);
        verifyNoInteractions(exportEvents, syncItems);
    }

    private StudyCalendarConnection connection(boolean authorizationValid) {
        return StudyCalendarConnection.builder()
                .id("connection-id")
                .studentId(OWNER_ID)
                .provider(CalendarIntegrationProvider.GOOGLE)
                .encryptedAccessToken("encrypted-access")
                .encryptedRefreshToken("encrypted-refresh")
                .accessTokenExpiresAt(Instant.parse("2026-10-11T01:00:00Z"))
                .connectedAt(Instant.parse("2026-10-01T01:00:00Z"))
                .authorizationValid(authorizationValid)
                .build();
    }

    private void studentOwner() {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(OWNER_ID);
        when(students.getByStudentId(PUBLIC_STUDENT_ID)).thenReturn(student);
    }
}
