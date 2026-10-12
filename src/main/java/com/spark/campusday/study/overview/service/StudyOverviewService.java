package com.spark.campusday.study.overview.service;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import com.spark.campusday.study.focus.model.FocusSession;
import com.spark.campusday.study.focus.repository.FocusSessionRepository;
import com.spark.campusday.study.overview.dto.OverviewFocusSummaryResponse;
import com.spark.campusday.study.overview.dto.OverviewGoalSummaryResponse;
import com.spark.campusday.study.overview.dto.OverviewScheduleItemResponse;
import com.spark.campusday.study.overview.dto.OverviewScheduleItemType;
import com.spark.campusday.study.overview.dto.OverviewSyllabusSummaryResponse;
import com.spark.campusday.study.overview.dto.OverviewTaskResponse;
import com.spark.campusday.study.overview.dto.OverviewTaskSummaryResponse;
import com.spark.campusday.study.overview.dto.StudyOverviewResponse;
import com.spark.campusday.study.preferences.dto.StudyPreferencesResponse;
import com.spark.campusday.study.preferences.service.StudyPreferencesService;
import com.spark.campusday.study.routine.model.StudyRoutineBlock;
import com.spark.campusday.study.routine.repository.StudyRoutineRepository;
import com.spark.campusday.study.syllabus.model.StudySyllabusTopic;
import com.spark.campusday.study.syllabus.model.SyllabusTopicStatus;
import com.spark.campusday.study.syllabus.repository.StudySyllabusTopicRepository;
import com.spark.campusday.study.task.model.StudyTask;
import com.spark.campusday.study.task.model.TaskStatus;
import com.spark.campusday.study.task.repository.StudyTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyOverviewService {
    private static final int TASK_PREVIEW_LIMIT = 4;

    private final StudyTaskRepository tasks;
    private final StudyRoutineRepository routines;
    private final FocusSessionRepository sessions;
    private final StudySyllabusTopicRepository syllabusTopics;
    private final StudyPreferencesService preferences;
    private final StudentService students;
    @Qualifier("studyClock")
    private final Clock clock;

    public StudyOverviewResponse getToday(String studentId) {
        return get(studentId, LocalDate.now(clock));
    }

    public StudyOverviewResponse get(String studentId, LocalDate date) {
        if (date == null) {
            throw new InvalidRequestException("Overview date is required");
        }
        Student student = students.getByStudentId(studentId);
        String ownerId = student.getId();

        List<StudyTask> allTasks = tasks.findByStudentIdOrderByDueDateAscDueTimeAsc(ownerId);
        List<StudyTask> datedTasks = tasks
                .findByStudentIdAndDueDateGreaterThanEqualAndDueDateLessThanEqualOrderByDueDateAscDueTimeAsc(
                        ownerId, date, date);
        List<StudyRoutineBlock> dayRoutines = routines
                .findByStudentIdAndActiveTrueAndDaysContainingOrderByStartTimeAsc(
                        ownerId, date.getDayOfWeek());

        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        List<FocusSession> weeklySessions = sessions
                .findByStudentIdAndFocusDateGreaterThanEqualAndFocusDateLessThanEqualOrderByStartedAtDesc(
                        ownerId, weekStart, weekEnd);
        List<StudySyllabusTopic> topics = syllabusTopics
                .findByStudentIdOrderBySyllabusSubjectIdAscDisplayOrderAscTitleAsc(ownerId);
        StudyPreferencesResponse setting = preferences.get(studentId);

        return new StudyOverviewResponse(
                date,
                taskSummary(allTasks, date),
                focusSummary(weeklySessions, date),
                syllabusSummary(topics),
                new OverviewGoalSummaryResponse(setting.dailyFocusGoalMinutes(),
                        setting.weeklySessionGoal(), setting.topicCompletionGoal()),
                nextBlock(dayRoutines, date),
                schedule(dayRoutines, datedTasks, date),
                overdueTasks(allTasks, date),
                upcomingTasks(allTasks, date)
        );
    }

    private OverviewTaskSummaryResponse taskSummary(List<StudyTask> allTasks, LocalDate date) {
        int completed = (int) allTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
        int pending = allTasks.size() - completed;
        int overdue = (int) allTasks.stream().filter(task -> isOverdue(task, date)).count();
        int scheduled = (int) allTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.PENDING && task.getDueTime() != null)
                .count();
        return new OverviewTaskSummaryResponse(
                allTasks.size(), completed, pending, overdue, scheduled);
    }

    private OverviewFocusSummaryResponse focusSummary(List<FocusSession> weeklySessions,
                                                       LocalDate date) {
        int weeklyMinutes = weeklySessions.stream()
                .mapToInt(FocusSession::getFocusedMinutes).sum();
        int todayMinutes = weeklySessions.stream()
                .filter(session -> session.getFocusDate().equals(date))
                .mapToInt(FocusSession::getFocusedMinutes).sum();
        return new OverviewFocusSummaryResponse(
                todayMinutes, weeklyMinutes, weeklySessions.size());
    }

    private OverviewSyllabusSummaryResponse syllabusSummary(
            List<StudySyllabusTopic> topics) {
        int completed = (int) topics.stream()
                .filter(topic -> topic.getStatus() == SyllabusTopicStatus.COMPLETED)
                .count();
        return new OverviewSyllabusSummaryResponse(completed, topics.size());
    }

    private OverviewScheduleItemResponse nextBlock(List<StudyRoutineBlock> blocks,
                                                   LocalDate date) {
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        if (date.isBefore(today)) {
            return null;
        }
        return blocks.stream()
                .filter(block -> date.isAfter(today) || !block.getEndTime().isBefore(now))
                .findFirst()
                .map(block -> routineItem(block, date, today, now))
                .orElse(null);
    }

    private List<OverviewScheduleItemResponse> schedule(List<StudyRoutineBlock> blocks,
                                                        List<StudyTask> datedTasks,
                                                        LocalDate date) {
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        List<OverviewScheduleItemResponse> items = new ArrayList<>();
        blocks.stream().map(block -> routineItem(block, date, today, now)).forEach(items::add);
        datedTasks.stream().map(this::taskItem).forEach(items::add);
        items.sort(Comparator
                .comparing(OverviewScheduleItemResponse::startTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(OverviewScheduleItemResponse::title));
        return List.copyOf(items);
    }

    private OverviewScheduleItemResponse routineItem(StudyRoutineBlock block, LocalDate date,
                                                      LocalDate today, LocalTime now) {
        boolean completed = date.isBefore(today)
                || (date.equals(today) && !block.getEndTime().isAfter(now));
        return new OverviewScheduleItemResponse(block.getId(), OverviewScheduleItemType.ROUTINE,
                block.getTopic(), block.getSubject(), block.getBlockType(), block.getStartTime(),
                block.getEndTime(), completed);
    }

    private OverviewScheduleItemResponse taskItem(StudyTask task) {
        return new OverviewScheduleItemResponse(task.getId(), OverviewScheduleItemType.TASK,
                task.getTitle(), task.getSubject(), task.getTaskType(), task.getDueTime(), null,
                task.getStatus() == TaskStatus.COMPLETED);
    }

    private List<OverviewTaskResponse> overdueTasks(List<StudyTask> allTasks, LocalDate date) {
        return allTasks.stream().filter(task -> isOverdue(task, date))
                .limit(TASK_PREVIEW_LIMIT).map(this::taskResponse).toList();
    }

    private List<OverviewTaskResponse> upcomingTasks(List<StudyTask> allTasks, LocalDate date) {
        return allTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.PENDING
                        && !task.getDueDate().isBefore(date))
                .limit(TASK_PREVIEW_LIMIT).map(this::taskResponse).toList();
    }

    private boolean isOverdue(StudyTask task, LocalDate date) {
        return task.getStatus() == TaskStatus.PENDING && task.getDueDate().isBefore(date);
    }

    private OverviewTaskResponse taskResponse(StudyTask task) {
        return new OverviewTaskResponse(task.getId(), task.getTitle(), task.getSubject(),
                task.getTaskType(), task.getPriority(), task.getDueDate(), task.getDueTime(),
                task.getEstimatedMinutes(), task.getStatus());
    }
}
