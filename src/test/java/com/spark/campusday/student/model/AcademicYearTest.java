package com.spark.campusday.student.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcademicYearTest {
    @Test
    void classifiesEveryAcademicYear() {
        for (AcademicYear year : AcademicYear.values()) {
            assertTrue(year.isFixedChargeYear() != year.isFinalYear());
            assertTrue(year.isFourthYear() == (year == AcademicYear.FOURTH_YEAR));
        }
        assertFalse(AcademicYear.FINAL_YEAR.isFixedChargeYear());
        assertTrue(AcademicYear.FINAL_YEAR.isFinalYear());
        assertTrue(AcademicYear.FOURTH_YEAR.isFixedChargeYear());
    }
}
