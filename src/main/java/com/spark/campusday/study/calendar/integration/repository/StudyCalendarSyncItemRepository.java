package com.spark.campusday.study.calendar.integration.repository;

import com.spark.campusday.study.calendar.integration.model.StudyCalendarSyncItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface StudyCalendarSyncItemRepository
        extends MongoRepository<StudyCalendarSyncItem, String> {
    @Query("{ 'connectionId': ?0, 'eventDate': { $gte: ?1, $lte: ?2 } }")
    List<StudyCalendarSyncItem> findByConnectionIdAndEventDateGreaterThanEqualAndEventDateLessThanEqual(
            String connectionId, LocalDate from, LocalDate to);

    List<StudyCalendarSyncItem> findByConnectionId(String connectionId);

    void deleteAllByConnectionId(String connectionId);
}
