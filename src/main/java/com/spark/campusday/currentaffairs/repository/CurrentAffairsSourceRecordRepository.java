package com.spark.campusday.currentaffairs.repository;

import com.spark.campusday.currentaffairs.model.CurrentAffairsProvider;
import com.spark.campusday.currentaffairs.model.CurrentAffairsSourceRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface CurrentAffairsSourceRecordRepository
        extends MongoRepository<CurrentAffairsSourceRecord, String> {
    List<CurrentAffairsSourceRecord> findAllByProviderAndExternalArticleIdIn(
            CurrentAffairsProvider provider,
            Collection<String> externalArticleIds);
}
