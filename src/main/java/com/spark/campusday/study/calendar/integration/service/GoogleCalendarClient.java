package com.spark.campusday.study.calendar.integration.service;

import com.spark.campusday.study.calendar.dto.CalendarEventResponse;
import com.spark.campusday.study.calendar.integration.config.GoogleCalendarProperties;
import com.spark.campusday.study.calendar.integration.dto.GoogleTokenResponse;
import com.spark.campusday.study.calendar.integration.exception.CalendarIntegrationException;
import com.spark.campusday.study.calendar.integration.exception.GoogleCalendarAuthorizationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleCalendarClient {
    private static final String EVENTS_SCOPE =
            "https://www.googleapis.com/auth/calendar.events";
    private static final DateTimeFormatter GOOGLE_DATE_TIME =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Qualifier("googleCalendarRestClient")
    private final RestClient restClient;
    private final GoogleCalendarProperties properties;
    @Qualifier("studyClock")
    private final Clock clock;

    public String authorizationUrl(String state) {
        return UriComponentsBuilder.fromUriString(properties.authorizationUri())
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", EVENTS_SCOPE)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("include_granted_scopes", "true")
                .queryParam("state", state)
                .build().encode().toUriString();
    }

    public GoogleTokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> form = baseTokenForm();
        form.add("code", code);
        form.add("redirect_uri", properties.redirectUri());
        form.add("grant_type", "authorization_code");
        return token(form, true, "authorize");
    }

    public GoogleTokenResponse refresh(String refreshToken) {
        MultiValueMap<String, String> form = baseTokenForm();
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");
        return token(form, false, "refresh");
    }

    public String createEvent(String accessToken, String localEventKey,
                              CalendarEventResponse event) {
        try {
            Map<String, Object> response = restClient.post()
                    .uri(eventsUri())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(eventBody(localEventKey, event))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            Object id = response == null ? null : response.get("id");
            if (id == null || id.toString().isBlank()) {
                throw new CalendarIntegrationException(
                        "Google Calendar did not return an event id");
            }
            return id.toString();
        } catch (RestClientResponseException exception) {
            throw googleFailure("create", exception);
        }
    }

    public boolean updateEvent(String accessToken, String externalEventId,
                               String localEventKey, CalendarEventResponse event) {
        try {
            restClient.put()
                    .uri(eventUri(externalEventId))
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(eventBody(localEventKey, event))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404
                    || exception.getStatusCode().value() == 410) {
                return false;
            }
            throw googleFailure("update", exception);
        }
    }

    public boolean deleteEvent(String accessToken, String externalEventId) {
        try {
            restClient.delete()
                    .uri(eventUri(externalEventId))
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404
                    || exception.getStatusCode().value() == 410) {
                return false;
            }
            throw googleFailure("delete", exception);
        }
    }

    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", token);
        try {
            restClient.post()
                    .uri("https://oauth2.googleapis.com/revoke")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw googleFailure("disconnect", exception);
        }
    }

    private GoogleTokenResponse token(MultiValueMap<String, String> form,
                                      boolean includeRefreshToken,
                                      String operation) {
        try {
            Map<String, Object> response = restClient.post()
                    .uri(properties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            String accessToken = stringValue(response, "access_token");
            String refreshToken = includeRefreshToken
                    ? optionalStringValue(response, "refresh_token") : null;
            long expiresIn = longValue(response, "expires_in", 3600L);
            return new GoogleTokenResponse(accessToken, refreshToken,
                    Instant.now(clock).plusSeconds(Math.max(60L, expiresIn)));
        } catch (RestClientResponseException exception) {
            throw googleFailure(operation, exception);
        }
    }

    private MultiValueMap<String, String> baseTokenForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        return form;
    }

    private Map<String, Object> eventBody(String localEventKey,
                                          CalendarEventResponse event) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("summary", event.title());
        body.put("description", description(event));
        body.put("status", "confirmed");
        body.put("start", start(event));
        body.put("end", end(event));
        body.put("extendedProperties", Map.of("private",
                Map.of("campusDayKey", localEventKey)));
        return body;
    }

    private Map<String, String> start(CalendarEventResponse event) {
        if (event.allDay() || event.startTime() == null) {
            return Map.of("date", event.date().toString());
        }
        return Map.of("dateTime", dateTime(event.date(), event.startTime()),
                "timeZone", clock.getZone().getId());
    }

    private Map<String, String> end(CalendarEventResponse event) {
        if (event.allDay() || event.startTime() == null) {
            return Map.of("date", event.date().plusDays(1).toString());
        }
        LocalTime endTime = event.endTime() == null
                ? event.startTime().plusMinutes(30) : event.endTime();
        LocalDate endDate = endTime.isAfter(event.startTime())
                ? event.date() : event.date().plusDays(1);
        return Map.of("dateTime", dateTime(endDate, endTime),
                "timeZone", clock.getZone().getId());
    }

    private String dateTime(LocalDate date, LocalTime time) {
        ZoneId zone = clock.getZone();
        return ZonedDateTime.of(LocalDateTime.of(date, time), zone)
                .format(GOOGLE_DATE_TIME);
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

    private URI eventsUri() {
        return UriComponentsBuilder.fromUriString(properties.calendarApiBaseUri())
                .pathSegment("calendars", "primary", "events")
                .build().encode().toUri();
    }

    private URI eventUri(String eventId) {
        return UriComponentsBuilder.fromUriString(properties.calendarApiBaseUri())
                .pathSegment("calendars", "primary", "events", eventId)
                .build().encode().toUri();
    }

    private String stringValue(Map<String, Object> response, String key) {
        String value = optionalStringValue(response, key);
        if (value == null) {
            throw new CalendarIntegrationException(
                    "Google Calendar response is missing " + key);
        }
        return value;
    }

    private String optionalStringValue(Map<String, Object> response, String key) {
        Object value = response == null ? null : response.get(key);
        return value == null || value.toString().isBlank() ? null : value.toString();
    }

    private long longValue(Map<String, Object> response, String key, long fallback) {
        Object value = response == null ? null : response.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? fallback : Long.parseLong(value.toString());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private CalendarIntegrationException googleFailure(
            String operation, RestClientResponseException exception) {
        String googleError = googleError(exception);
        if ("invalid_grant".equals(googleError)) {
            return new GoogleCalendarAuthorizationException(
                    "Google Calendar authorization was revoked or expired. "
                            + "Reconnect the calendar", true);
        }
        if ("invalid_client".equals(googleError)) {
            return new GoogleCalendarAuthorizationException(
                    "Google Calendar OAuth client credentials are invalid. "
                            + "Check the configured client ID and client secret", false);
        }
        if ("redirect_uri_mismatch".equals(googleError)) {
            return new GoogleCalendarAuthorizationException(
                    "Google Calendar redirect URI does not match the OAuth client configuration",
                    false);
        }
        if (exception.getStatusCode().value() == 401) {
            return new GoogleCalendarAuthorizationException(
                    "Google Calendar authorization expired. Reconnect the calendar", true);
        }
        String target = switch (operation) {
            case "authorize", "refresh" -> "authorization";
            case "disconnect" -> "calendar connection";
            default -> "event";
        };
        return new CalendarIntegrationException(
                "Google Calendar could not " + operation + " " + target + " ("
                        + exception.getStatusCode().value() + ")");
    }

    private String googleError(RestClientResponseException exception) {
        try {
            Map<String, Object> response = exception.getResponseBodyAs(
                    new ParameterizedTypeReference<>() {
                    });
            return optionalStringValue(response, "error");
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
