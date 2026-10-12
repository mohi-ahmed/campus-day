package com.spark.campusday.marketplace.image.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.marketplace.image.config.CloudinaryProperties;
import com.spark.campusday.marketplace.image.dto.MarketplaceImageResponse;
import com.spark.campusday.marketplace.image.model.MarketplaceImage;
import com.spark.campusday.marketplace.image.model.MarketplaceImageStatus;
import com.spark.campusday.marketplace.image.repository.MarketplaceImageRepository;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketplaceImageService {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");

    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;
    private final MarketplaceImageRepository images;
    private final StudentService students;

    public MarketplaceImageResponse upload(String studentId, MultipartFile file) {
        String normalizedStudentId = studentId.trim();
        students.getByStudentId(normalizedStudentId);
        validateConfiguration();
        validateFile(file);

        String publicId = "listing-" + UUID.randomUUID();
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", "image",
                    "asset_folder", properties.assetFolder(),
                    "public_id", publicId,
                    "overwrite", false));
            MarketplaceImage image = MarketplaceImage.builder()
                    .uploaderStudentId(normalizedStudentId)
                    .assetId(requiredString(result, "asset_id"))
                    .publicId(requiredString(result, "public_id"))
                    .secureUrl(requiredString(result, "secure_url"))
                    .format(requiredString(result, "format"))
                    .width(requiredNumber(result, "width").intValue())
                    .height(requiredNumber(result, "height").intValue())
                    .bytes(requiredNumber(result, "bytes").longValue())
                    .originalFilename(file.getOriginalFilename())
                    .build();
            return response(images.save(image));
        } catch (IOException exception) {
            throw new InvalidBusinessStateException("Could not upload the marketplace image");
        }
    }

    public List<MarketplaceImage> requireTemporaryImages(Collection<String> imageIds, String studentId) {
        if (imageIds.isEmpty()) {
            throw new InvalidRequestException("At least one product image is required");
        }
        if (imageIds.size() > 8 || imageIds.stream().distinct().count() != imageIds.size()) {
            throw new InvalidRequestException("A listing can contain 1 to 8 unique images");
        }
        List<MarketplaceImage> found = images.findAllByIdInAndUploaderStudentIdAndStatus(
                imageIds, studentId, MarketplaceImageStatus.TEMPORARY);
        if (found.size() != imageIds.size()) {
            throw new InvalidRequestException("One or more images are unavailable or belong to another student");
        }
        Map<String, MarketplaceImage> byId = found.stream()
                .collect(Collectors.toMap(MarketplaceImage::getId, Function.identity()));
        return imageIds.stream().map(byId::get).toList();
    }

    public List<MarketplaceImage> attachToListing(
            List<MarketplaceImage> temporaryImages,
            String listingId) {
        List<MarketplaceImage> attached = java.util.stream.IntStream.range(0, temporaryImages.size())
                .mapToObj(index -> temporaryImages.get(index).toBuilder()
                        .listingId(listingId)
                        .sortOrder(index)
                        .status(MarketplaceImageStatus.ATTACHED)
                        .build())
                .toList();
        return images.saveAll(attached);
    }

    public Map<String, MarketplaceImage> findByIds(Collection<String> imageIds) {
        if (imageIds.isEmpty()) return Map.of();
        return images.findAllById(imageIds).stream()
                .collect(Collectors.toMap(MarketplaceImage::getId, Function.identity()));
    }

    public List<MarketplaceImageResponse> responses(
            List<String> imageIds,
            Map<String, MarketplaceImage> imageMap) {
        return imageIds.stream()
                .map(imageMap::get)
                .filter(java.util.Objects::nonNull)
                .map(MarketplaceImageService::response)
                .toList();
    }

    public void deleteTemporary(String imageId, String studentId) {
        MarketplaceImage image = images.findById(imageId)
                .filter(item -> item.getUploaderStudentId().equals(studentId))
                .filter(item -> item.getStatus() == MarketplaceImageStatus.TEMPORARY)
                .orElseThrow(() -> new ResourceNotFoundException("Temporary marketplace image not found: " + imageId));
        destroy(image.getPublicId());
        images.delete(image);
    }

    private void validateConfiguration() {
        if (isBlank(properties.cloudName()) || isBlank(properties.apiKey()) || isBlank(properties.apiSecret())) {
            throw new InvalidBusinessStateException("Cloudinary credentials are not configured");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Image file is required");
        }
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidRequestException("Only JPEG, PNG and WebP images are supported");
        }
        if (file.getSize() > properties.maxFileSize().toBytes()) {
            throw new InvalidRequestException("Image size exceeds " + properties.maxFileSize());
        }
    }

    private void destroy(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "image",
                    "invalidate", true));
        } catch (IOException exception) {
            throw new InvalidBusinessStateException("Could not remove the marketplace image");
        }
    }

    private static MarketplaceImageResponse response(MarketplaceImage image) {
        return new MarketplaceImageResponse(
                image.getId(), image.getSecureUrl(), image.getFormat(), image.getWidth(),
                image.getHeight(), image.getBytes(), image.getSortOrder(), image.getCreatedAt());
    }

    private static String requiredString(Map<?, ?> result, String key) {
        Object value = result.get(key);
        if (value == null) throw new InvalidBusinessStateException("Cloudinary response is missing " + key);
        return value.toString();
    }

    private static Number requiredNumber(Map<?, ?> result, String key) {
        Object value = result.get(key);
        if (value instanceof Number number) return number;
        throw new InvalidBusinessStateException("Cloudinary response is missing " + key);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
