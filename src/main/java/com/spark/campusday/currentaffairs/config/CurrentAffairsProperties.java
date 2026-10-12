package com.spark.campusday.currentaffairs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties("app.current-affairs")
public record CurrentAffairsProperties(
        boolean enabled,
        String newsdataApiKey,
        URI newsdataBaseUrl,
        String country,
        String languages,
        int pageSize,
        String schemaVersion,
        boolean cloudinaryMirroringEnabled,
        String cloudinaryAssetFolder,
        boolean sourceRightsCleared
) {
}
