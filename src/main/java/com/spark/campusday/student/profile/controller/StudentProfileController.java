package com.spark.campusday.student.profile.controller;

import com.spark.campusday.auth.security.CampusUserPrincipal;
import com.spark.campusday.student.profile.dto.StudentProfileResponse;
import com.spark.campusday.student.profile.dto.UpdateStudentProfileRequest;
import com.spark.campusday.student.profile.service.StudentProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/students/me/profile")
@RequiredArgsConstructor
public class StudentProfileController {
    private final StudentProfileService service;

    @GetMapping
    StudentProfileResponse current(@AuthenticationPrincipal CampusUserPrincipal principal) {
        return service.current(principal);
    }

    @PutMapping
    StudentProfileResponse update(
            @AuthenticationPrincipal CampusUserPrincipal principal,
            @Valid @RequestBody UpdateStudentProfileRequest request) {
        return service.update(principal, request);
    }

    @PostMapping("/image")
    StudentProfileResponse uploadImage(
            @AuthenticationPrincipal CampusUserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        return service.uploadImage(principal, file);
    }

    @DeleteMapping("/image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeImage(@AuthenticationPrincipal CampusUserPrincipal principal) {
        service.removeImage(principal);
    }
}
