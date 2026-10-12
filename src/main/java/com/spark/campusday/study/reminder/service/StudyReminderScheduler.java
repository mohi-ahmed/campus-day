package com.spark.campusday.study.reminder.service;

import com.spark.campusday.student.repository.StudentRepository;
import com.spark.campusday.study.routine.repository.StudyRoutineRepository;
import com.spark.campusday.study.syllabus.model.SyllabusTopicStatus;
import com.spark.campusday.study.syllabus.repository.StudySyllabusTopicRepository;
import com.spark.campusday.study.task.model.TaskStatus;
import com.spark.campusday.study.task.repository.StudyTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudyReminderScheduler {
    private static final int DELIVERY_BATCH_SIZE = 100;

    private final StudyReminderService reminders;
    private final StudyReminderSchedulingService scheduling;
    private final StudyTaskRepository tasks;
    private final StudyRoutineRepository routines;
    private final StudySyllabusTopicRepository syllabusTopics;
    private final StudentRepository students;

    @Scheduled(
            initialDelayString = "${app.study.reminders.delivery-initial-delay-ms:10000}",
            fixedDelayString = "${app.study.reminders.delivery-delay-ms:30000}")
    public void deliverDueReminders() {
        try {
            reminders.deliverDue(DELIVERY_BATCH_SIZE);
        } catch (RuntimeException exception) {
            log.error("Study reminder delivery cycle failed", exception);
        }
    }

    @Scheduled(
            initialDelayString = "${app.study.reminders.planning-initial-delay-ms:5000}",
            fixedDelayString = "${app.study.reminders.planning-delay-ms:60000}")
    public void reconcileReminderPlans() {
        tasks.findByStatus(TaskStatus.PENDING)
                .forEach(task -> safely("task", task.getId(), () -> scheduling.syncTask(task)));
        routines.findByActiveTrue()
                .forEach(block -> safely("routine", block.getId(),
                        () -> scheduling.syncRoutine(block)));
        syllabusTopics.findByStatusNotAndTargetDateIsNotNull(SyllabusTopicStatus.COMPLETED)
                .forEach(topic -> safely("syllabus topic", topic.getId(),
                        () -> scheduling.syncSyllabusTopic(topic)));
        students.findAll().forEach(student -> safely("morning digest", student.getId(),
                () -> scheduling.syncMorningDigest(student.getId())));
    }

    private void safely(String sourceType, String sourceId, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            log.error("Could not reconcile study reminder for {} {}",
                    sourceType, sourceId, exception);
        }
    }
}
