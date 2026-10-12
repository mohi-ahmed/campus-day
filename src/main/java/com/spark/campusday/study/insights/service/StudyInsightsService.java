package com.spark.campusday.study.insights.service;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.student.service.StudentService;
import com.spark.campusday.study.focus.model.FocusSession;
import com.spark.campusday.study.focus.model.FocusSessionOutcome;
import com.spark.campusday.study.focus.repository.FocusSessionRepository;
import com.spark.campusday.study.habit.model.StudyHabit;
import com.spark.campusday.study.habit.model.StudyHabitCompletion;
import com.spark.campusday.study.habit.repository.StudyHabitCompletionRepository;
import com.spark.campusday.study.habit.repository.StudyHabitRepository;
import com.spark.campusday.study.insights.dto.DailyStudyTrendResponse;
import com.spark.campusday.study.insights.dto.FocusInsightsResponse;
import com.spark.campusday.study.insights.dto.HabitInsightsResponse;
import com.spark.campusday.study.insights.dto.HabitPerformanceResponse;
import com.spark.campusday.study.insights.dto.InsightsGoalProgressResponse;
import com.spark.campusday.study.insights.dto.InsightsPeriodResponse;
import com.spark.campusday.study.insights.dto.RoutineInsightsResponse;
import com.spark.campusday.study.insights.dto.StudyInsightsResponse;
import com.spark.campusday.study.insights.dto.SubjectPerformanceResponse;
import com.spark.campusday.study.insights.dto.SyllabusInsightsResponse;
import com.spark.campusday.study.insights.dto.TaskBreakdownResponse;
import com.spark.campusday.study.insights.dto.TaskInsightsResponse;
import com.spark.campusday.study.preferences.dto.StudyPreferencesResponse;
import com.spark.campusday.study.preferences.service.StudyPreferencesService;
import com.spark.campusday.study.routine.model.StudyRoutineBlock;
import com.spark.campusday.study.routine.repository.StudyRoutineRepository;
import com.spark.campusday.study.syllabus.model.StudySyllabusSubject;
import com.spark.campusday.study.syllabus.model.StudySyllabusTopic;
import com.spark.campusday.study.syllabus.model.SyllabusTopicStatus;
import com.spark.campusday.study.syllabus.repository.StudySyllabusSubjectRepository;
import com.spark.campusday.study.syllabus.repository.StudySyllabusTopicRepository;
import com.spark.campusday.study.task.model.StudyTask;
import com.spark.campusday.study.task.model.TaskPriority;
import com.spark.campusday.study.task.model.TaskStatus;
import com.spark.campusday.study.task.repository.StudyTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyInsightsService {
    private static final int MAX_RANGE_DAYS = 366;

    private final StudyTaskRepository tasks;
    private final StudyRoutineRepository routines;
    private final FocusSessionRepository focusSessions;
    private final StudyHabitRepository habits;
    private final StudyHabitCompletionRepository habitCompletions;
    private final StudySyllabusSubjectRepository syllabusSubjects;
    private final StudySyllabusTopicRepository syllabusTopics;
    private final StudyPreferencesService preferences;
    private final StudentService students;
    @Qualifier("studyClock")
    private final Clock clock;

    public StudyInsightsResponse getWeek(String studentId, LocalDate date) {
        requireDate(date);
        LocalDate from = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return getSummary(studentId, from, from.plusDays(6));
    }

    public StudyInsightsResponse getMonth(String studentId, int year, int month) {
        YearMonth yearMonth = requireYearMonth(year, month);
        return getSummary(studentId, yearMonth.atDay(1), yearMonth.atEndOfMonth());
    }

    public StudyInsightsResponse getSummary(String studentId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        String ownerId = students.getByStudentId(studentId).getId();
        LocalDate today = LocalDate.now(clock);

        List<StudyTask> allTasks = tasks.findByStudentIdOrderByDueDateAscDueTimeAsc(ownerId);
        List<StudyTask> periodTasks = allTasks.stream()
                .filter(task -> isWithin(task.getDueDate(), from, to))
                .toList();
        List<StudyRoutineBlock> activeRoutines = routines
                .findByStudentIdAndActiveTrueOrderByStartTimeAsc(ownerId);
        List<FocusSession> periodSessions = focusSessions
                .findByStudentIdAndFocusDateGreaterThanEqualAndFocusDateLessThanEqualOrderByStartedAtDesc(
                        ownerId, from, to);
        List<StudyHabit> activeHabits = habits
                .findByStudentIdAndActiveTrueOrderByDisplayOrderAscNameAsc(ownerId);
        List<StudyHabitCompletion> periodHabitCompletions = habitCompletions
                .findByStudentIdAndCompletedOnGreaterThanEqualAndCompletedOnLessThanEqualOrderByCompletedOnAsc(
                        ownerId, from, to);
        List<StudySyllabusSubject> subjects = syllabusSubjects
                .findByStudentIdAndActiveTrueOrderByDisplayOrderAscNameAsc(ownerId);
        Set<String> activeSubjectIds = subjects.stream()
                .map(StudySyllabusSubject::getId)
                .collect(Collectors.toSet());
        List<StudySyllabusTopic> topics = syllabusTopics
                .findByStudentIdOrderBySyllabusSubjectIdAscDisplayOrderAscTitleAsc(ownerId)
                .stream()
                .filter(topic -> activeSubjectIds.contains(topic.getSyllabusSubjectId()))
                .toList();
        StudyPreferencesResponse setting = preferences.get(studentId);

        Map<String, Set<LocalDate>> habitDates = completionDates(periodHabitCompletions);
        return new StudyInsightsResponse(
                new InsightsPeriodResponse(from, to, inclusiveDays(from, to)),
                focusInsights(periodSessions, from, to, today),
                taskInsights(periodTasks, today),
                syllabusInsights(topics, today),
                habitInsights(activeHabits, habitDates, from, to, today),
                routineInsights(activeRoutines),
                goalProgress(setting, periodSessions, topics, from, to, today),
                dailyTrend(allTasks, activeRoutines, periodSessions,
                        periodHabitCompletions, from, to),
                subjectPerformance(subjects, topics, periodTasks,
                        activeRoutines, periodSessions, from, to)
        );
    }

    private FocusInsightsResponse focusInsights(List<FocusSession> sessions,
                                                 LocalDate from, LocalDate to,
                                                 LocalDate today) {
        int totalMinutes = sessions.stream().mapToInt(FocusSession::getFocusedMinutes).sum();
        int completed = (int) sessions.stream()
                .filter(session -> session.getOutcome() == FocusSessionOutcome.COMPLETED)
                .count();
        int stopped = (int) sessions.stream()
                .filter(session -> session.getOutcome() == FocusSessionOutcome.STOPPED)
                .count();
        Set<LocalDate> activeDates = sessions.stream()
                .filter(session -> session.getFocusedMinutes() > 0)
                .map(FocusSession::getFocusDate)
                .collect(Collectors.toSet());
        return new FocusInsightsResponse(totalMinutes, sessions.size(), completed, stopped,
                average(totalMinutes, sessions.size()), activeDates.size(),
                currentCalendarStreak(activeDates, from, to, today),
                longestCalendarStreak(activeDates, from, to));
    }

    private TaskInsightsResponse taskInsights(List<StudyTask> periodTasks,
                                               LocalDate today) {
        int completed = countTasks(periodTasks,
                task -> task.getStatus() == TaskStatus.COMPLETED);
        int overdue = countTasks(periodTasks, task -> isOverdue(task, today));
        int scheduled = countTasks(periodTasks,
                task -> task.getStatus() == TaskStatus.PENDING && task.getDueTime() != null);

        List<TaskBreakdownResponse> byPriority = Arrays.stream(TaskPriority.values())
                .map(priority -> breakdown(priority.name(), periodTasks.stream()
                        .filter(task -> task.getPriority() == priority).toList()))
                .toList();

        Map<String, List<StudyTask>> typeGroups = periodTasks.stream()
                .collect(Collectors.groupingBy(task -> key(task.getTaskType())));
        List<TaskBreakdownResponse> byType = typeGroups.values().stream()
                .map(group -> breakdown(group.getFirst().getTaskType(), group))
                .sorted((left, right) -> String.CASE_INSENSITIVE_ORDER
                        .compare(left.label(), right.label()))
                .toList();

        return new TaskInsightsResponse(periodTasks.size(), completed,
                periodTasks.size() - completed, overdue, scheduled,
                percentage(completed, periodTasks.size()), byPriority, byType);
    }

    private TaskBreakdownResponse breakdown(String label, List<StudyTask> group) {
        int completed = countTasks(group, task -> task.getStatus() == TaskStatus.COMPLETED);
        return new TaskBreakdownResponse(label, group.size(), completed,
                group.size() - completed, percentage(completed, group.size()));
    }

    private SyllabusInsightsResponse syllabusInsights(List<StudySyllabusTopic> topics,
                                                       LocalDate today) {
        int completed = countTopics(topics, SyllabusTopicStatus.COMPLETED);
        int revision = countTopics(topics, SyllabusTopicStatus.REVISION);
        int learning = countTopics(topics, SyllabusTopicStatus.LEARNING);
        int notStarted = countTopics(topics, SyllabusTopicStatus.NOT_STARTED);
        int overdue = (int) topics.stream()
                .filter(topic -> topic.getStatus() != SyllabusTopicStatus.COMPLETED)
                .filter(topic -> topic.getTargetDate() != null
                        && topic.getTargetDate().isBefore(today))
                .count();
        return new SyllabusInsightsResponse(topics.size(), completed, revision, learning,
                notStarted, overdue, percentage(completed, topics.size()));
    }

    private HabitInsightsResponse habitInsights(List<StudyHabit> activeHabits,
                                                 Map<String, Set<LocalDate>> completionDates,
                                                 LocalDate from, LocalDate to,
                                                 LocalDate today) {
        List<HabitPerformanceResponse> performance = activeHabits.stream()
                .map(habit -> habitPerformance(habit,
                        completionDates.getOrDefault(habit.getId(), Set.of()),
                        from, to, today))
                .toList();
        int scheduled = performance.stream()
                .mapToInt(HabitPerformanceResponse::scheduledDays).sum();
        int completed = performance.stream()
                .mapToInt(HabitPerformanceResponse::completedDays).sum();
        return new HabitInsightsResponse(activeHabits.size(), scheduled, completed,
                percentage(completed, scheduled), performance);
    }

    private HabitPerformanceResponse habitPerformance(StudyHabit habit,
                                                       Set<LocalDate> completionDates,
                                                       LocalDate from, LocalDate to,
                                                       LocalDate today) {
        List<LocalDate> scheduledDates = dates(from, to).stream()
                .filter(date -> habit.getRepeatDays().contains(date.getDayOfWeek()))
                .toList();
        Set<LocalDate> validCompletions = scheduledDates.stream()
                .filter(completionDates::contains)
                .collect(Collectors.toSet());
        return new HabitPerformanceResponse(habit.getId(), habit.getName(),
                scheduledDates.size(), validCompletions.size(),
                percentage(validCompletions.size(), scheduledDates.size()),
                currentScheduledStreak(scheduledDates, validCompletions, today),
                longestScheduledStreak(scheduledDates, validCompletions));
    }

    private RoutineInsightsResponse routineInsights(List<StudyRoutineBlock> activeRoutines) {
        int classBlocks = 0;
        int weeklyClassMinutes = 0;
        int weeklySelfStudyMinutes = 0;
        for (StudyRoutineBlock block : activeRoutines) {
            int weeklyMinutes = blockMinutes(block) * block.getDays().size();
            if (isClass(block)) {
                classBlocks++;
                weeklyClassMinutes += weeklyMinutes;
            } else {
                weeklySelfStudyMinutes += weeklyMinutes;
            }
        }
        return new RoutineInsightsResponse(activeRoutines.size(), classBlocks,
                activeRoutines.size() - classBlocks,
                weeklyClassMinutes + weeklySelfStudyMinutes,
                weeklyClassMinutes, weeklySelfStudyMinutes);
    }

    private InsightsGoalProgressResponse goalProgress(StudyPreferencesResponse setting,
                                                       List<FocusSession> periodSessions,
                                                       List<StudySyllabusTopic> topics,
                                                       LocalDate from, LocalDate to,
                                                       LocalDate today) {
        int focusedToday = isWithin(today, from, to)
                ? periodSessions.stream()
                .filter(session -> session.getFocusDate().equals(today))
                .mapToInt(FocusSession::getFocusedMinutes).sum()
                : 0;
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        int sessionsThisWeek = (int) periodSessions.stream()
                .filter(session -> isWithin(session.getFocusDate(), weekStart, weekEnd))
                .count();
        int completedTopics = countTopics(topics, SyllabusTopicStatus.COMPLETED);
        return new InsightsGoalProgressResponse(setting.dailyFocusGoalMinutes(), focusedToday,
                setting.weeklySessionGoal(), sessionsThisWeek,
                setting.topicCompletionGoal(), completedTopics);
    }

    private List<DailyStudyTrendResponse> dailyTrend(
            List<StudyTask> allTasks,
            List<StudyRoutineBlock> activeRoutines,
            List<FocusSession> periodSessions,
            List<StudyHabitCompletion> periodHabitCompletions,
            LocalDate from, LocalDate to) {
        Map<LocalDate, Integer> focusedMinutes = periodSessions.stream()
                .collect(Collectors.groupingBy(FocusSession::getFocusDate,
                        Collectors.summingInt(FocusSession::getFocusedMinutes)));
        Map<LocalDate, Long> completedTasks = allTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .filter(task -> task.getCompletedAt() != null)
                .collect(Collectors.groupingBy(
                        task -> LocalDate.ofInstant(task.getCompletedAt(), clock.getZone()),
                        Collectors.counting()));
        Map<LocalDate, Long> completedHabits = periodHabitCompletions.stream()
                .collect(Collectors.groupingBy(StudyHabitCompletion::getCompletedOn,
                        Collectors.counting()));

        return dates(from, to).stream()
                .map(date -> new DailyStudyTrendResponse(date,
                        plannedSelfStudyMinutes(activeRoutines, date),
                        focusedMinutes.getOrDefault(date, 0),
                        completedTasks.getOrDefault(date, 0L).intValue(),
                        completedHabits.getOrDefault(date, 0L).intValue()))
                .toList();
    }

    private List<SubjectPerformanceResponse> subjectPerformance(
            List<StudySyllabusSubject> syllabusSubjectList,
            List<StudySyllabusTopic> topics,
            List<StudyTask> periodTasks,
            List<StudyRoutineBlock> activeRoutines,
            List<FocusSession> periodSessions,
            LocalDate from, LocalDate to) {
        Map<String, SubjectAccumulator> totals = new LinkedHashMap<>();
        Map<String, String> subjectNamesById = new HashMap<>();

        syllabusSubjectList.forEach(subject -> {
            subjectNamesById.put(subject.getId(), subject.getName());
            subject(totals, subject.getName()).prefer(subject.getName(), subject.getColor());
        });
        topics.forEach(topic -> {
            String subjectName = subjectNamesById.get(topic.getSyllabusSubjectId());
            if (subjectName != null) {
                SubjectAccumulator total = subject(totals, subjectName);
                total.totalTopics++;
                if (topic.getStatus() == SyllabusTopicStatus.COMPLETED) {
                    total.completedTopics++;
                }
            }
        });
        periodTasks.forEach(task -> {
            SubjectAccumulator total = subject(totals, task.getSubject());
            total.totalTasks++;
            if (task.getStatus() == TaskStatus.COMPLETED) {
                total.completedTasks++;
            }
        });
        periodSessions.forEach(session ->
                subject(totals, session.getSubject()).focusedMinutes += session.getFocusedMinutes());
        dates(from, to).forEach(date -> activeRoutines.stream()
                .filter(block -> !isClass(block))
                .filter(block -> block.getDays().contains(date.getDayOfWeek()))
                .forEach(block -> subject(totals, block.getSubject()).plannedMinutes +=
                        blockMinutes(block)));

        return totals.values().stream()
                .map(SubjectAccumulator::response)
                .sorted((left, right) -> String.CASE_INSENSITIVE_ORDER
                        .compare(left.subject(), right.subject()))
                .toList();
    }

    private SubjectAccumulator subject(Map<String, SubjectAccumulator> totals,
                                       String subjectName) {
        String cleanedName = subjectName == null || subjectName.isBlank()
                ? "Unspecified" : subjectName.trim();
        return totals.computeIfAbsent(key(cleanedName), ignored ->
                new SubjectAccumulator(cleanedName));
    }

    private Map<String, Set<LocalDate>> completionDates(
            List<StudyHabitCompletion> completions) {
        Map<String, Set<LocalDate>> datesByHabit = new HashMap<>();
        completions.forEach(completion -> datesByHabit
                .computeIfAbsent(completion.getHabitId(), ignored -> new HashSet<>())
                .add(completion.getCompletedOn()));
        return datesByHabit;
    }

    private int plannedSelfStudyMinutes(List<StudyRoutineBlock> activeRoutines,
                                        LocalDate date) {
        return activeRoutines.stream()
                .filter(block -> !isClass(block))
                .filter(block -> block.getDays().contains(date.getDayOfWeek()))
                .mapToInt(this::blockMinutes)
                .sum();
    }

    private int blockMinutes(StudyRoutineBlock block) {
        return Math.toIntExact(ChronoUnit.MINUTES.between(
                block.getStartTime(), block.getEndTime()));
    }

    private boolean isClass(StudyRoutineBlock block) {
        return key(block.getBlockType()).contains("class");
    }

    private boolean isOverdue(StudyTask task, LocalDate today) {
        return task.getStatus() == TaskStatus.PENDING && task.getDueDate().isBefore(today);
    }

    private int countTasks(List<StudyTask> taskList, Predicate<StudyTask> predicate) {
        return (int) taskList.stream().filter(predicate).count();
    }

    private int countTopics(List<StudySyllabusTopic> topics, SyllabusTopicStatus status) {
        return (int) topics.stream().filter(topic -> topic.getStatus() == status).count();
    }

    private int currentCalendarStreak(Set<LocalDate> activeDates,
                                      LocalDate from, LocalDate to,
                                      LocalDate today) {
        LocalDate anchor = today.isBefore(to) ? today : to;
        if (anchor.isBefore(from)) {
            return 0;
        }
        int streak = 0;
        for (LocalDate date = anchor;
             !date.isBefore(from) && activeDates.contains(date);
             date = date.minusDays(1)) {
            streak++;
        }
        return streak;
    }

    private int longestCalendarStreak(Set<LocalDate> activeDates,
                                      LocalDate from, LocalDate to) {
        int current = 0;
        int longest = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            current = activeDates.contains(date) ? current + 1 : 0;
            longest = Math.max(longest, current);
        }
        return longest;
    }

    private int currentScheduledStreak(List<LocalDate> scheduledDates,
                                       Set<LocalDate> completedDates,
                                       LocalDate today) {
        int streak = 0;
        for (int index = scheduledDates.size() - 1; index >= 0; index--) {
            LocalDate date = scheduledDates.get(index);
            if (date.isAfter(today)) {
                continue;
            }
            if (!completedDates.contains(date)) {
                break;
            }
            streak++;
        }
        return streak;
    }

    private int longestScheduledStreak(List<LocalDate> scheduledDates,
                                       Set<LocalDate> completedDates) {
        int current = 0;
        int longest = 0;
        for (LocalDate date : scheduledDates) {
            current = completedDates.contains(date) ? current + 1 : 0;
            longest = Math.max(longest, current);
        }
        return longest;
    }

    private List<LocalDate> dates(LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>(inclusiveDays(from, to));
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            dates.add(date);
        }
        return dates;
    }

    private int inclusiveDays(LocalDate from, LocalDate to) {
        return Math.toIntExact(ChronoUnit.DAYS.between(from, to) + 1);
    }

    private boolean isWithin(LocalDate value, LocalDate from, LocalDate to) {
        return value != null && !value.isBefore(from) && !value.isAfter(to);
    }

    private double percentage(int completed, int total) {
        return total == 0 ? 0.0 : round(completed * 100.0 / total);
    }

    private double average(int total, int count) {
        return count == 0 ? 0.0 : round((double) total / count);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String key(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void requireDate(LocalDate date) {
        if (date == null) {
            throw new InvalidRequestException("Insights date is required");
        }
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new InvalidRequestException("Insights start and end dates are required");
        }
        if (to.isBefore(from)) {
            throw new InvalidRequestException(
                    "Insights end date must not be before start date");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
            throw new InvalidRequestException(
                    "Insights range cannot exceed " + MAX_RANGE_DAYS + " days");
        }
    }

    private YearMonth requireYearMonth(int year, int month) {
        try {
            return YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new InvalidRequestException("Insights year or month is invalid");
        }
    }

    private final class SubjectAccumulator {
        private String name;
        private String color;
        private int focusedMinutes;
        private int plannedMinutes;
        private int completedTasks;
        private int totalTasks;
        private int completedTopics;
        private int totalTopics;

        private SubjectAccumulator(String name) {
            this.name = name;
        }

        private void prefer(String preferredName, String preferredColor) {
            name = preferredName;
            color = preferredColor;
        }

        private SubjectPerformanceResponse response() {
            return new SubjectPerformanceResponse(name, color, focusedMinutes,
                    plannedMinutes, completedTasks, totalTasks, completedTopics, totalTopics,
                    percentage(completedTasks, totalTasks),
                    percentage(completedTopics, totalTopics));
        }
    }
}
