package com.spark.campusday.currentaffairs.service;

import com.spark.campusday.currentaffairs.config.CurrentAffairsConfiguration;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

@Service
public class CurrentAffairsCacheService {
    @Caching(evict = {
            @CacheEvict(cacheNames = CurrentAffairsConfiguration.FEED_CACHE, allEntries = true),
            @CacheEvict(cacheNames = CurrentAffairsConfiguration.ARTICLE_CACHE, allEntries = true),
            @CacheEvict(cacheNames = CurrentAffairsConfiguration.HIGHLIGHTS_CACHE, allEntries = true)
    })
    public void evictContentCaches() {
        // Cache eviction is provided by the annotation at this service boundary.
    }
}
