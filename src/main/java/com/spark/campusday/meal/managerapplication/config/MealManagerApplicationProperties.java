package com.spark.campusday.meal.managerapplication.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.meal.manager-application")
public class MealManagerApplicationProperties {
    private String adminEmail;
    private URI frontendBaseUrl = URI.create("http://localhost:4173");
    private Duration decisionTtl = Duration.ofHours(72);
}
