package com.spark.campusday.currentaffairs.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.spark.campusday.currentaffairs.config.CurrentAffairsProperties;
import com.spark.campusday.currentaffairs.model.CurrentAffairsRightsStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrentAffairsMediaService {
    private final Cloudinary cloudinary;
    private final CurrentAffairsProperties properties;

    public MediaReference resolve(
            String sourceImageUrl,
            String externalArticleId,
            CurrentAffairsRightsStatus rightsStatus,
            String existingCloudinaryUrl,
            String existingPublicId) {
        if (isBlank(sourceImageUrl)) return new MediaReference(null, null, null);
        if (!isBlank(existingCloudinaryUrl)) {
            return new MediaReference(sourceImageUrl, existingCloudinaryUrl, existingPublicId);
        }
        if (!properties.cloudinaryMirroringEnabled()
                || rightsStatus != CurrentAffairsRightsStatus.CLEARED) {
            return new MediaReference(sourceImageUrl, null, null);
        }

        String publicId = "newsdata-" + CurrentAffairsDigest.sha256(externalArticleId).substring(0, 24);
        try {
            Map<?, ?> result = cloudinary.uploader().upload(sourceImageUrl, ObjectUtils.asMap(
                    "resource_type", "image",
                    "asset_folder", properties.cloudinaryAssetFolder(),
                    "public_id", publicId,
                    "overwrite", false));
            Object secureUrl = result.get("secure_url");
            Object storedPublicId = result.get("public_id");
            return new MediaReference(
                    sourceImageUrl,
                    secureUrl == null ? null : secureUrl.toString(),
                    storedPublicId == null ? publicId : storedPublicId.toString());
        } catch (IOException | RuntimeException exception) {
            log.warn("Could not mirror an approved Current Affairs image to Cloudinary; using source URL");
            return new MediaReference(sourceImageUrl, null, null);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record MediaReference(
            String originalUrl,
            String cloudinaryUrl,
            String cloudinaryPublicId
    ) {
    }
}
