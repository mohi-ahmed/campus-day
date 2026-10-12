package com.spark.campusday.marketplace.image.controller;

import com.spark.campusday.marketplace.image.dto.MarketplaceImageResponse;
import com.spark.campusday.marketplace.image.service.MarketplaceImageService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/marketplace/images")
@RequiredArgsConstructor
public class MarketplaceImageController {
    private final MarketplaceImageService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MarketplaceImageResponse upload(
            @RequestParam @NotBlank String studentId,
            @RequestParam("file") MultipartFile file) {
        return service.upload(studentId, file);
    }

    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemporary(
            @PathVariable @NotBlank String imageId,
            @RequestParam @NotBlank String studentId) {
        service.deleteTemporary(imageId, studentId);
    }
}
