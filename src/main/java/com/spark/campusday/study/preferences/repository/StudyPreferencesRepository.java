package com.spark.campusday.study.preferences.repository;

import com.spark.campusday.study.preferences.model.StudyPreferences;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface StudyPreferencesRepository extends MongoRepository<StudyPreferences, String> {
    Optional<StudyPreferences> findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);
}
