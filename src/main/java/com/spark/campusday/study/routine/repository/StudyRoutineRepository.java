package com.spark.campusday.study.routine.repository;

import com.spark.campusday.study.routine.model.StudyRoutineBlock;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface StudyRoutineRepository extends MongoRepository<StudyRoutineBlock, String> {
    Optional<StudyRoutineBlock> findByIdAndStudentId(String id, String studentId);

    List<StudyRoutineBlock> findByStudentIdOrderByStartTimeAsc(String studentId);

    List<StudyRoutineBlock> findByStudentIdAndActiveTrueOrderByStartTimeAsc(String studentId);

    List<StudyRoutineBlock> findByActiveTrue();

    List<StudyRoutineBlock> findByStudentIdAndActiveTrueAndDaysContainingOrderByStartTimeAsc(
            String studentId, DayOfWeek day);

    boolean existsByIdAndStudentId(String id, String studentId);
}
