package com.spark.campusday.study.syllabus.repository;

import com.spark.campusday.study.syllabus.model.StudySyllabusSubject;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface StudySyllabusSubjectRepository
        extends MongoRepository<StudySyllabusSubject, String> {
    Optional<StudySyllabusSubject> findByIdAndStudentId(String id, String studentId);

    List<StudySyllabusSubject> findByStudentIdOrderByDisplayOrderAscNameAsc(String studentId);

    List<StudySyllabusSubject> findByStudentIdAndActiveTrueOrderByDisplayOrderAscNameAsc(
            String studentId);

    boolean existsByStudentIdAndNameKey(String studentId, String nameKey);

    boolean existsByStudentIdAndNameKeyAndIdNot(
            String studentId, String nameKey, String excludedSubjectId);
}
