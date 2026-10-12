package com.spark.campusday.study.reminder.service;

import com.spark.campusday.study.preferences.dto.StudyPreferencesResponse;
import com.spark.campusday.study.preferences.service.StudyPreferencesService;
import com.spark.campusday.study.reminder.dto.StudyReminderScheduleRequest;
import com.spark.campusday.study.reminder.model.StudyReminderType;
import com.spark.campusday.study.routine.model.StudyRoutineBlock;
import com.spark.campusday.study.syllabus.model.StudySyllabusTopic;
import com.spark.campusday.study.syllabus.model.SyllabusTopicStatus;
import com.spark.campusday.study.syllabus.repository.StudySyllabusSubjectRepository;
import com.spark.campusday.study.task.model.StudyTask;
import com.spark.campusday.study.task.model.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudyReminderSchedulingService {
    private static final LocalTime MORNING_DIGEST_TIME = LocalTime.of(7, 30);
    private static final LocalTime SYLLABUS_PREVIEW_TIME = LocalTime.of(9, 0);
    private static final LocalTime SYLLABUS_MORNING_TIME = LocalTime.of(7, 30);
    private static final LocalTime QUIET_HOURS_START = LocalTime.of(23, 0);
    private static final LocalTime QUIET_HOURS_END = LocalTime.of(6, 0);

    private final StudyReminderService reminders;
    private final StudyPreferencesService preferences;
    private final StudySyllabusSubjectRepository syllabusSubjects;
    @Qualifier("studyClock")
    private final Clock clock;

    public void syncTask(StudyTask task) {
        StudyPreferencesResponse setting = preferences.getForOwner(task.getStudentId());
        List<StudyReminderScheduleRequest> desired = List.of();
        if (setting.taskReminders() && task.getStatus() == TaskStatus.PENDING
                && task.getDueTime() != null) {
            Instant scheduledAt = reminderTime(
                    task.getDueDate(), task.getDueTime(), setting.quietHoursEnabled());
            desired = List.of(request(StudyReminderType.TASK_DEADLINE, task.getId(),
                    task.getTitle(), task.getSubject() + " · " + task.getTaskType(),
                    "/study", scheduledAt));
        }
        reminders.replaceScheduledForOwner(task.getStudentId(),
                StudyReminderType.TASK_DEADLINE, task.getId(), desired);
    }

    public void syncRoutine(StudyRoutineBlock block) {
        StudyPreferencesResponse setting = preferences.getForOwner(block.getStudentId());
        List<StudyReminderScheduleRequest> desired = List.of();
        if (setting.routineReminders() && block.isActive() && isSelfStudy(block)) {
            Instant scheduledAt = nextRoutineTime(block, setting.reminderLeadMinutes(),
                    setting.quietHoursEnabled());
            desired = List.of(request(StudyReminderType.ROUTINE_BLOCK, block.getId(),
                    block.getTopic(), block.getSubject() + " · " + block.getBlockType(),
                    "/study", scheduledAt));
        }
        reminders.replaceScheduledForOwner(block.getStudentId(),
                StudyReminderType.ROUTINE_BLOCK, block.getId(), desired);
    }

    public void syncSyllabusTopic(StudySyllabusTopic topic) {
        StudyPreferencesResponse setting = preferences.getForOwner(topic.getStudentId());
        var subject = syllabusSubjects.findById(topic.getSyllabusSubjectId());
        List<StudyReminderScheduleRequest> desired = List.of();
        if (setting.examReminders() && topic.getTargetDate() != null
                && topic.getStatus() != SyllabusTopicStatus.COMPLETED
                && subject.map(value -> value.isActive()).orElse(false)) {
            String subjectLabel = subject
                    .map(value -> value.getName() + " · syllabus target")
                    .orElse("Syllabus target");
            desired = List.of(
                    request(StudyReminderType.SYLLABUS_TARGET, topic.getId(),
                            topic.getTitle(), subjectLabel, "/study",
                            reminderTime(topic.getTargetDate().minusDays(1),
                                    SYLLABUS_PREVIEW_TIME, setting.quietHoursEnabled())),
                    request(StudyReminderType.SYLLABUS_TARGET, topic.getId(),
                            topic.getTitle(), subjectLabel, "/study",
                            reminderTime(topic.getTargetDate(), SYLLABUS_MORNING_TIME,
                                    setting.quietHoursEnabled())));
        }
        reminders.replaceScheduledForOwner(topic.getStudentId(),
                StudyReminderType.SYLLABUS_TARGET, topic.getId(), desired);
    }

    public void syncMorningDigest(String ownerId) {
        StudyPreferencesResponse setting = preferences.getForOwner(ownerId);
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDate digestDate = now.toLocalTime().isBefore(MORNING_DIGEST_TIME)
                ? now.toLocalDate() : now.toLocalDate().plusDays(1);
        List<StudyReminderScheduleRequest> desired = setting.morningDigest()
                ? List.of(request(StudyReminderType.MORNING_DIGEST,
                        digestDate.toString(), "Today's study plan",
                        "Review your tasks, routine and syllabus targets.",
                        "/study", reminderTime(digestDate, MORNING_DIGEST_TIME, false)))
                : List.of();
        reminders.replaceScheduledForOwner(ownerId, StudyReminderType.MORNING_DIGEST,
                digestDate.toString(), desired);
    }

    public void cancelTask(StudyTask task) {
        reminders.cancelScheduledForSourceByOwner(task.getStudentId(),
                StudyReminderType.TASK_DEADLINE, task.getId());
    }

    public void cancelRoutine(StudyRoutineBlock block) {
        reminders.cancelScheduledForSourceByOwner(block.getStudentId(),
                StudyReminderType.ROUTINE_BLOCK, block.getId());
    }

    public void cancelSyllabusTopic(StudySyllabusTopic topic) {
        reminders.cancelScheduledForSourceByOwner(topic.getStudentId(),
                StudyReminderType.SYLLABUS_TARGET, topic.getId());
    }

    public void syncTaskSafely(StudyTask task) {
        safely("task", task.getId(), () -> syncTask(task));
    }

    public void syncRoutineSafely(StudyRoutineBlock block) {
        safely("routine", block.getId(), () -> syncRoutine(block));
    }

    public void syncSyllabusTopicSafely(StudySyllabusTopic topic) {
        safely("syllabus topic", topic.getId(), () -> syncSyllabusTopic(topic));
    }

    public void cancelTaskSafely(StudyTask task) {
        safely("task", task.getId(), () -> cancelTask(task));
    }

    public void cancelRoutineSafely(StudyRoutineBlock block) {
        safely("routine", block.getId(), () -> cancelRoutine(block));
    }

    public void cancelSyllabusTopicSafely(StudySyllabusTopic topic) {
        safely("syllabus topic", topic.getId(), () -> cancelSyllabusTopic(topic));
    }

    private Instant nextRoutineTime(StudyRoutineBlock block, int leadMinutes,
                                    boolean quietHoursEnabled) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        return block.getDays().stream()
                .map(day -> nextOccurrence(now, day, block.getStartTime(), leadMinutes,
                        quietHoursEnabled))
                .min(Comparator.naturalOrder())
                .orElseThrow()
                .toInstant();
    }

    private ZonedDateTime nextOccurrence(ZonedDateTime now, DayOfWeek day,
                                         LocalTime startTime, int leadMinutes,
                                         boolean quietHoursEnabled) {
        LocalDate date = now.toLocalDate().with(TemporalAdjusters.nextOrSame(day));
        ZonedDateTime candidate = date.atTime(startTime).atZone(clock.getZone())
                .minusMinutes(leadMinutes);
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusWeeks(1);
        }
        return respectQuietHours(candidate, quietHoursEnabled);
    }

    private Instant reminderTime(LocalDate date, LocalTime time,
                                 boolean quietHoursEnabled) {
        return respectQuietHours(date.atTime(time).atZone(clock.getZone()), quietHoursEnabled)
                .toInstant();
    }

    private ZonedDateTime respectQuietHours(ZonedDateTime value, boolean enabled) {
        if (!enabled) {
            return value;
        }
        LocalTime time = value.toLocalTime();
        if (!time.isBefore(QUIET_HOURS_START)) {
            return value.plusDays(1).with(QUIET_HOURS_END);
        }
        if (time.isBefore(QUIET_HOURS_END)) {
            return value.with(QUIET_HOURS_END);
        }
        return value;
    }

    private boolean isSelfStudy(StudyRoutineBlock block) {
        String type = block.getBlockType().trim().toLowerCase(java.util.Locale.ROOT);
        return type.contains("study") || type.contains("revision");
    }

    private StudyReminderScheduleRequest request(
            StudyReminderType type, String sourceId, String title, String message,
            String actionPath, Instant scheduledAt) {
        return new StudyReminderScheduleRequest(
                type, sourceId, title, message, actionPath, scheduledAt);
    }

    private void safely(String sourceType, String sourceId, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            log.error("Could not synchronize study reminder for {} {}",
                    sourceType, sourceId, exception);
        }
    }
}
