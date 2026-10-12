package com.spark.campusday.study.insights.dto;

public record RoutineInsightsResponse(
        int activeBlocks,
        int classBlocks,
        int selfStudyBlocks,
        int weeklyPlannedMinutes,
        int weeklyClassMinutes,
        int weeklySelfStudyMinutes
) {
}
