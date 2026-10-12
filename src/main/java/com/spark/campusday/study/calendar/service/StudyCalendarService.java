package com.spark.campusday.study.calendar.service;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.student.service.StudentService;
import com.spark.campusday.study.calendar.dto.CalendarDayResponse;
import com.spark.campusday.study.calendar.dto.CalendarDaySummaryResponse;
import com.spark.campusday.study.calendar.dto.CalendarEventResponse;
import com.spark.campusday.study.calendar.dto.CalendarEventType;
import com.spark.campusday.study.calendar.dto.CalendarMonthResponse;
import com.spark.campusday.study.calendar.dto.CalendarRangeResponse;
import com.spark.campusday.study.focus.model.FocusSession;
import com.spark.campusday.study.focus.model.FocusSessionOutcome;
import com.spark.campusday.study.focus.repository.FocusSessionRepository;
import com.spark.campusday.study.habit.model.StudyHabit;
import com.spark.campusday.study.habit.model.StudyHabitCompletion;
import com.spark.campusday.study.habit.repository.StudyHabitCompletionRepository;
import com.spark.campusday.study.habit.repository.StudyHabitRepository;
import com.spark.campusday.study.routine.model.StudyRoutineBlock;
import com.spark.campusday.study.routine.repository.StudyRoutineRepository;
import com.spark.campusday.study.syllabus.model.StudySyllabusSubject;
import com.spark.campusday.study.syllabus.model.StudySyllabusTopic;
import com.spark.campusday.study.syllabus.model.SyllabusTopicStatus;
import com.spark.campusday.study.syllabus.repository.StudySyllabusSubjectRepository;
import com.spark.campusday.study.syllabus.repository.StudySyllabusTopicRepository;
import com.spark.campusday.study.task.model.StudyTask;
import com.spark.campusday.study.task.model.TaskStatus;
import com.spark.campusday.study.task.repository.StudyTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyCalendarService {
    private static final int MAX_RANGE_DAYS = 366;

    private static final Comparator<CalendarEventResponse> EVENT_ORDER = Comparator
            .comparing(CalendarEventResponse::startTime,
                    Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(CalendarEventResponse::title, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(CalendarEventResponse::id);

    private final StudyTaskRepository tasks;
    private final StudyRoutineRepository routines;
    private final StudyHabitRepository habits;
    private final StudyHabitCompletionRepository habitCompletions;
    private final StudySyllabusSubjectRepository syllabusSubjects;
    private final StudySyllabusTopicRepository syllabusTopics;
    private final FocusSessionRepository focusSessions;
    private final StudentService students;
    @Qualifier("studyClock")
    private final Clock clock;

    public CalendarDayResponse getDay(String studentId, LocalDate date) {
        requireDate(date);
        return getRange(studentId, date, date).days().getFirst();
    }

    public CalendarMonthResponse getMonth(String studentId, int year, int month) {
        YearMonth yearMonth = requireYearMonth(year, month);
        CalendarRangeResponse range = getRange(
                studentId, yearMonth.atDay(1), yearMonth.atEndOfMonth());
        return new CalendarMonthResponse(year, month,
                range.days().stream().map(CalendarDayResponse::summary).toList());
    }

    public CalendarRangeResponse getRange(String studentId, LocalDate startDate,
                                          LocalDate endDate) {
        validateRange(startDate, endDate);
        String ownerId = students.getByStudentId(studentId).getId();
        Map<LocalDate, List<CalendarEventResponse>> eventsByDate = dateBuckets(
                startDate, endDate);

        addTasks(eventsByDate, ownerId, startDate, endDate);
        addRoutines(eventsByDate, ownerId);
        addHabits(eventsByDate, ownerId, startDate, endDate);
        addSyllabusTopics(eventsByDate, ownerId, startDate, endDate);
        addFocusSessions(eventsByDate, ownerId, startDate, endDate);

        LocalDate today = LocalDate.now(clock);
        List<CalendarDayResponse> days = eventsByDate.entrySet().stream()
                .map(entry -> dayResponse(entry.getKey(), entry.getValue(), today))
                .toList();
        return new CalendarRangeResponse(startDate, endDate, days);
    }

    private void addTasks(Map<LocalDate, List<CalendarEventResponse>> eventsByDate,
                          String ownerId, LocalDate startDate, LocalDate endDate) {
        tasks.findByStudentIdAndDueDateGreaterThanEqualAndDueDateLessThanEqualOrderByDueDateAscDueTimeAsc(
                        ownerId, startDate, endDate)
                .forEach(task -> add(eventsByDate, task.getDueDate(), taskEvent(task)));
    }

    private void addRoutines(Map<LocalDate, List<CalendarEventResponse>> eventsByDate,
                             String ownerId) {
        List<StudyRoutineBlock> activeRoutines =
                routines.findByStudentIdAndActiveTrueOrderByStartTimeAsc(ownerId);
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);

        eventsByDate.forEach((date, events) -> activeRoutines.stream()
                .filter(block -> block.getDays().contains(date.getDayOfWeek()))
                .map(block -> routineEvent(block, date, today, now))
                .forEach(events::add));
    }

    private void addHabits(Map<LocalDate, List<CalendarEventResponse>> eventsByDate,
                           String ownerId, LocalDate startDate, LocalDate endDate) {
        List<StudyHabit> activeHabits =
                habits.findByStudentIdAndActiveTrueOrderByDisplayOrderAscNameAsc(ownerId);
        Set<HabitCompletionKey> completed = habitCompletions
                .findByStudentIdAndCompletedOnGreaterThanEqualAndCompletedOnLessThanEqualOrderByCompletedOnAsc(
                        ownerId, startDate, endDate)
                .stream()
                .map(this::completionKey)
                .collect(Collectors.toSet());

        eventsByDate.forEach((date, events) -> activeHabits.stream()
                .filter(habit -> habit.getRepeatDays().contains(date.getDayOfWeek()))
                .map(habit -> habitEvent(habit, date,
                        completed.contains(new HabitCompletionKey(habit.getId(), date))))
                .forEach(events::add));
    }

    private void addSyllabusTopics(
            Map<LocalDate, List<CalendarEventResponse>> eventsByDate,
            String ownerId, LocalDate startDate, LocalDate endDate) {
        Map<String, String> subjectNames = syllabusSubjects
                .findByStudentIdOrderByDisplayOrderAscNameAsc(ownerId)
                .stream()
                .collect(Collectors.toMap(StudySyllabusSubject::getId,
                        StudySyllabusSubject::getName, (first, ignored) -> first));

        syllabusTopics.findByStudentIdAndTargetDateGreaterThanEqualAndTargetDateLessThanEqualOrderByTargetDateAscDisplayOrderAsc(
                        ownerId, startDate, endDate)
                .forEach(topic -> add(eventsByDate, topic.getTargetDate(),
                        syllabusEvent(topic, subjectNames.get(topic.getSyllabusSubjectId()))));
    }

    private void addFocusSessions(
            Map<LocalDate, List<CalendarEventResponse>> eventsByDate,
            String ownerId, LocalDate startDate, LocalDate endDate) {
        focusSessions.findByStudentIdAndFocusDateGreaterThanEqualAndFocusDateLessThanEqualOrderByStartedAtDesc(
                        ownerId, startDate, endDate)
                .forEach(session -> add(eventsByDate, session.getFocusDate(),
                        focusEvent(session)));
    }

    private CalendarEventResponse taskEvent(StudyTask task) {
        boolean completed = task.getStatus() == TaskStatus.COMPLETED;
        return new CalendarEventResponse(task.getId(), CalendarEventType.TASK,
                task.getTitle(), task.getSubject(), task.getTaskType(), task.getNote(),
                task.getDueDate(), task.getDueTime(), null, task.getDueTime() == null,
                completed, task.getStatus().name(), task.getPriority().name());
    }

    private CalendarEventResponse routineEvent(StudyRoutineBlock block, LocalDate date,
                                                LocalDate today, LocalTime now) {
        boolean completed = date.isBefore(today)
                || (date.equals(today) && !block.getEndTime().isAfter(now));
        return new CalendarEventResponse(block.getId(), CalendarEventType.ROUTINE,
                block.getTopic(), block.getSubject(), block.getBlockType(),
                block.getLocation(), date, block.getStartTime(), block.getEndTime(), false,
                completed, "ACTIVE", null);
    }

    private CalendarEventResponse habitEvent(StudyHabit habit, LocalDate date,
                                              boolean completed) {
        return new CalendarEventResponse(habit.getId(), CalendarEventType.HABIT,
                habit.getName(), null, "HABIT", habit.getDescription(), date,
                null, null, true, completed, completed ? "COMPLETED" : "PENDING", null);
    }

    private CalendarEventResponse syllabusEvent(StudySyllabusTopic topic,
                                                 String subjectName) {
        boolean completed = topic.getStatus() == SyllabusTopicStatus.COMPLETED;
        return new CalendarEventResponse(topic.getId(), CalendarEventType.SYLLABUS_TOPIC,
                topic.getTitle(), subjectName, "SYLLABUS_TOPIC", topic.getNote(),
                topic.getTargetDate(), null, null, true, completed,
                topic.getStatus().name(), null);
    }

    private CalendarEventResponse focusEvent(FocusSession session) {
        return new CalendarEventResponse(session.getId(), CalendarEventType.FOCUS_SESSION,
                session.getTaskTitle(), session.getSubject(), "FOCUS_SESSION",
                session.getFocusedMinutes() + " focused minutes", session.getFocusDate(),
                LocalTime.ofInstant(session.getStartedAt(), clock.getZone()),
                LocalTime.ofInstant(session.getEndedAt(), clock.getZone()), false,
                session.getOutcome() == FocusSessionOutcome.COMPLETED,
                session.getOutcome().name(), null);
    }

    private CalendarDayResponse dayResponse(LocalDate date,
                                            List<CalendarEventResponse> mutableEvents,
                                            LocalDate today) {
        List<CalendarEventResponse> events = mutableEvents.stream().sorted(EVENT_ORDER).toList();
        int completed = (int) events.stream().filter(CalendarEventResponse::completed).count();
        int overdueTasks = (int) events.stream()
                .filter(event -> event.type() == CalendarEventType.TASK
                        && !event.completed() && date.isBefore(today))
                .count();
        CalendarDaySummaryResponse summary = new CalendarDaySummaryResponse(
                date, events.size(), completed, events.size() - completed, overdueTasks);
        return new CalendarDayResponse(date, summary, events);
    }

    private Map<LocalDate, List<CalendarEventResponse>> dateBuckets(
            LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, List<CalendarEventResponse>> buckets = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            buckets.put(date, new ArrayList<>());
        }
        return buckets;
    }

    private void add(Map<LocalDate, List<CalendarEventResponse>> eventsByDate,
                     LocalDate date, CalendarEventResponse event) {
        List<CalendarEventResponse> events = eventsByDate.get(date);
        if (events != null) {
            events.add(event);
        }
    }

    private HabitCompletionKey completionKey(StudyHabitCompletion completion) {
        return new HabitCompletionKey(completion.getHabitId(), completion.getCompletedOn());
    }

    private void requireDate(LocalDate date) {
        if (date == null) {
            throw new InvalidRequestException("Calendar date is required");
        }
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidRequestException("Calendar start and end dates are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new InvalidRequestException(
                    "Calendar end date must not be before start date");
        }
        long requestedDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (requestedDays > MAX_RANGE_DAYS) {
            throw new InvalidRequestException(
                    "Calendar range cannot exceed " + MAX_RANGE_DAYS + " days");
        }
    }

    private YearMonth requireYearMonth(int year, int month) {
        try {
            return YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new InvalidRequestException("Calendar year or month is invalid");
        }
    }

    private record HabitCompletionKey(String habitId, LocalDate completedOn) {
    }
}
