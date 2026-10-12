package com.spark.campusday.careerprep.question.dto;

import com.spark.campusday.careerprep.question.model.ContentRightsStatus;
import com.spark.campusday.careerprep.question.model.QuestionStatus;

import java.time.Instant;

public record QuestionReviewResponse(
        String questionId,
        QuestionStatus status,
        ContentRightsStatus rightsStatus,
        Double answerConfidence,
        Double labelConfidence,
        boolean trainingEligible,
        boolean predictionEligible,
        String reviewedBy,
        Instant reviewedAt) {
}
