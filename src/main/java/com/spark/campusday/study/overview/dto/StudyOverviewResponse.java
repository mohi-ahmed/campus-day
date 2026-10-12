package com.spark.campusday.study.overview.dto;

import java.time.LocalDate;
import java.util.List;

public record StudyOverviewResponse(
        LocalDate date,
        OverviewTaskSummaryResponse taskSummary,
        OverviewFocusSummaryResponse focusSummary,
        OverviewSyllabusSummaryResponse syllabusSummary,
        OverviewGoalSummaryResponse goals,
        OverviewScheduleItemResponse nextBlock,
        List<OverviewScheduleItemResponse> schedule,
        List<OverviewTaskResponse> overdueTasks,
        List<OverviewTaskResponse> upcomingTasks
) {
}
