package com.spark.campusday.currentaffairs.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CurrentAffairsProperties.class)
public class CurrentAffairsConfiguration {
    public static final String FEED_CACHE = "currentAffairsFeed";
    public static final String ARTICLE_CACHE = "currentAffairsArticles";
    public static final String HIGHLIGHTS_CACHE = "currentAffairsHighlights";

    @Bean
    @Qualifier("newsDataRestClient")
    RestClient newsDataRestClient(CurrentAffairsProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.newsdataBaseUrl().toString())
                .build();
    }
}
