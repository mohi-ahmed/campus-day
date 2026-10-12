package com.spark.campusday.careerprep.question.dto;

import com.spark.campusday.careerprep.question.model.BcsExamType;
import com.spark.campusday.careerprep.question.model.QuestionDifficulty;
import com.spark.campusday.careerprep.question.model.QuestionKind;
import com.spark.campusday.careerprep.question.model.QuestionLanguage;
import com.spark.campusday.careerprep.question.model.QuestionOption;

import java.util.List;

public record QuestionDeliveryResponse(
        String questionId,
        BcsExamType examType,
        Integer examNumber,
        Integer questionNumber,
        QuestionLanguage language,
        String subjectCode,
        String topicCode,
        String subTopicCode,
        List<String> conceptCodes,
        QuestionKind questionKind,
        String questionText,
        List<QuestionOption> options,
        QuestionDifficulty difficulty,
        List<String> questionImageUrls) {
}
