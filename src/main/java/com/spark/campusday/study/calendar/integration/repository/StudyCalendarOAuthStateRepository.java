package com.spark.campusday.study.calendar.integration.repository;

import com.spark.campusday.study.calendar.integration.model.StudyCalendarOAuthState;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface StudyCalendarOAuthStateRepository
        extends MongoRepository<StudyCalendarOAuthState, String> {
    Optional<StudyCalendarOAuthState> findByState(String state);
}
