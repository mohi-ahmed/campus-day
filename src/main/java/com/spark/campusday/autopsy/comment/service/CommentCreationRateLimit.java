package com.spark.campusday.autopsy.comment.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.spark.campusday.common.exception.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class CommentCreationRateLimit {
    private static final int HOURLY_COMMENT_LIMIT = 30;
    private static final int MAX_TRACKED_ACTORS = 100_000;
    private static final Duration COMMENT_COOLDOWN = Duration.ofSeconds(10);
    private static final Duration COMMENT_WINDOW = Duration.ofHours(1);

    private final Set<String> activeCommentCreations = ConcurrentHashMap.newKeySet();
    private final Cache<String, CommentCreationState> creationStates = Caffeine.newBuilder()
            .maximumSize(MAX_TRACKED_ACTORS)
            .expireAfterAccess(Duration.ofHours(2))
            .build();

    public <T> T execute(String actorKey, Supplier<T> action) {
        if (!activeCommentCreations.add(actorKey)) {
            throw new RateLimitExceededException(
                    "Only one anonymous comment can be submitted at a time");
        }

        try {
            return executeRateLimited(actorKey, action);
        } finally {
            activeCommentCreations.remove(actorKey);
        }
    }

    private <T> T executeRateLimited(String actorKey, Supplier<T> action) {
        CommentCreationState state = creationStates.get(actorKey, ignored -> new CommentCreationState());
        Instant now = Instant.now();
        Instant reservationWindow = reserve(state, now);
        try {
            T result = action.get();
            markSuccessful(state);
            return result;
        } catch (RuntimeException exception) {
            releaseReservation(state, reservationWindow);
            throw exception;
        }
    }

    private Instant reserve(CommentCreationState state, Instant now) {
        synchronized (state) {
            state.startWindow(now);
            if (state.lastSuccessfulCommentAt != null) {
                Duration elapsed = Duration.between(state.lastSuccessfulCommentAt, now);
                if (elapsed.compareTo(COMMENT_COOLDOWN) < 0) {
                    Duration remaining = COMMENT_COOLDOWN.minus(elapsed);
                    long waitSeconds = Math.max(1, (remaining.toMillis() + 999) / 1_000);
                    throw new RateLimitExceededException(
                            "Please wait " + waitSeconds + " seconds before creating another comment");
                }
            }
            if (state.windowCommentCount >= HOURLY_COMMENT_LIMIT) {
                throw new RateLimitExceededException(
                        "Hourly anonymous comment limit reached. You can create up to 30 comments per hour");
            }
            state.windowCommentCount++;
            return state.windowStartedAt;
        }
    }

    private void markSuccessful(CommentCreationState state) {
        synchronized (state) {
            state.lastSuccessfulCommentAt = Instant.now();
        }
    }

    private void releaseReservation(CommentCreationState state, Instant reservationWindow) {
        synchronized (state) {
            if (reservationWindow.equals(state.windowStartedAt) && state.windowCommentCount > 0) {
                state.windowCommentCount--;
            }
        }
    }

    private static final class CommentCreationState {
        private Instant windowStartedAt;
        private int windowCommentCount;
        private Instant lastSuccessfulCommentAt;

        private void startWindow(Instant now) {
            if (windowStartedAt == null || !now.isBefore(windowStartedAt.plus(COMMENT_WINDOW))) {
                windowStartedAt = now;
                windowCommentCount = 0;
            }
        }
    }
}
