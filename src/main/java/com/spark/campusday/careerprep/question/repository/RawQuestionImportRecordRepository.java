package com.spark.campusday.careerprep.question.repository;

import com.spark.campusday.careerprep.question.model.RawQuestionImportRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RawQuestionImportRecordRepository
        extends MongoRepository<RawQuestionImportRecord, String> {
}
