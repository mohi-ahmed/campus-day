package com.spark.campusday.study.habit.repository;

import com.spark.campusday.study.habit.model.StudyHabit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface StudyHabitRepository extends MongoRepository<StudyHabit, String> {
    Optional<StudyHabit> findByIdAndStudentId(String id, String studentId);

    List<StudyHabit> findByStudentIdOrderByDisplayOrderAscNameAsc(String studentId);

    List<StudyHabit> findByStudentIdAndActiveTrueOrderByDisplayOrderAscNameAsc(String studentId);

    boolean existsByStudentIdAndNameKey(String studentId, String nameKey);

    boolean existsByStudentIdAndNameKeyAndIdNot(
            String studentId, String nameKey, String excludedHabitId);
}
