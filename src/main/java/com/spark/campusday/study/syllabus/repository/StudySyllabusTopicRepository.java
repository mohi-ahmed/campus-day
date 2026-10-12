package com.spark.campusday.study.syllabus.repository;

import com.spark.campusday.study.syllabus.model.StudySyllabusTopic;
import com.spark.campusday.study.syllabus.model.SyllabusTopicStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudySyllabusTopicRepository
        extends MongoRepository<StudySyllabusTopic, String> {
    Optional<StudySyllabusTopic> findByIdAndStudentId(String id, String studentId);

    Optional<StudySyllabusTopic> findByIdAndSyllabusSubjectIdAndStudentId(
            String id, String syllabusSubjectId, String studentId);

    List<StudySyllabusTopic> findBySyllabusSubjectIdAndStudentIdOrderByDisplayOrderAscTitleAsc(
            String syllabusSubjectId, String studentId);

    List<StudySyllabusTopic> findByStudentIdOrderBySyllabusSubjectIdAscDisplayOrderAscTitleAsc(
            String studentId);

    List<StudySyllabusTopic> findByStudentIdAndStatusOrderByDisplayOrderAscTitleAsc(
            String studentId, SyllabusTopicStatus status);

    List<StudySyllabusTopic> findByStatusNotAndTargetDateIsNotNull(
            SyllabusTopicStatus status);

    @Query(
            value = "{ 'studentId': ?0, 'targetDate': { $gte: ?1, $lte: ?2 } }",
            sort = "{ 'targetDate': 1, 'displayOrder': 1 }")
    List<StudySyllabusTopic> findByStudentIdAndTargetDateGreaterThanEqualAndTargetDateLessThanEqualOrderByTargetDateAscDisplayOrderAsc(
            String studentId, LocalDate from, LocalDate to);

    boolean existsBySyllabusSubjectIdAndTitleKey(
            String syllabusSubjectId, String titleKey);

    boolean existsBySyllabusSubjectIdAndTitleKeyAndIdNot(
            String syllabusSubjectId, String titleKey, String excludedTopicId);

    long deleteBySyllabusSubjectIdAndStudentId(String syllabusSubjectId, String studentId);
}
