package com.spark.campusday.study.insights.dto;

import java.time.LocalDate;

public record DailyStudyTrendResponse(
        LocalDate date,
        int plannedMinutes,
        int focusedMinutes,
        int completedTasks,
        int completedHabits
) {
}
