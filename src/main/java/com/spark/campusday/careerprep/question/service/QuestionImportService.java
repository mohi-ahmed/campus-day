package com.spark.campusday.careerprep.question.service;

import com.spark.campusday.careerprep.question.dto.QuestionImportItem;
import com.spark.campusday.careerprep.question.dto.QuestionImportRequest;
import com.spark.campusday.careerprep.question.dto.QuestionImportResponse;
import com.spark.campusday.careerprep.question.model.BcsQuestion;
import com.spark.campusday.careerprep.question.model.QuestionImportBatch;
import com.spark.campusday.careerprep.question.model.QuestionImportStatus;
import com.spark.campusday.careerprep.question.model.RawQuestionImportRecord;
import com.spark.campusday.careerprep.question.repository.BcsQuestionRepository;
import com.spark.campusday.careerprep.question.repository.QuestionImportBatchRepository;
import com.spark.campusday.careerprep.question.repository.RawQuestionImportRecordRepository;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionImportService {
    private static final int MAX_RECORDED_ERRORS = 100;

    private final BcsQuestionRepository questions;
    private final QuestionImportBatchRepository batches;
    private final RawQuestionImportRecordRepository rawRecords;
    private final QuestionNormalizationService normalization;
    private final QuestionTaxonomyService taxonomy;
    private final Validator validator;

    public QuestionImportResponse importQuestions(QuestionImportRequest request) {
        return batches.findByBatchKey(request.batchKey().trim())
                .map(this::response)
                .orElseGet(() -> executeImport(request));
    }

    public QuestionImportResponse getBatch(String batchId) {
        return batches.findById(batchId)
                .map(this::response)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Question import batch not found: " + batchId));
    }

    private QuestionImportResponse executeImport(QuestionImportRequest request) {
        QuestionImportBatch batch = batches.save(QuestionImportBatch.builder()
                .batchKey(request.batchKey().trim())
                .sourceName(request.sourceName().trim())
                .sourceType(request.sourceType())
                .sourceUrl(normalize(request.sourceUrl()))
                .rightsStatus(request.rightsStatus())
                .schemaVersion(BcsQuestion.CURRENT_SCHEMA_VERSION)
                .status(QuestionImportStatus.RUNNING)
                .receivedCount(request.questions().size())
                .errors(new ArrayList<>())
                .build());

        int inserted = 0;
        int skipped = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        var activeTaxonomy = taxonomy.activeNodeMap();
        for (int index = 0; index < request.questions().size(); index++) {
            QuestionImportItem item = request.questions().get(index);
            RawQuestionImportRecord raw = RawQuestionImportRecord.builder()
                    .importBatchId(batch.getId())
                    .ordinal(index + 1)
                    .payload(Map.of())
                    .build();
            try {
                raw = rawRecord(batch.getId(), index + 1, item);
                raw = rawRecords.save(raw);
                BcsQuestion question = normalization.normalize(request, item, batch.getId());
                taxonomy.validateQuestion(question, activeTaxonomy);
                validate(question);
                if (questions.existsBySourceKey(question.getSourceKey())) {
                    skipped++;
                } else {
                    try {
                        BcsQuestion saved = questions.save(question);
                        raw.setNormalizedQuestionId(saved.getQuestionId());
                        inserted++;
                    } catch (DuplicateKeyException duplicate) {
                        skipped++;
                    }
                }
                rawRecords.save(raw);
            } catch (RuntimeException exception) {
                failed++;
                String message = "Item " + (index + 1) + ": " + safeMessage(exception);
                if (errors.size() < MAX_RECORDED_ERRORS) {
                    errors.add(message);
                }
                raw.setError(message);
                saveRawErrorSafely(raw);
            }
        }

        batch.setInsertedCount(inserted);
        batch.setSkippedCount(skipped);
        batch.setFailedCount(failed);
        batch.setErrors(List.copyOf(errors));
        batch.setStatus(failed == 0
                ? QuestionImportStatus.COMPLETED
                : QuestionImportStatus.COMPLETED_WITH_ERRORS);
        return response(batches.save(batch));
    }

    private RawQuestionImportRecord rawRecord(String batchId, int ordinal,
                                               QuestionImportItem item) {
        Map<String, Object> payload = item.rawPayload() == null
                ? canonicalPayload(item) : new LinkedHashMap<>(item.rawPayload());
        return RawQuestionImportRecord.builder()
                .importBatchId(batchId)
                .ordinal(ordinal)
                .externalId(normalize(item.externalId()))
                .rawChecksum(normalization.rawChecksum(payload))
                .payload(payload)
                .build();
    }

    private Map<String, Object> canonicalPayload(QuestionImportItem item) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("externalId", item.externalId());
        payload.put("examType", item.examType());
        payload.put("examNumber", item.examNumber());
        payload.put("examLabelYear", item.examLabelYear());
        payload.put("preliminaryHeldDate", item.preliminaryHeldDate());
        payload.put("examSetCode", item.examSetCode());
        payload.put("questionNumber", item.questionNumber());
        payload.put("language", item.language());
        payload.put("subjectCode", item.subjectCode());
        payload.put("topicCode", item.topicCode());
        payload.put("subTopicCode", item.subTopicCode());
        payload.put("conceptCodes", item.conceptCodes());
        payload.put("questionKind", item.questionKind());
        payload.put("questionText", item.questionText());
        payload.put("options", item.options());
        payload.put("correctOptionKey", item.correctOptionKey());
        payload.put("explanation", item.explanation());
        payload.put("difficulty", item.difficulty());
        payload.put("questionImageUrls", item.questionImageUrls());
        payload.put("explanationImageUrls", item.explanationImageUrls());
        payload.put("tags", item.tags());
        payload.put("timeSensitive", item.timeSensitive());
        payload.put("validFrom", item.validFrom());
        payload.put("validUntil", item.validUntil());
        payload.put("answerConfidence", item.answerConfidence());
        payload.put("labelConfidence", item.labelConfidence());
        return payload;
    }

    private void validate(BcsQuestion question) {
        var violations = validator.validate(question);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void saveRawErrorSafely(RawQuestionImportRecord raw) {
        try {
            rawRecords.save(raw);
        } catch (RuntimeException ignored) {
            // The batch error summary remains the durable failure record.
        }
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName() : message;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private QuestionImportResponse response(QuestionImportBatch batch) {
        return new QuestionImportResponse(batch.getId(), batch.getBatchKey(), batch.getStatus(),
                batch.getReceivedCount(), batch.getInsertedCount(), batch.getSkippedCount(),
                batch.getFailedCount(), batch.getErrors() == null ? List.of() : batch.getErrors(),
                batch.getCreatedAt(), batch.getUpdatedAt());
    }
}
