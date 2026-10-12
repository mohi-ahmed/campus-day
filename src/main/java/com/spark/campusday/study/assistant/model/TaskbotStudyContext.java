package com.spark.campusday.study.assistant.model;

import com.spark.campusday.study.calendar.dto.CalendarRangeResponse;
import com.spark.campusday.study.habit.dto.StudyHabitProgressResponse;
import com.spark.campusday.study.insights.dto.StudyInsightsResponse;
import com.spark.campusday.study.overview.dto.StudyOverviewResponse;
import com.spark.campusday.study.routine.dto.StudyRoutineResponse;
import com.spark.campusday.study.syllabus.dto.SyllabusSubjectProgressResponse;
import com.spark.campusday.study.task.dto.StudyTaskResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record TaskbotStudyContext(
        LocalDate currentDate,
        LocalTime currentTime,
        String timeZone,
        StudyOverviewResponse overview,
        StudyInsightsResponse weeklyInsights,
        CalendarRangeResponse upcomingCalendar,
        List<StudyTaskResponse> pendingTasks,
        List<StudyRoutineResponse> activeRoutines,
        List<StudyHabitProgressResponse> activeHabits,
        List<SyllabusSubjectProgressResponse> syllabus
) {
}
