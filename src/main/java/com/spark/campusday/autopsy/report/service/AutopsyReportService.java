package com.spark.campusday.autopsy.report.service;

import com.spark.campusday.autopsy.post.service.AutopsyPostService;
import com.spark.campusday.autopsy.report.dto.ReportRequest;
import com.spark.campusday.autopsy.report.dto.ReportResponse;
import com.spark.campusday.autopsy.report.model.AutopsyReport;
import com.spark.campusday.autopsy.report.repository.AutopsyReportRepository;
import com.spark.campusday.autopsy.support.AutopsyInput;
import com.spark.campusday.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutopsyReportService {
    private final AutopsyReportRepository reports;
    private final AutopsyPostService posts;

    public ReportResponse create(String postId, ReportRequest request) {
        posts.requireActive(postId);
        AutopsyReport report = AutopsyReport.builder()
                .postId(postId)
                .actorHash(AutopsyInput.actorHash(request.sessionId()))
                .reason(request.reason())
                .details(AutopsyInput.optional(request.details()))
                .build();
        try {
            AutopsyReport saved = reports.save(report);
            return new ReportResponse(
                    saved.getId(),
                    saved.getPostId(),
                    saved.getStatus(),
                    saved.getCreatedAt());
        } catch (DuplicateKeyException exception) {
            throw new ConflictException(
                    "This post has already been reported from this anonymous session",
                    exception);
        }
    }
}
