package com.spark.campusday.careerprep.question.service;

import com.spark.campusday.careerprep.question.dto.CursorPage;
import com.spark.campusday.careerprep.question.dto.QuestionDeliveryResponse;
import com.spark.campusday.careerprep.question.dto.TrainingQuestionResponse;
import com.spark.campusday.careerprep.question.model.BcsExamType;
import com.spark.campusday.careerprep.question.model.BcsQuestion;
import com.spark.campusday.careerprep.question.model.QuestionDifficulty;
import com.spark.campusday.careerprep.question.model.QuestionStatus;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionQueryService {
    private static final int MAX_PUBLIC_PAGE_SIZE = 100;
    private static final int MAX_TRAINING_PAGE_SIZE = 1000;

    private final MongoTemplate mongo;
    private final QuestionNormalizationService normalization;

    public CursorPage<QuestionDeliveryResponse> findPublished(
            String subjectCode,
            String topicCode,
            BcsExamType examType,
            Integer examNumber,
            QuestionDifficulty difficulty,
            String afterId,
            int limit) {
        int pageSize = validLimit(limit, MAX_PUBLIC_PAGE_SIZE);
        if ((subjectCode == null || subjectCode.isBlank()) && examNumber == null) {
            throw new InvalidRequestException(
                    "subjectCode or examNumber is required for an indexed question query");
        }
        if (topicCode != null && !topicCode.isBlank()
                && (subjectCode == null || subjectCode.isBlank())) {
            throw new InvalidRequestException("topicCode requires subjectCode");
        }
        if (examNumber != null && examType == null) {
            throw new InvalidRequestException("examType is required with examNumber");
        }

        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("status").is(QuestionStatus.PUBLISHED));
        if (subjectCode != null && !subjectCode.isBlank()) {
            criteria.add(Criteria.where("subjectCode")
                    .is(normalization.normalizeFilterCode(subjectCode)));
        }
        if (topicCode != null && !topicCode.isBlank()) {
            criteria.add(Criteria.where("topicCode")
                    .is(normalization.normalizeFilterCode(topicCode)));
        }
        if (examNumber != null) {
            criteria.add(Criteria.where("examType").is(examType));
            criteria.add(Criteria.where("examNumber").is(examNumber));
        }
        if (difficulty != null) {
            criteria.add(Criteria.where("difficulty").is(difficulty));
        }
        addCursor(criteria, afterId);

        Query query = new Query(new Criteria().andOperator(criteria));
        query.with(Sort.by(Sort.Direction.ASC, "_id"));
        query.limit(pageSize + 1);
        applyDeliveryProjection(query);
        List<BcsQuestion> found = mongo.find(query, BcsQuestion.class);
        return page(found, pageSize, this::delivery);
    }

    public QuestionDeliveryResponse getPublished(String questionId) {
        Query query = Query.query(Criteria.where("questionId").is(questionId)
                .and("status").is(QuestionStatus.PUBLISHED));
        applyDeliveryProjection(query);
        BcsQuestion question = java.util.Optional.ofNullable(
                        mongo.findOne(query, BcsQuestion.class))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Published BCS question not found: " + questionId));
        return delivery(question);
    }

    public CursorPage<TrainingQuestionResponse> findTrainingQuestions(
            boolean predictionOnly, String afterId, int limit) {
        int pageSize = validLimit(limit, MAX_TRAINING_PAGE_SIZE);
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where(predictionOnly
                ? "predictionEligible" : "trainingEligible").is(true));
        addCursor(criteria, afterId);

        Query query = new Query(new Criteria().andOperator(criteria));
        query.with(Sort.by(Sort.Direction.ASC, "_id"));
        query.limit(pageSize + 1);
        List<BcsQuestion> found = mongo.find(query, BcsQuestion.class);
        return page(found, pageSize, this::training);
    }

    private void addCursor(List<Criteria> criteria, String afterId) {
        if (afterId == null || afterId.isBlank()) {
            return;
        }
        if (!ObjectId.isValid(afterId)) {
            throw new InvalidRequestException("Invalid question cursor");
        }
        criteria.add(Criteria.where("_id").gt(new ObjectId(afterId)));
    }

    private int validLimit(int requested, int maximum) {
        if (requested < 1 || requested > maximum) {
            throw new InvalidRequestException(
                    "Page size must be between 1 and " + maximum);
        }
        return requested;
    }

    private void applyDeliveryProjection(Query query) {
        query.fields()
                .include("questionId")
                .include("examType")
                .include("examNumber")
                .include("questionNumber")
                .include("language")
                .include("subjectCode")
                .include("topicCode")
                .include("subTopicCode")
                .include("conceptCodes")
                .include("questionKind")
                .include("questionText")
                .include("options")
                .include("difficulty")
                .include("questionImageUrls");
    }

    private <T> CursorPage<T> page(List<BcsQuestion> found, int pageSize,
                                   java.util.function.Function<BcsQuestion, T> mapper) {
        boolean hasMore = found.size() > pageSize;
        List<BcsQuestion> visible = hasMore ? found.subList(0, pageSize) : found;
        String nextCursor = hasMore && !visible.isEmpty()
                ? visible.getLast().getId() : null;
        return new CursorPage<>(visible.stream().map(mapper).toList(), nextCursor, hasMore);
    }

    private QuestionDeliveryResponse delivery(BcsQuestion question) {
        return new QuestionDeliveryResponse(
                question.getQuestionId(), question.getExamType(), question.getExamNumber(),
                question.getQuestionNumber(), question.getLanguage(), question.getSubjectCode(),
                question.getTopicCode(), question.getSubTopicCode(), question.getConceptCodes(),
                question.getQuestionKind(), question.getQuestionText(), question.getOptions(),
                question.getDifficulty(), question.getQuestionImageUrls());
    }

    private TrainingQuestionResponse training(BcsQuestion question) {
        return new TrainingQuestionResponse(
                question.getId(), question.getQuestionId(), question.getFingerprint(),
                question.getExamType(), question.getExamNumber(), question.getExamLabelYear(),
                question.getPreliminaryHeldDate(), question.getExamSetCode(),
                question.getQuestionNumber(), question.getLanguage(), question.getSubjectCode(),
                question.getTopicCode(), question.getSubTopicCode(), question.getConceptCodes(),
                question.getQuestionKind(), question.getQuestionText(), question.getOptions(),
                question.getCorrectOptionKey(), question.getExplanation(),
                question.getDifficulty(), question.getTags(), question.isTimeSensitive(),
                question.getSource().type(), question.getAnswerConfidence(),
                question.getLabelConfidence(), question.isPredictionEligible(),
                question.getSchemaVersion());
    }
}
