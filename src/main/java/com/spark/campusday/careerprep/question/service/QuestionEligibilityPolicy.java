package com.spark.campusday.careerprep.question.service;

import com.spark.campusday.careerprep.question.model.BcsQuestion;
import com.spark.campusday.careerprep.question.model.ContentRightsStatus;
import com.spark.campusday.careerprep.question.model.QuestionSourceType;
import com.spark.campusday.careerprep.question.model.QuestionStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class QuestionEligibilityPolicy {
    private static final double MIN_TRAINING_CONFIDENCE = 0.80;
    private static final double MIN_PREDICTION_CONFIDENCE = 0.90;
    private static final Set<QuestionStatus> APPROVED_STATUSES =
            EnumSet.of(QuestionStatus.VERIFIED, QuestionStatus.PUBLISHED);
    private static final Set<QuestionSourceType> PREDICTION_SOURCES =
            EnumSet.of(QuestionSourceType.PREVIOUS_BCS, QuestionSourceType.OFFICIAL_BPSC);

    public void apply(BcsQuestion question) {
        boolean approved = APPROVED_STATUSES.contains(question.getStatus());
        boolean rightsCleared = question.getRightsStatus() == ContentRightsStatus.CLEARED;
        boolean reliableEnough = atLeast(question.getAnswerConfidence(), MIN_TRAINING_CONFIDENCE)
                && atLeast(question.getLabelConfidence(), MIN_TRAINING_CONFIDENCE);
        boolean humanOrLicensedSource = question.getSource().type()
                != QuestionSourceType.AI_GENERATED;

        boolean trainingEligible = approved && rightsCleared && reliableEnough
                && humanOrLicensedSource;
        question.setTrainingEligible(trainingEligible);

        boolean predictionEligible = trainingEligible
                && PREDICTION_SOURCES.contains(question.getSource().type())
                && question.getExamNumber() != null
                && question.getQuestionNumber() != null
                && atLeast(question.getAnswerConfidence(), MIN_PREDICTION_CONFIDENCE)
                && atLeast(question.getLabelConfidence(), MIN_PREDICTION_CONFIDENCE);
        question.setPredictionEligible(predictionEligible);
    }

    private boolean atLeast(Double value, double minimum) {
        return value != null && value >= minimum;
    }
}
