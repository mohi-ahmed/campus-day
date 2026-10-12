package com.spark.campusday.student.profile.dto;

import com.spark.campusday.student.model.AcademicYear;
import com.spark.campusday.student.model.StudentGender;

public record StudentProfileResponse(
        String studentId,
        String name,
        String email,
        AcademicYear academicYear,
        String phoneNumber,
        StudentGender gender,
        String profileImageUrl,
        boolean mealManagerAccess
) {
}
