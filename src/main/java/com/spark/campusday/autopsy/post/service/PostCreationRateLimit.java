package com.spark.campusday.autopsy.post.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.spark.campusday.common.exception.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.function.Supplier;

@Component
public class PostCreationRateLimit {
    private static final int DAILY_POST_LIMIT = 10;
    private static final int MAX_TRACKED_ACTORS = 100_000;
    private static final ZoneId CAMPUS_TIME_ZONE = ZoneId.of("Asia/Dhaka");
    private static final Duration POST_COOLDOWN = Duration.ofMinutes(3);

    private final Cache<String, PostCreationState> creationStates = Caffeine.newBuilder()
            .maximumSize(MAX_TRACKED_ACTORS)
            .expireAfterAccess(Duration.ofHours(26))
            .build();
    public <T> T execute(String actorKey, Supplier<T> action) {
        PostCreationState state = creationStates.get(actorKey, ignored -> new PostCreationState());
        LocalDate today = LocalDate.now(CAMPUS_TIME_ZONE);
        Instant now = Instant.now();
        reserve(state, today, now);

        try {
            T result = action.get();
            markSuccessful(state);
            return result;
        } catch (RuntimeException exception) {
            releaseReservation(state, today);
            throw exception;
        }
    }

    private void reserve(PostCreationState state, LocalDate today, Instant now) {
        synchronized (state) {
            state.startDay(today);
            if (state.lastSuccessfulPostAt != null) {
                Duration elapsed = Duration.between(state.lastSuccessfulPostAt, now);
                if (elapsed.compareTo(POST_COOLDOWN) < 0) {
                    Duration remaining = POST_COOLDOWN.minus(elapsed);
                    long waitSeconds = Math.max(1, (remaining.toMillis() + 999) / 1_000);
                    throw new RateLimitExceededException(
                            "Please wait " + waitSeconds + " seconds before creating another post");
                }
            }
            if (state.dailyPostCount >= DAILY_POST_LIMIT) {
                throw new RateLimitExceededException(
                        "Daily anonymous post limit reached. You can create up to 10 posts per day");
            }
            state.dailyPostCount++;
        }
    }

    private void markSuccessful(PostCreationState state) {
        synchronized (state) {
            state.lastSuccessfulPostAt = Instant.now();
        }
    }

    private void releaseReservation(PostCreationState state, LocalDate reservationDate) {
        synchronized (state) {
            if (reservationDate.equals(state.date) && state.dailyPostCount > 0) {
                state.dailyPostCount--;
            }
        }
    }

    private static final class PostCreationState {
        private LocalDate date;
        private int dailyPostCount;
        private Instant lastSuccessfulPostAt;

        private void startDay(LocalDate today) {
            if (!today.equals(date)) {
                date = today;
                dailyPostCount = 0;
            }
        }
    }
}
