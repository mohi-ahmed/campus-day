package com.spark.campusday.study.assistant.service;

import com.spark.campusday.study.assistant.config.TaskbotProperties;
import com.spark.campusday.study.assistant.exception.InvalidTaskbotResponseException;
import com.spark.campusday.study.assistant.model.TaskbotStudyContext;
import com.spark.campusday.study.calendar.service.StudyCalendarService;
import com.spark.campusday.study.habit.service.StudyHabitService;
import com.spark.campusday.study.insights.service.StudyInsightsService;
import com.spark.campusday.study.overview.service.StudyOverviewService;
import com.spark.campusday.study.routine.service.StudyRoutineService;
import com.spark.campusday.study.syllabus.dto.SyllabusSubjectProgressResponse;
import com.spark.campusday.study.syllabus.service.SyllabusSubjectService;
import com.spark.campusday.study.task.model.TaskStatus;
import com.spark.campusday.study.task.service.StudyTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class TaskbotStudyContextService {
    private final StudyOverviewService overview;
    private final StudyInsightsService insights;
    private final StudyCalendarService calendar;
    private final StudyTaskService tasks;
    private final StudyRoutineService routines;
    private final StudyHabitService habits;
    private final SyllabusSubjectService syllabusSubjects;
    private final TaskbotProperties properties;
    private final ObjectMapper objectMapper;
    @Qualifier("studyClock")
    private final Clock clock;

    public String context(String studentId) {
        LocalDate today = LocalDate.now(clock);
        int taskLimit = Math.max(1, properties.maxContextTasks());
        int topicLimit = Math.max(1, properties.maxTopicsPerSubject());

        TaskbotStudyContext context = new TaskbotStudyContext(
                today,
                LocalTime.now(clock),
                clock.getZone().getId(),
                overview.get(studentId, today),
                insights.getWeek(studentId, today),
                calendar.getRange(studentId, today, today.plusDays(6)),
                tasks.list(studentId).stream()
                        .filter(task -> task.status() == TaskStatus.PENDING)
                        .limit(taskLimit)
                        .toList(),
                routines.list(studentId, true).stream()
                        .limit(taskLimit)
                        .toList(),
                habits.progress(studentId, today, today.plusDays(6), true),
                syllabusSubjects.progressList(studentId, true).stream()
                        .map(subject -> limitedTopics(subject, topicLimit))
                        .toList());
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JacksonException exception) {
            throw new InvalidTaskbotResponseException(
                    "Taskbot could not prepare the study context");
        }
    }

    private SyllabusSubjectProgressResponse limitedTopics(
            SyllabusSubjectProgressResponse progress, int topicLimit) {
        return new SyllabusSubjectProgressResponse(
                progress.subject(),
                progress.completedTopics(),
                progress.totalTopics(),
                progress.topics().stream().limit(topicLimit).toList());
    }
}
