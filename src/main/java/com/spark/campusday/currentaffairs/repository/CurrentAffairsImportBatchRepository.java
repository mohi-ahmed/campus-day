package com.spark.campusday.currentaffairs.repository;

import com.spark.campusday.currentaffairs.model.CurrentAffairsImportBatch;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CurrentAffairsImportBatchRepository
        extends MongoRepository<CurrentAffairsImportBatch, String> {
}
