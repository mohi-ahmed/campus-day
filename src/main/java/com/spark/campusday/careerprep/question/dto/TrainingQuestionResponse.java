package com.spark.campusday.careerprep.question.dto;

import com.spark.campusday.careerprep.question.model.BcsExamType;
import com.spark.campusday.careerprep.question.model.QuestionDifficulty;
import com.spark.campusday.careerprep.question.model.QuestionKind;
import com.spark.campusday.careerprep.question.model.QuestionLanguage;
import com.spark.campusday.careerprep.question.model.QuestionOption;
import com.spark.campusday.careerprep.question.model.QuestionSourceType;

import java.time.LocalDate;
import java.util.List;

public record TrainingQuestionResponse(
        String id,
        String questionId,
        String fingerprint,
        BcsExamType examType,
        Integer examNumber,
        Integer examLabelYear,
        LocalDate preliminaryHeldDate,
        String examSetCode,
        Integer questionNumber,
        QuestionLanguage language,
        String subjectCode,
        String topicCode,
        String subTopicCode,
        List<String> conceptCodes,
        QuestionKind questionKind,
        String questionText,
        List<QuestionOption> options,
        String correctOptionKey,
        String explanation,
        QuestionDifficulty difficulty,
        List<String> tags,
        boolean timeSensitive,
        QuestionSourceType sourceType,
        Double answerConfidence,
        Double labelConfidence,
        boolean predictionEligible,
        int schemaVersion) {
}
