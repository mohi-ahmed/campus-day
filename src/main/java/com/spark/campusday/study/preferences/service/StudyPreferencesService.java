package com.spark.campusday.study.preferences.service;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.student.service.StudentService;
import com.spark.campusday.study.preferences.dto.StudyPreferencesRequest;
import com.spark.campusday.study.preferences.dto.StudyPreferencesResponse;
import com.spark.campusday.study.preferences.exception.InvalidStudyPreferencesException;
import com.spark.campusday.study.preferences.model.StudyPreferences;
import com.spark.campusday.study.preferences.repository.StudyPreferencesRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudyPreferencesService {
    private static final int DEFAULT_FOCUS_MINUTES = 25;
    private static final int DEFAULT_SHORT_BREAK_MINUTES = 5;
    private static final int DEFAULT_LONG_BREAK_MINUTES = 15;
    private static final int DEFAULT_DAILY_FOCUS_GOAL_MINUTES = 120;
    private static final int DEFAULT_WEEKLY_SESSION_GOAL = 8;
    private static final int DEFAULT_TOPIC_COMPLETION_GOAL = 12;
    private static final int DEFAULT_REMINDER_LEAD_MINUTES = 10;
    private static final int DEFAULT_DAILY_REMINDER_LIMIT = 5;

    private final StudyPreferencesRepository preferences;
    private final StudentService students;
    private final Validator validator;

    public StudyPreferencesResponse get(String studentId) {
        return getForOwner(ownerId(studentId));
    }

    /** Internal boundary for study services that already hold the Mongo student owner id. */
    public StudyPreferencesResponse getForOwner(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new InvalidRequestException("Student owner id is required");
        }
        StudyPreferences setting = preferences.findByStudentId(ownerId)
                .map(this::applyMissingReminderDefaults)
                .orElseGet(() -> saveDefault(ownerId));
        return response(setting);
    }

    public StudyPreferencesResponse update(String studentId, StudyPreferencesRequest request) {
        validateRequest(request);
        String ownerId = ownerId(studentId);
        StudyPreferences setting = preferences.findByStudentId(ownerId)
                .orElseGet(() -> defaultPreferences(ownerId));

        setting.setFocusMinutes(request.focusMinutes());
        setting.setShortBreakMinutes(request.shortBreakMinutes());
        setting.setLongBreakMinutes(request.longBreakMinutes());
        setting.setDailyFocusGoalMinutes(request.dailyFocusGoalMinutes());
        setting.setWeeklySessionGoal(request.weeklySessionGoal());
        setting.setTopicCompletionGoal(request.topicCompletionGoal());
        setting.setTaskReminders(request.taskReminders());
        setting.setRoutineReminders(request.routineReminders());
        setting.setExamReminders(request.examReminders());
        setting.setReminderLeadMinutes(request.reminderLeadMinutes());
        setting.setMorningDigest(request.morningDigest());
        setting.setQuietHoursEnabled(request.quietHoursEnabled());
        setting.setDailyReminderLimit(request.dailyReminderLimit());

        validate(setting);
        return response(preferences.save(setting));
    }

    public StudyPreferencesResponse reset(String studentId) {
        String ownerId = ownerId(studentId);
        StudyPreferences setting = preferences.findByStudentId(ownerId)
                .orElseGet(() -> defaultPreferences(ownerId));
        applyDefaults(setting);
        validate(setting);
        return response(preferences.save(setting));
    }

    private StudyPreferences saveDefault(String ownerId) {
        StudyPreferences setting = defaultPreferences(ownerId);
        validate(setting);
        try {
            return preferences.save(setting);
        } catch (DuplicateKeyException exception) {
            return preferences.findByStudentId(ownerId)
                    .map(this::applyMissingReminderDefaults)
                    .orElseThrow(() -> exception);
        }
    }

    private StudyPreferences defaultPreferences(String ownerId) {
        StudyPreferences setting = StudyPreferences.builder().studentId(ownerId).build();
        applyDefaults(setting);
        return setting;
    }

    private void applyDefaults(StudyPreferences setting) {
        setting.setFocusMinutes(DEFAULT_FOCUS_MINUTES);
        setting.setShortBreakMinutes(DEFAULT_SHORT_BREAK_MINUTES);
        setting.setLongBreakMinutes(DEFAULT_LONG_BREAK_MINUTES);
        setting.setDailyFocusGoalMinutes(DEFAULT_DAILY_FOCUS_GOAL_MINUTES);
        setting.setWeeklySessionGoal(DEFAULT_WEEKLY_SESSION_GOAL);
        setting.setTopicCompletionGoal(DEFAULT_TOPIC_COMPLETION_GOAL);
        setting.setTaskReminders(true);
        setting.setRoutineReminders(true);
        setting.setExamReminders(true);
        setting.setReminderLeadMinutes(DEFAULT_REMINDER_LEAD_MINUTES);
        setting.setMorningDigest(true);
        setting.setQuietHoursEnabled(true);
        setting.setDailyReminderLimit(DEFAULT_DAILY_REMINDER_LIMIT);
    }

    private StudyPreferences applyMissingReminderDefaults(StudyPreferences setting) {
        boolean changed = false;
        if (setting.getMorningDigest() == null) {
            setting.setMorningDigest(true);
            changed = true;
        }
        if (setting.getQuietHoursEnabled() == null) {
            setting.setQuietHoursEnabled(true);
            changed = true;
        }
        if (setting.getDailyReminderLimit() == null) {
            setting.setDailyReminderLimit(DEFAULT_DAILY_REMINDER_LIMIT);
            changed = true;
        }
        if (!changed) {
            return setting;
        }
        validate(setting);
        return preferences.save(setting);
    }

    private String ownerId(String studentId) {
        return students.getByStudentId(studentId).getId();
    }

    private void validateRequest(StudyPreferencesRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Study preferences request is required");
        }
        if (request.focusMinutes() < 5 || request.focusMinutes() > 180
                || request.shortBreakMinutes() < 1 || request.shortBreakMinutes() > 30
                || request.longBreakMinutes() < 5 || request.longBreakMinutes() > 60
                || request.dailyFocusGoalMinutes() < 1
                || request.dailyFocusGoalMinutes() > 1440
                || request.weeklySessionGoal() < 1 || request.weeklySessionGoal() > 100
                || request.topicCompletionGoal() < 1 || request.topicCompletionGoal() > 1000
                || request.reminderLeadMinutes() < 0
                || request.reminderLeadMinutes() > 1440
                || request.dailyReminderLimit() < 1
                || request.dailyReminderLimit() > 20) {
            throw new InvalidStudyPreferencesException(
                    "One or more study preference values are outside the supported range");
        }
        if (request.longBreakMinutes() <= request.shortBreakMinutes()) {
            throw new InvalidStudyPreferencesException(
                    "Long break must be longer than short break");
        }
    }

    private void validate(StudyPreferences setting) {
        var violations = validator.validate(setting);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private StudyPreferencesResponse response(StudyPreferences setting) {
        return new StudyPreferencesResponse(setting.getId(), setting.getFocusMinutes(),
                setting.getShortBreakMinutes(), setting.getLongBreakMinutes(),
                setting.getDailyFocusGoalMinutes(), setting.getWeeklySessionGoal(),
                setting.getTopicCompletionGoal(),
                setting.isTaskReminders(), setting.isRoutineReminders(),
                setting.isExamReminders(), setting.getReminderLeadMinutes(),
                Boolean.TRUE.equals(setting.getMorningDigest()),
                Boolean.TRUE.equals(setting.getQuietHoursEnabled()),
                setting.getDailyReminderLimit(),
                setting.getCreatedAt(), setting.getUpdatedAt());
    }
}
