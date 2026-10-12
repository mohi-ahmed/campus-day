package com.spark.campusday.study.calendar.integration.service;

import com.spark.campusday.student.service.StudentService;
import com.spark.campusday.study.calendar.integration.dto.CalendarSyncRequest;
import com.spark.campusday.study.calendar.integration.model.CalendarIntegrationProvider;
import com.spark.campusday.study.calendar.integration.repository.StudyCalendarConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyCalendarAutoSyncService {
    private static final int SYNC_HORIZON_DAYS = 365;

    private final StudyCalendarIntegrationService integrations;
    private final StudyCalendarConnectionRepository connections;
    private final StudentService students;
    @Qualifier("studyCalendarSyncExecutor")
    private final TaskExecutor syncExecutor;
    @Qualifier("studyClock")
    private final Clock clock;

    public void syncDatesSafely(String studentId, LocalDate... dates) {
        LocalDate[] distinctDates = Arrays.stream(dates)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toArray(LocalDate[]::new);
        if (distinctDates.length == 0) {
            return;
        }
        submit(() -> Arrays.stream(distinctDates)
                .forEach(date -> syncRangeSafely(studentId, date, date)));
    }

    public void syncHorizonSafely(String studentId) {
        LocalDate today = LocalDate.now(clock);
        submit(() -> syncRangeSafely(
                studentId, today, today.plusDays(SYNC_HORIZON_DAYS)));
    }

    @Scheduled(
            fixedDelayString = "${app.study.calendar.integration.auto-sync-fixed-delay:PT15M}",
            initialDelayString = "${app.study.calendar.integration.auto-sync-initial-delay:PT30S}")
    public void reconcileConnectedCalendars() {
        connections.findByProvider(CalendarIntegrationProvider.GOOGLE)
                .forEach(connection -> submit(() -> {
                    String studentId = students.getById(connection.getStudentId()).getStudentId();
                    LocalDate today = LocalDate.now(clock);
                    syncRangeSafely(studentId, today,
                            today.plusDays(SYNC_HORIZON_DAYS));
                }));
    }

    private void submit(Runnable syncWork) {
        try {
            syncExecutor.execute(syncWork);
        } catch (RuntimeException exception) {
            log.warn("Google Calendar automatic sync could not be queued", exception);
        }
    }

    private void syncRangeSafely(String studentId, LocalDate from, LocalDate to) {
        try {
            integrations.syncGoogleIfConnected(
                    studentId, new CalendarSyncRequest(from, to));
        } catch (RuntimeException exception) {
            log.warn("Google Calendar automatic sync failed for student {}", studentId,
                    exception);
        }
    }
}
