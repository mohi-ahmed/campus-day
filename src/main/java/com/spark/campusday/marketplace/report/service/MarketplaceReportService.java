package com.spark.campusday.marketplace.report.service;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.marketplace.listing.service.MarketplaceListingService;
import com.spark.campusday.marketplace.report.dto.MarketplaceReportRequest;
import com.spark.campusday.marketplace.report.dto.MarketplaceReportResponse;
import com.spark.campusday.marketplace.report.model.MarketplaceReport;
import com.spark.campusday.marketplace.report.model.MarketplaceReportTarget;
import com.spark.campusday.marketplace.report.repository.MarketplaceReportRepository;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketplaceReportService {
    private final MarketplaceReportRepository reports;
    private final MarketplaceListingService listings;
    private final StudentService students;

    public MarketplaceReportResponse create(MarketplaceReportRequest request) {
        String reporterStudentId = request.reporterStudentId().trim();
        students.getByStudentId(reporterStudentId);
        if (request.targetType() == MarketplaceReportTarget.LISTING) {
            listings.get(request.targetId().trim());
        } else {
            students.getByStudentId(request.targetId().trim());
        }
        MarketplaceReport report = MarketplaceReport.builder()
                .reporterStudentId(reporterStudentId)
                .targetType(request.targetType())
                .targetId(request.targetId().trim())
                .reason(request.reason())
                .details(optional(request.details()))
                .build();
        try {
            return response(reports.save(report));
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("You already reported this marketplace target", exception);
        }
    }

    private static MarketplaceReportResponse response(MarketplaceReport report) {
        return new MarketplaceReportResponse(
                report.getId(), report.getReporterStudentId(), report.getTargetType(), report.getTargetId(),
                report.getReason(), report.getDetails(), report.getStatus(), report.getCreatedAt());
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
