package com.spark.campusday.meal.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class MealReadCacheConfiguration {
    public static final String PUBLISHED_MENUS = "publishedMenus";
    public static final String APPLICABLE_MEAL_PRICES = "applicableMealPrices";

    @Bean
    public CacheManager mealReadCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(15)));
        return manager;
    }
}
