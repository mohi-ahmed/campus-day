package com.spark.campusday.meal.managementperiod.dto;

import com.spark.campusday.student.model.AcademicYear;

public record ManagerSummaryResponse(String id, String studentId, String name, AcademicYear academicYear) {
}
