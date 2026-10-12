package com.spark.campusday.study.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class StudyTimeConfiguration {
    @Bean
    Clock studyClock(@Value("${app.study.zone-id:Asia/Dhaka}") String zoneId) {
        return Clock.system(ZoneId.of(zoneId));
    }
}
