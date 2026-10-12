package com.spark.campusday.study.assistant.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.spark.campusday.common.exception.RateLimitExceededException;
import com.spark.campusday.study.assistant.config.TaskbotProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TaskbotRateLimit {
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final TaskbotProperties properties;
    @Qualifier("studyClock")
    private final Clock clock;
    private final Cache<String, RequestWindow> requestWindows = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(5))
            .maximumSize(10_000)
            .build();

    public void check(String studentOwnerId) {
        Instant now = Instant.now(clock);
        RequestWindow window = requestWindows.get(
                studentOwnerId, ignored -> new RequestWindow(now));
        int limit = Math.max(1, properties.requestsPerMinute());
        synchronized (window) {
            if (!now.isBefore(window.startedAt.plus(WINDOW))) {
                window.startedAt = now;
                window.requests = 0;
            }
            if (window.requests >= limit) {
                throw new RateLimitExceededException(
                        "Taskbot request limit reached. Please wait a minute and try again");
            }
            window.requests++;
        }
    }

    private static final class RequestWindow {
        private Instant startedAt;
        private int requests;

        private RequestWindow(Instant startedAt) {
            this.startedAt = startedAt;
        }
    }
}
