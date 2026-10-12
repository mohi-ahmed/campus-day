package com.spark.campusday.student.profile.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.spark.campusday.auth.security.CampusUserPrincipal;
import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.profile.config.ProfileImageProperties;
import com.spark.campusday.student.profile.dto.StudentProfileResponse;
import com.spark.campusday.student.profile.dto.UpdateStudentProfileRequest;
import com.spark.campusday.student.service.StudentService;
import com.spark.campusday.student.service.CurrentStudentResolver;
import com.spark.campusday.meal.access.MealManagerAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentProfileService {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");

    private final StudentService students;
    private final CurrentStudentResolver currentStudent;
    private final MealManagerAccess mealManagerAccess;
    private final Cloudinary cloudinary;
    private final ProfileImageProperties imageProperties;

    public StudentProfileResponse current(CampusUserPrincipal principal) {
        return response(currentStudent.require(principal));
    }

    public StudentProfileResponse update(
            CampusUserPrincipal principal,
            UpdateStudentProfileRequest request) {
        Student student = currentStudent.require(principal);
        String phone = normalizePhone(request.phoneNumber());
        students.findByPhoneNumber(phone)
                .filter(existing -> !existing.getId().equals(student.getId()))
                .ifPresent(existing -> {
                    throw new ConflictException("A student profile already exists for this phone number");
                });
        student.updateProfile(request.fullName().trim(), request.academicYear(), phone, request.gender());
        return response(students.save(student));
    }

    public StudentProfileResponse uploadImage(
            CampusUserPrincipal principal,
            MultipartFile file) {
        validateCloudinaryConfiguration();
        validateFile(file);
        Student student = currentStudent.require(principal);
        String newPublicId = "student-" + student.getStudentId() + "-" + UUID.randomUUID();
        String previousPublicId = student.getProfileImagePublicId();
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", "image",
                    "asset_folder", imageProperties.assetFolder(),
                    "public_id", newPublicId,
                    "overwrite", false));
            String uploadedPublicId = requiredString(result, "public_id");
            student.updateProfileImage(requiredString(result, "secure_url"), uploadedPublicId);
            try {
                Student saved = students.save(student);
                destroyQuietly(previousPublicId);
                return response(saved);
            } catch (RuntimeException exception) {
                destroyQuietly(uploadedPublicId);
                throw exception;
            }
        } catch (IOException exception) {
            throw new InvalidBusinessStateException("Could not upload the profile photo");
        }
    }

    public void removeImage(CampusUserPrincipal principal) {
        Student student = currentStudent.require(principal);
        String publicId = student.getProfileImagePublicId();
        if (publicId == null || publicId.isBlank()) return;
        destroy(publicId);
        student.clearProfileImage();
        students.save(student);
    }

    private void validateCloudinaryConfiguration() {
        if (isBlank(cloudinary.config.cloudName)
                || isBlank(cloudinary.config.apiKey)
                || isBlank(cloudinary.config.apiSecret)) {
            throw new InvalidBusinessStateException("Cloudinary credentials are not configured");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Profile photo is required");
        }
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidRequestException("Only JPEG, PNG and WebP images are supported");
        }
        if (file.getSize() > imageProperties.maxFileSize().toBytes()) {
            throw new InvalidRequestException("Profile photo size exceeds " + imageProperties.maxFileSize());
        }
    }

    private void destroy(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "image", "invalidate", true));
        } catch (IOException exception) {
            throw new InvalidBusinessStateException("Could not remove the profile photo");
        }
    }

    private void destroyQuietly(String publicId) {
        if (isBlank(publicId)) return;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "image", "invalidate", true));
        } catch (IOException exception) {
            log.warn("Could not clean up replaced Cloudinary profile image {}", publicId);
        }
    }

    private StudentProfileResponse response(Student student) {
        return new StudentProfileResponse(
                student.getStudentId(), student.getName(), student.getEmail(),
                student.getAcademicYear(), student.getPhoneNumber(), student.getGender(),
                student.getProfileImageUrl(), mealManagerAccess.hasCurrentAccess(student.getId()));
    }

    private static String normalizePhone(String phoneNumber) {
        return phoneNumber.replace(" ", "").replace("-", "");
    }

    private static String requiredString(Map<?, ?> result, String key) {
        Object value = result.get(key);
        if (value == null) {
            throw new InvalidBusinessStateException("Cloudinary response is missing " + key);
        }
        return value.toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
