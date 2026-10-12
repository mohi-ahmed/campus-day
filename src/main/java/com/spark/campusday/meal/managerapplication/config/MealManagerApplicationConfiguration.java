package com.spark.campusday.meal.managerapplication.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MealManagerApplicationProperties.class)
public class MealManagerApplicationConfiguration {
}
