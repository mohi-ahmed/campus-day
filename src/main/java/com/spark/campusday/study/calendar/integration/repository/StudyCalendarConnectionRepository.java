package com.spark.campusday.study.calendar.integration.repository;

import com.spark.campusday.study.calendar.integration.model.CalendarIntegrationProvider;
import com.spark.campusday.study.calendar.integration.model.StudyCalendarConnection;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface StudyCalendarConnectionRepository
        extends MongoRepository<StudyCalendarConnection, String> {
    Optional<StudyCalendarConnection> findByStudentIdAndProvider(
            String studentId, CalendarIntegrationProvider provider);

    List<StudyCalendarConnection> findByProvider(
            CalendarIntegrationProvider provider);
}
