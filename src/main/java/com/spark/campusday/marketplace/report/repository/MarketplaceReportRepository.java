package com.spark.campusday.marketplace.report.repository;

import com.spark.campusday.marketplace.report.model.MarketplaceReport;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MarketplaceReportRepository extends MongoRepository<MarketplaceReport, String> {
}
