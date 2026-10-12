package com.spark.campusday.autopsy.report.repository;

import com.spark.campusday.autopsy.report.model.AutopsyReport;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AutopsyReportRepository extends MongoRepository<AutopsyReport, String> {
}
