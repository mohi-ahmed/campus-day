package com.spark.campusday.study.task.repository;

import com.spark.campusday.study.task.model.StudyTask;
import com.spark.campusday.study.task.model.TaskStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudyTaskRepository extends MongoRepository<StudyTask, String> {
    Optional<StudyTask> findByIdAndStudentId(String id, String studentId);

    List<StudyTask> findByStudentIdOrderByDueDateAscDueTimeAsc(String studentId);

    List<StudyTask> findByStudentIdAndStatusOrderByDueDateAscDueTimeAsc(
            String studentId, TaskStatus status);

    List<StudyTask> findByStatus(TaskStatus status);

    @Query(
            value = "{ 'studentId': ?0, 'dueDate': { $gte: ?1, $lte: ?2 } }",
            sort = "{ 'dueDate': 1, 'dueTime': 1 }")
    List<StudyTask> findByStudentIdAndDueDateGreaterThanEqualAndDueDateLessThanEqualOrderByDueDateAscDueTimeAsc(
            String studentId, LocalDate from, LocalDate to);

    boolean existsByIdAndStudentId(String id, String studentId);
}
