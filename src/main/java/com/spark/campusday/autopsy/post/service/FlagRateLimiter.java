package com.spark.campusday.autopsy.post.service;

import com.spark.campusday.common.exception.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class FlagRateLimiter {
    private final Set<String> activePostCreations = ConcurrentHashMap.newKeySet();

    public <T> T execute(String actorKey, Supplier<T> action) {
        if (!activePostCreations.add(actorKey)) {
            throw new RateLimitExceededException(
                    "Only one anonymous post can be submitted at a time");
        }

        try {
            return action.get();
        } finally {
            activePostCreations.remove(actorKey);
        }
    }
}
