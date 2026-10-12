package com.spark.campusday.study.insights.dto;

import java.util.List;

public record StudyInsightsResponse(
        InsightsPeriodResponse period,
        FocusInsightsResponse focus,
        TaskInsightsResponse tasks,
        SyllabusInsightsResponse syllabus,
        HabitInsightsResponse habits,
        RoutineInsightsResponse routine,
        InsightsGoalProgressResponse goals,
        List<DailyStudyTrendResponse> dailyTrend,
        List<SubjectPerformanceResponse> subjects
) {
}
