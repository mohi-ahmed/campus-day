package com.spark.campusday.currentaffairs.service;

import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.currentaffairs.config.CurrentAffairsProperties;
import com.spark.campusday.currentaffairs.dto.NewsDataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
public class NewsDataClient {
    @Qualifier("newsDataRestClient")
    private final RestClient restClient;
    private final CurrentAffairsProperties properties;

    public NewsDataResponse fetchLatest() {
        if (properties.newsdataApiKey() == null || properties.newsdataApiKey().isBlank()) {
            throw new InvalidBusinessStateException("NewsData.io API key is not configured");
        }

        RestClientException firstFailure = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                NewsDataResponse response = request();
                if (response == null || !"success".equalsIgnoreCase(response.status())) {
                    throw new InvalidBusinessStateException("NewsData.io returned an unsuccessful response");
                }
                return response;
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode().is4xxClientError()) {
                    throw new InvalidBusinessStateException(
                            "NewsData.io rejected the request with status " + exception.getStatusCode().value());
                }
                firstFailure = exception;
            } catch (RestClientException exception) {
                firstFailure = exception;
            }
        }
        throw new InvalidBusinessStateException(
                firstFailure == null
                        ? "NewsData.io request failed"
                        : "NewsData.io is temporarily unavailable");
    }

    private NewsDataResponse request() {
        int size = Math.max(1, Math.min(properties.pageSize(), 10));
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/1/latest")
                        .queryParam("apikey", properties.newsdataApiKey())
                        .queryParam("country", properties.country())
                        .queryParam("language", properties.languages())
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .body(NewsDataResponse.class);
    }
}
