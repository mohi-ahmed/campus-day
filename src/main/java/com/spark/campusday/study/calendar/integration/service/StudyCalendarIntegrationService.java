package com.spark.campusday.study.calendar.integration.service;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.student.service.StudentService;
import com.spark.campusday.study.calendar.dto.CalendarEventResponse;
import com.spark.campusday.study.calendar.integration.config.CalendarIntegrationProperties;
import com.spark.campusday.study.calendar.integration.config.GoogleCalendarProperties;
import com.spark.campusday.study.calendar.integration.dto.CalendarAuthorizationResponse;
import com.spark.campusday.study.calendar.integration.dto.CalendarIntegrationStatusResponse;
import com.spark.campusday.study.calendar.integration.dto.CalendarSyncRequest;
import com.spark.campusday.study.calendar.integration.dto.CalendarSyncResponse;
import com.spark.campusday.study.calendar.integration.dto.GoogleTokenResponse;
import com.spark.campusday.study.calendar.integration.exception.CalendarIntegrationException;
import com.spark.campusday.study.calendar.integration.exception.GoogleCalendarAuthorizationException;
import com.spark.campusday.study.calendar.integration.model.CalendarIntegrationProvider;
import com.spark.campusday.study.calendar.integration.model.StudyCalendarConnection;
import com.spark.campusday.study.calendar.integration.model.StudyCalendarOAuthState;
import com.spark.campusday.study.calendar.integration.model.StudyCalendarSyncItem;
import com.spark.campusday.study.calendar.integration.repository.StudyCalendarConnectionRepository;
import com.spark.campusday.study.calendar.integration.repository.StudyCalendarOAuthStateRepository;
import com.spark.campusday.study.calendar.integration.repository.StudyCalendarSyncItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class StudyCalendarIntegrationService {
    private static final int STATE_BYTES = 32;
    private static final int MAX_SYNC_DAYS = 366;
    private static final Duration DEFAULT_STATE_TTL = Duration.ofMinutes(10);
    private static final Duration TOKEN_REFRESH_WINDOW = Duration.ofMinutes(1);

    private final StudyCalendarConnectionRepository connections;
    private final StudyCalendarOAuthStateRepository oauthStates;
    private final StudyCalendarSyncItemRepository syncItems;
    private final StudentService students;
    private final CalendarExportEventService exportEvents;
    private final GoogleCalendarClient google;
    private final CalendarTokenCipher tokenCipher;
    private final GoogleCalendarProperties googleProperties;
    private final CalendarIntegrationProperties integrationProperties;
    @Qualifier("studyClock")
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentMap<String, ReentrantLock> syncLocks = new ConcurrentHashMap<>();

    public CalendarIntegrationStatusResponse status(String studentId) {
        String ownerId = ownerId(studentId);
        StudyCalendarConnection connection = googleConnection(ownerId);
        boolean connected = hasUsableAuthorization(connection);
        return new CalendarIntegrationStatusResponse(
                googleProperties.configured(),
                connected,
                connected ? connection.getConnectedAt() : null,
                connected ? connection.getLastSyncedAt() : null,
                true);
    }

    public CalendarAuthorizationResponse beginGoogleAuthorization(String studentId) {
        requireGoogleConfiguration();
        String ownerId = ownerId(studentId);
        String state = newState();
        Duration ttl = integrationProperties.oauthStateTtl() == null
                ? DEFAULT_STATE_TTL : integrationProperties.oauthStateTtl();
        oauthStates.save(StudyCalendarOAuthState.builder()
                .state(state)
                .studentId(ownerId)
                .expiresAt(Instant.now(clock).plus(ttl))
                .build());
        return new CalendarAuthorizationResponse(google.authorizationUrl(state));
    }

    public void completeGoogleAuthorization(String state, String code) {
        requireGoogleConfiguration();
        if (state == null || state.isBlank() || code == null || code.isBlank()) {
            throw new InvalidRequestException("Google authorization code and state are required");
        }
        StudyCalendarOAuthState savedState = oauthStates.findByState(state)
                .orElseThrow(() -> new InvalidRequestException(
                        "Google authorization state is invalid or has expired"));
        oauthStates.delete(savedState);
        if (!savedState.getExpiresAt().isAfter(Instant.now(clock))) {
            throw new InvalidRequestException("Google authorization state has expired");
        }

        GoogleTokenResponse tokens = google.exchangeCode(code);
        StudyCalendarConnection connection = googleConnection(savedState.getStudentId());
        Instant connectedAt = Instant.now(clock);
        if (connection == null) {
            connection = StudyCalendarConnection.builder()
                    .studentId(savedState.getStudentId())
                    .provider(CalendarIntegrationProvider.GOOGLE)
                    .connectedAt(connectedAt)
                    .build();
        }
        connection.setEncryptedAccessToken(tokenCipher.encrypt(tokens.accessToken()));
        if (tokens.refreshToken() != null) {
            connection.setEncryptedRefreshToken(tokenCipher.encrypt(tokens.refreshToken()));
        }
        connection.setAccessTokenExpiresAt(tokens.expiresAt());
        connection.setConnectedAt(connectedAt);
        connection.setAuthorizationValid(true);
        connections.save(connection);
    }

    public CalendarSyncResponse syncGoogle(String studentId, CalendarSyncRequest request) {
        validateSyncRequest(request);
        String ownerId = ownerId(studentId);
        StudyCalendarConnection connection = requireGoogleConnection(ownerId);
        return syncConnectedGoogle(studentId, connection, request);
    }

    public boolean syncGoogleIfConnected(String studentId, CalendarSyncRequest request) {
        validateSyncRequest(request);
        String ownerId = ownerId(studentId);
        StudyCalendarConnection connection = googleConnection(ownerId);
        if (!hasUsableAuthorization(connection)) {
            return false;
        }
        try {
            syncConnectedGoogle(studentId, connection, request);
            return true;
        } catch (GoogleCalendarAuthorizationException exception) {
            if (exception.reconnectRequired()) {
                return false;
            }
            throw exception;
        }
    }

    private CalendarSyncResponse syncConnectedGoogle(
            String studentId, StudyCalendarConnection connection,
            CalendarSyncRequest request) {
        ReentrantLock syncLock = syncLocks.computeIfAbsent(
                connection.getId(), ignored -> new ReentrantLock());
        syncLock.lock();
        try {
            return doSyncConnectedGoogle(studentId, connection, request);
        } catch (GoogleCalendarAuthorizationException exception) {
            if (exception.reconnectRequired()) {
                connection.setAuthorizationValid(false);
                connections.save(connection);
            }
            throw exception;
        } finally {
            syncLock.unlock();
        }
    }

    private CalendarSyncResponse doSyncConnectedGoogle(
            String studentId, StudyCalendarConnection connection,
            CalendarSyncRequest request) {
        String accessToken = usableAccessToken(connection);
        List<CalendarEventResponse> events = exportEvents.events(
                studentId, request.from(), request.to());
        Map<String, CalendarEventResponse> desiredEvents = new LinkedHashMap<>();
        events.forEach(event -> desiredEvents.put(exportEvents.eventKey(event), event));

        List<StudyCalendarSyncItem> existingItems = syncItems
                .findByConnectionIdAndEventDateGreaterThanEqualAndEventDateLessThanEqual(
                        connection.getId(), request.from(), request.to());
        Map<String, StudyCalendarSyncItem> existingByKey = new LinkedHashMap<>();
        existingItems.forEach(item -> existingByKey.put(item.getLocalEventKey(), item));

        int created = 0;
        int updated = 0;
        for (Map.Entry<String, CalendarEventResponse> desired : desiredEvents.entrySet()) {
            String fingerprint = fingerprint(desired.getValue());
            StudyCalendarSyncItem item = existingByKey.remove(desired.getKey());
            if (item == null) {
                String externalId = google.createEvent(
                        accessToken, desired.getKey(), desired.getValue());
                syncItems.save(StudyCalendarSyncItem.builder()
                        .connectionId(connection.getId())
                        .localEventKey(desired.getKey())
                        .eventDate(desired.getValue().date())
                        .externalEventId(externalId)
                        .eventFingerprint(fingerprint)
                        .build());
                created++;
                continue;
            }
            if (fingerprint.equals(item.getEventFingerprint())) {
                continue;
            }
            boolean found = google.updateEvent(accessToken, item.getExternalEventId(),
                    desired.getKey(), desired.getValue());
            if (!found) {
                item.setExternalEventId(google.createEvent(
                        accessToken, desired.getKey(), desired.getValue()));
                created++;
            } else {
                updated++;
            }
            item.setEventDate(desired.getValue().date());
            item.setEventFingerprint(fingerprint);
            syncItems.save(item);
        }

        int deleted = 0;
        for (StudyCalendarSyncItem staleItem : existingByKey.values()) {
            google.deleteEvent(accessToken, staleItem.getExternalEventId());
            syncItems.delete(staleItem);
            deleted++;
        }

        Instant syncedAt = Instant.now(clock);
        connection.setLastSyncedAt(syncedAt);
        connections.save(connection);
        return new CalendarSyncResponse(created, updated, deleted, syncedAt);
    }

    public void disconnectGoogle(String studentId) {
        String ownerId = ownerId(studentId);
        StudyCalendarConnection connection = googleConnection(ownerId);
        if (connection == null) {
            throw new CalendarIntegrationException("Google Calendar is not connected");
        }
        if (!hasUsableAuthorization(connection)) {
            removeLocalConnection(connection);
            return;
        }
        try {
            String accessToken = usableAccessToken(connection);
            List<StudyCalendarSyncItem> items = syncItems.findByConnectionId(connection.getId());
            for (StudyCalendarSyncItem item : items) {
                google.deleteEvent(accessToken, item.getExternalEventId());
            }
            String refreshToken = tokenCipher.decrypt(connection.getEncryptedRefreshToken());
            google.revoke(refreshToken == null ? accessToken : refreshToken);
        } catch (GoogleCalendarAuthorizationException exception) {
            if (!exception.reconnectRequired()) {
                throw exception;
            }
        }
        removeLocalConnection(connection);
    }

    private void removeLocalConnection(StudyCalendarConnection connection) {
        syncItems.deleteAllByConnectionId(connection.getId());
        connections.delete(connection);
    }

    private String usableAccessToken(StudyCalendarConnection connection) {
        if (!hasUsableAuthorization(connection)) {
            throw new GoogleCalendarAuthorizationException(
                    "Google Calendar must be reconnected before syncing", true);
        }
        Instant refreshAt = Instant.now(clock).plus(TOKEN_REFRESH_WINDOW);
        if (connection.getAccessTokenExpiresAt().isAfter(refreshAt)) {
            return tokenCipher.decrypt(connection.getEncryptedAccessToken());
        }
        String refreshToken = tokenCipher.decrypt(connection.getEncryptedRefreshToken());
        if (refreshToken == null) {
            throw new GoogleCalendarAuthorizationException(
                    "Google Calendar must be reconnected before syncing", true);
        }
        GoogleTokenResponse refreshed = google.refresh(refreshToken);
        connection.setEncryptedAccessToken(tokenCipher.encrypt(refreshed.accessToken()));
        connection.setAccessTokenExpiresAt(refreshed.expiresAt());
        connections.save(connection);
        return refreshed.accessToken();
    }

    private StudyCalendarConnection requireGoogleConnection(String ownerId) {
        StudyCalendarConnection connection = googleConnection(ownerId);
        if (!hasUsableAuthorization(connection)) {
            throw new CalendarIntegrationException("Google Calendar is not connected");
        }
        return connection;
    }

    private boolean hasUsableAuthorization(StudyCalendarConnection connection) {
        return connection != null
                && !Boolean.FALSE.equals(connection.getAuthorizationValid());
    }

    private StudyCalendarConnection googleConnection(String ownerId) {
        return connections.findByStudentIdAndProvider(
                        ownerId, CalendarIntegrationProvider.GOOGLE)
                .orElse(null);
    }

    private String ownerId(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            throw new InvalidRequestException("Student id is required");
        }
        return students.getByStudentId(studentId).getId();
    }

    private void validateSyncRequest(CalendarSyncRequest request) {
        if (request == null || request.from() == null || request.to() == null) {
            throw new InvalidRequestException("Calendar sync range is required");
        }
        if (request.to().isBefore(request.from())) {
            throw new InvalidRequestException(
                    "Calendar sync end date must not be before start date");
        }
        long days = ChronoUnit.DAYS.between(request.from(), request.to()) + 1;
        if (days > MAX_SYNC_DAYS) {
            throw new InvalidRequestException(
                    "Calendar sync range cannot exceed " + MAX_SYNC_DAYS + " days");
        }
    }

    private void requireGoogleConfiguration() {
        if (!googleProperties.configured()) {
            throw new CalendarIntegrationException(
                    "Google Calendar credentials are not configured");
        }
    }

    private String fingerprint(CalendarEventResponse event) {
        String payload = String.join("\u001f",
                value(event.title()), value(event.subject()), value(event.itemType()),
                value(event.description()), value(event.date()), value(event.startTime()),
                value(event.endTime()), Boolean.toString(event.allDay()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private String newState() {
        byte[] bytes = new byte[STATE_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
