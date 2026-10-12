package com.spark.campusday.student.profile.dto;

import com.spark.campusday.student.model.AcademicYear;
import com.spark.campusday.student.model.StudentGender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateStudentProfileRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotNull AcademicYear academicYear,
        @NotNull StudentGender gender,
        @NotBlank
        @Pattern(regexp = "^\\+?[0-9](?:[ -]?[0-9]){9,14}$", message = "must contain 10 to 15 digits")
        String phoneNumber
) {
}
