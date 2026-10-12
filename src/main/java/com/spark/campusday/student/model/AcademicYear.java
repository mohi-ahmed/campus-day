package com.spark.campusday.student.model;

public enum AcademicYear {
    FIRST_YEAR,
    SECOND_YEAR,
    THIRD_YEAR,
    FOURTH_YEAR,
    FINAL_YEAR;

    public boolean isFinalYear() {
        return this == FINAL_YEAR;
    }

    public boolean isFixedChargeYear() {
        return this != FINAL_YEAR;
    }

    public boolean isFourthYear() {
        return this == FOURTH_YEAR;
    }
}
