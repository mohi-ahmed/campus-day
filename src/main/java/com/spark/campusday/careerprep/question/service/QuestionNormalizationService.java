package com.spark.campusday.careerprep.question.service;

import com.spark.campusday.careerprep.question.dto.QuestionImportItem;
import com.spark.campusday.careerprep.question.dto.QuestionImportRequest;
import com.spark.campusday.careerprep.question.model.BcsQuestion;
import com.spark.campusday.careerprep.question.model.QuestionDifficulty;
import com.spark.campusday.careerprep.question.model.QuestionOption;
import com.spark.campusday.careerprep.question.model.QuestionSource;
import com.spark.campusday.careerprep.question.model.QuestionStatus;
import com.spark.campusday.common.exception.InvalidRequestException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class QuestionNormalizationService {
    public BcsQuestion normalize(QuestionImportRequest request, QuestionImportItem item,
                                 String importBatchId) {
        requireHistoricalExamIdentity(request, item);
        String questionText = normalizeText(item.questionText());
        List<QuestionOption> options = normalizeOptions(item.options());
        String correctOption = normalizeOptionKey(item.correctOptionKey());
        requireCorrectOption(options, correctOption);
        requireValidDates(item);

        String subjectCode = normalizeCode(item.subjectCode());
        String topicCode = normalizeCode(item.topicCode());
        String subTopicCode = nullableCode(item.subTopicCode());
        List<String> conceptCodes = normalizeConceptCodes(item.conceptCodes());
        List<String> tags = normalizeTags(item.tags());
        String fingerprint = fingerprint(questionText, options);
        String sourceKey = sourceKey(request, item, fingerprint);
        String questionId = questionId(item, sourceKey);

        return BcsQuestion.builder()
                .questionId(questionId)
                .sourceKey(sourceKey)
                .fingerprint(fingerprint)
                .examType(item.examType())
                .examNumber(item.examNumber())
                .examLabelYear(item.examLabelYear())
                .preliminaryHeldDate(item.preliminaryHeldDate())
                .examSetCode(normalizeNullable(item.examSetCode()))
                .questionNumber(item.questionNumber())
                .language(item.language())
                .subjectCode(subjectCode)
                .topicCode(topicCode)
                .subTopicCode(subTopicCode)
                .conceptCodes(conceptCodes)
                .questionKind(item.questionKind())
                .questionText(questionText)
                .normalizedQuestionText(questionText.toLowerCase(Locale.ROOT))
                .options(options)
                .correctOptionKey(correctOption)
                .explanation(normalizeNullableText(item.explanation()))
                .difficulty(item.difficulty() == null
                        ? QuestionDifficulty.UNKNOWN : item.difficulty())
                .questionImageUrls(normalizeUrls(item.questionImageUrls()))
                .explanationImageUrls(normalizeUrls(item.explanationImageUrls()))
                .tags(tags)
                .timeSensitive(item.timeSensitive())
                .validFrom(item.validFrom())
                .validUntil(item.validUntil())
                .source(new QuestionSource(
                        request.sourceName().trim(),
                        request.sourceType(),
                        normalizeNullable(request.sourceUrl()),
                        normalizeNullable(item.externalId()),
                        normalizeNullable(request.licenseName()),
                        normalizeNullable(request.attribution()),
                        request.retrievedAt() == null ? Instant.now() : request.retrievedAt()))
                .rightsStatus(request.rightsStatus())
                .status(QuestionStatus.NEEDS_REVIEW)
                .answerConfidence(item.answerConfidence())
                .labelConfidence(item.labelConfidence())
                .trainingEligible(false)
                .predictionEligible(false)
                .importBatchId(importBatchId)
                .schemaVersion(BcsQuestion.CURRENT_SCHEMA_VERSION)
                .build();
    }

    public String rawChecksum(Object payload) {
        return sha256(String.valueOf(payload));
    }

    public String normalizeFilterCode(String value) {
        return normalizeCode(value);
    }

    private List<QuestionOption> normalizeOptions(List<QuestionOption> rawOptions) {
        if (rawOptions == null) {
            throw new InvalidRequestException("Question options are required");
        }
        List<QuestionOption> normalized = new ArrayList<>(rawOptions.size());
        Set<String> keys = new LinkedHashSet<>();
        Set<String> texts = new LinkedHashSet<>();
        for (QuestionOption option : rawOptions) {
            if (option == null) {
                throw new InvalidRequestException("Question option cannot be null");
            }
            String key = normalizeOptionKey(option.key());
            String text = normalizeText(option.text());
            if (!keys.add(key)) {
                throw new InvalidRequestException("Duplicate option key: " + key);
            }
            String comparableText = text.toLowerCase(Locale.ROOT);
            if (!texts.add(comparableText)) {
                throw new InvalidRequestException("Duplicate option text is not allowed");
            }
            normalized.add(new QuestionOption(key, text));
        }
        if (normalized.size() < 2 || normalized.size() > 8) {
            throw new InvalidRequestException("A question must have between 2 and 8 options");
        }
        return List.copyOf(normalized);
    }

    private void requireCorrectOption(List<QuestionOption> options, String correctOption) {
        boolean present = options.stream().anyMatch(option -> option.key().equals(correctOption));
        if (!present) {
            throw new InvalidRequestException(
                    "Correct option key must match one of the question options");
        }
    }

    private void requireValidDates(QuestionImportItem item) {
        if (item.validFrom() != null && item.validUntil() != null
                && item.validFrom().isAfter(item.validUntil())) {
            throw new InvalidRequestException("validFrom cannot be after validUntil");
        }
    }

    private void requireHistoricalExamIdentity(QuestionImportRequest request,
                                               QuestionImportItem item) {
        boolean historical = request.sourceType()
                == com.spark.campusday.careerprep.question.model.QuestionSourceType.PREVIOUS_BCS
                || request.sourceType()
                == com.spark.campusday.careerprep.question.model.QuestionSourceType.OFFICIAL_BPSC;
        if (historical && (item.examType() == null || item.examNumber() == null
                || item.questionNumber() == null)) {
            throw new InvalidRequestException(
                    "Historical BCS questions require examType, examNumber and questionNumber");
        }
    }

    private String sourceKey(QuestionImportRequest request, QuestionImportItem item,
                             String fingerprint) {
        String identity = normalizeNullable(item.externalId());
        if (identity == null) {
            identity = item.examType() + "|" + item.examNumber() + "|"
                    + normalizeNullable(item.examSetCode()) + "|" + item.questionNumber()
                    + "|" + fingerprint;
        }
        return sha256(request.sourceName().trim().toLowerCase(Locale.ROOT) + "|" + identity);
    }

    private String questionId(QuestionImportItem item, String sourceKey) {
        if (item.examNumber() != null && item.questionNumber() != null) {
            String setCode = normalizeNullable(item.examSetCode());
            return "BCS-" + item.examType() + "-" + item.examNumber() + "-"
                    + (setCode == null ? "MAIN" : identifierPart(setCode)) + "-Q"
                    + item.questionNumber();
        }
        return "BCS-SOURCE-" + sourceKey.substring(0, 20).toUpperCase(Locale.ROOT);
    }

    private String fingerprint(String questionText, List<QuestionOption> options) {
        StringBuilder canonical = new StringBuilder(questionText.toLowerCase(Locale.ROOT));
        options.forEach(option -> canonical.append('|')
                .append(option.key()).append(':')
                .append(option.text().toLowerCase(Locale.ROOT)));
        return sha256(canonical.toString());
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String normalizeOptionKey(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException("Option key is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9]{1,8}")) {
            throw new InvalidRequestException("Option key must contain only A-Z or 0-9");
        }
        return normalized;
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException("Question taxonomy code is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.length() < 2) {
            throw new InvalidRequestException("Question taxonomy code is invalid");
        }
        return normalized;
    }

    private String nullableCode(String value) {
        return value == null || value.isBlank() ? null : normalizeCode(value);
    }

    private List<String> normalizeConceptCodes(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT)
                        .replaceAll("[^A-Z0-9_.]+", "_")
                        .replaceAll("^[_.]+|[_.]+$", ""))
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> normalizeTags(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT)
                        .replaceAll("[^A-Z0-9_.-]+", "_")
                        .replaceAll("^[_.-]+|[_.-]+$", ""))
                .filter(value -> value.length() >= 2)
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> normalizeUrls(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(this::normalizeNullable)
                .filter(value -> value != null)
                .distinct()
                .toList();
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException("Question text is required");
        }
        return value.strip().replaceAll("\\s+", " ");
    }

    private String normalizeNullableText(String value) {
        return value == null || value.isBlank() ? null
                : value.strip().replaceAll("\\s+", " ");
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String identifierPart(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.isBlank() ? "MAIN" : normalized;
    }
}
