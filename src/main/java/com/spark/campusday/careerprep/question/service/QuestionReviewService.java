package com.spark.campusday.careerprep.question.service;

import com.spark.campusday.careerprep.question.dto.QuestionReviewRequest;
import com.spark.campusday.careerprep.question.dto.QuestionReviewResponse;
import com.spark.campusday.careerprep.question.model.BcsQuestion;
import com.spark.campusday.careerprep.question.model.ContentRightsStatus;
import com.spark.campusday.careerprep.question.model.QuestionStatus;
import com.spark.campusday.careerprep.question.repository.BcsQuestionRepository;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QuestionReviewService {
    private static final Map<QuestionStatus, Set<QuestionStatus>> ALLOWED_TRANSITIONS = Map.of(
            QuestionStatus.NEEDS_REVIEW,
            EnumSet.of(QuestionStatus.VERIFIED, QuestionStatus.REJECTED),
            QuestionStatus.VERIFIED,
            EnumSet.of(QuestionStatus.PUBLISHED, QuestionStatus.NEEDS_REVIEW,
                    QuestionStatus.ARCHIVED),
            QuestionStatus.PUBLISHED,
            EnumSet.of(QuestionStatus.NEEDS_REVIEW, QuestionStatus.ARCHIVED),
            QuestionStatus.ARCHIVED,
            EnumSet.of(QuestionStatus.NEEDS_REVIEW, QuestionStatus.PUBLISHED),
            QuestionStatus.REJECTED,
            EnumSet.of(QuestionStatus.NEEDS_REVIEW));

    private final BcsQuestionRepository questions;
    private final QuestionEligibilityPolicy eligibilityPolicy;
    private final Validator validator;

    public QuestionReviewResponse review(String questionId, QuestionReviewRequest request) {
        BcsQuestion question = questions.findByQuestionId(questionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "BCS question not found: " + questionId));
        requireTransition(question.getStatus(), request.status());
        requirePublishableRights(request.status(), request.rightsStatus());

        question.setStatus(request.status());
        question.setRightsStatus(request.rightsStatus());
        question.setAnswerConfidence(request.answerConfidence());
        question.setLabelConfidence(request.labelConfidence());
        question.setReviewedBy(request.reviewerId().trim());
        question.setReviewedAt(Instant.now());
        eligibilityPolicy.apply(question);
        validate(question);
        return response(questions.save(question));
    }

    private void requireTransition(QuestionStatus current, QuestionStatus requested) {
        if (current == requested) {
            return;
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(requested)) {
            throw new InvalidBusinessStateException(
                    "Question cannot transition from " + current + " to " + requested);
        }
    }

    private void requirePublishableRights(QuestionStatus status,
                                          ContentRightsStatus rightsStatus) {
        if ((status == QuestionStatus.VERIFIED || status == QuestionStatus.PUBLISHED)
                && rightsStatus != ContentRightsStatus.CLEARED) {
            throw new InvalidBusinessStateException(
                    "Verified or published questions require cleared content rights");
        }
    }

    private void validate(BcsQuestion question) {
        var violations = validator.validate(question);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private QuestionReviewResponse response(BcsQuestion question) {
        return new QuestionReviewResponse(question.getQuestionId(), question.getStatus(),
                question.getRightsStatus(), question.getAnswerConfidence(),
                question.getLabelConfidence(), question.isTrainingEligible(),
                question.isPredictionEligible(), question.getReviewedBy(),
                question.getReviewedAt());
    }
}
