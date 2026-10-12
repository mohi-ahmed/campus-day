package com.spark.campusday.careerprep.knowledge.ingest;

import com.spark.campusday.careerprep.knowledge.model.KnowledgeIngestionStatus;
import com.spark.campusday.careerprep.knowledge.model.KnowledgeSection;
import com.spark.campusday.careerprep.knowledge.model.KnowledgeSource;
import com.spark.campusday.careerprep.knowledge.repository.KnowledgeSectionRepository;
import com.spark.campusday.careerprep.knowledge.repository.KnowledgeSourceRepository;
import com.spark.campusday.careerprep.question.model.BcsTaxonomyNode;
import com.spark.campusday.careerprep.question.model.QuestionLanguage;
import com.spark.campusday.careerprep.question.model.TaxonomyLevel;
import com.spark.campusday.careerprep.question.service.QuestionTaxonomyService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Order(400)
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = {"app.career-prep.enabled", "app.career-prep.knowledge.sections.enabled"},
        havingValue = "true")
public class KnowledgeSectionImportRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeSectionImportRunner.class);
    private static final int SAVE_CHUNK_SIZE = 500;

    private final ObjectMapper objectMapper;
    private final KnowledgeSourceRepository sources;
    private final KnowledgeSectionRepository sections;
    private final QuestionTaxonomyService taxonomyService;

    @Value("${app.career-prep.knowledge.sections.path:data/import/staging/knowledge/sections.json}")
    private String importPath;

    @Value("${app.career-prep.knowledge.sections.max-import-file-bytes:26214400}")
    private long maxImportFileBytes;

    @Value("${app.career-prep.knowledge.sections.max-sections-per-source:500}")
    private int maxSectionsPerSource;

    @Value("${app.career-prep.knowledge.sections.max-section-characters:4500}")
    private int maxSectionCharacters;

    @Value("${app.career-prep.knowledge.sections.max-section-utf8-bytes:16000}")
    private int maxSectionUtf8Bytes;

    @Value("${app.career-prep.knowledge.sections.max-total-characters:1500000}")
    private long maxTotalCharacters;

    @Value("${app.career-prep.knowledge.sections.min-extraction-quality:0.70}")
    private double minExtractionQuality;

    @Value("${app.career-prep.knowledge.sections.require-topic:true}")
    private boolean requireTopic;

    @Value("${app.career-prep.knowledge.sections.reject-ocr-required:true}")
    private boolean rejectOcrRequired;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path path = Path.of(importPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Knowledge-section import file does not exist: " + path);
        }
        long importFileBytes = Files.size(path);
        if (importFileBytes > maxImportFileBytes) {
            throw new IllegalStateException("Knowledge-section import file exceeds "
                    + maxImportFileBytes + " bytes: " + importFileBytes);
        }
        JsonNode root = objectMapper.readTree(path.toFile());
        String sourceId = requiredText(root, "sourceId");
        KnowledgeSource source = sources.findBySourceId(sourceId)
                .orElseThrow(() -> new IllegalStateException(
                        "Knowledge source is not registered: " + sourceId));
        String documentSha256 = requiredText(root, "documentSha256").toUpperCase();
        if (source.getSha256() != null && !source.getSha256().equalsIgnoreCase(documentSha256)) {
            throw new IllegalStateException("Knowledge document checksum does not match registry");
        }
        String extractionMethod = requiredText(root, "extractionMethod");
        String extractionVersion = requiredText(root, "extractionVersion");
        JsonNode sectionNodes = root.path("sections");
        if (!sectionNodes.isArray()) {
            throw new IllegalStateException("Knowledge sections must be an array");
        }
        if (sectionNodes.isEmpty()) {
            throw new IllegalStateException("Knowledge-section import contains no sections");
        }
        if (sectionNodes.size() > maxSectionsPerSource) {
            throw new IllegalStateException("Knowledge-section import exceeds per-source section limit: "
                    + sectionNodes.size() + " > " + maxSectionsPerSource);
        }

        Map<String, BcsTaxonomyNode> taxonomy = taxonomyService.activeNodeMap();
        List<KnowledgeSection> existingSections = sections.findBySourceId(sourceId);
        if (existingSections.size() > maxSectionsPerSource) {
            throw new IllegalStateException("Existing source already exceeds section limit: " + sourceId);
        }
        Map<String, KnowledgeSection> existing = existingSections.stream()
                .collect(Collectors.toMap(KnowledgeSection::getSectionId,
                        Function.identity(), (left, right) -> left));
        Set<String> checksums = existingSections.stream()
                .map(KnowledgeSection::getSha256)
                .filter(value -> value != null && !value.isBlank())
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(HashSet::new));
        Set<Integer> ordinals = new HashSet<>();
        List<KnowledgeSection> buffer = new ArrayList<>(SAVE_CHUNK_SIZE);
        int projectedSectionCount = existingSections.size();
        long projectedCharacterCount = existingSections.stream()
                .map(KnowledgeSection::getNormalizedText)
                .filter(value -> value != null)
                .mapToLong(String::length)
                .sum();
        if (projectedCharacterCount > maxTotalCharacters) {
            throw new IllegalStateException("Existing source already exceeds total character limit: "
                    + sourceId);
        }
        int inserted = 0;
        int updated = 0;
        int duplicateSkipped = 0;
        long incomingCharacters = 0;

        for (JsonNode node : sectionNodes) {
            int ordinal = node.path("ordinal").asInt();
            if (ordinal < 1 || !ordinals.add(ordinal)) {
                throw new IllegalStateException("Invalid or duplicate knowledge-section ordinal: " + ordinal);
            }
            String sectionId = sourceId + ":" + ordinal;
            String normalizedText = requiredText(node, "normalizedText");
            validateTextSize(sectionId, normalizedText);
            incomingCharacters += normalizedText.length();
            if (incomingCharacters > maxTotalCharacters) {
                throw new IllegalStateException("Knowledge-section import exceeds total character limit: "
                        + incomingCharacters + " > " + maxTotalCharacters);
            }
            String declaredChecksum = requiredText(node, "sha256").toUpperCase();
            if (!sha256(normalizedText).equals(declaredChecksum)) {
                throw new IllegalStateException("Knowledge-section checksum mismatch: " + sectionId);
            }
            String subjectCode = requiredText(node, "subjectCode");
            if (!source.getSubjectCodes().contains(subjectCode)) {
                throw new IllegalStateException(
                        "Knowledge section subject is outside source scope: " + sectionId);
            }
            String topicCode = textOrNull(node, "topicCode");
            if (requireTopic && topicCode == null) {
                throw new IllegalStateException(
                        "Knowledge section must be classified to a BCS topic before import: " + sectionId);
            }
            String subTopicCode = textOrNull(node, "subTopicCode");
            List<String> conceptCodes = textList(node.path("conceptCodes"));
            validateTaxonomy(subjectCode, topicCode, subTopicCode, conceptCodes, taxonomy);

            double extractionQuality = node.path("extractionQuality").asDouble(0.0);
            boolean requiresOcr = node.path("requiresOcr").asBoolean(true);
            if (extractionQuality < minExtractionQuality) {
                throw new IllegalStateException("Knowledge-section extraction quality is below "
                        + minExtractionQuality + ": " + sectionId);
            }
            if (rejectOcrRequired && requiresOcr) {
                throw new IllegalStateException(
                        "Knowledge section requires OCR/review before import: " + sectionId);
            }

            KnowledgeSection section = existing.get(sectionId);
            boolean isNew = section == null;
            if (isNew) {
                section = new KnowledgeSection();
                section.setSectionId(sectionId);
                section.setSourceId(sourceId);
            } else if (section.isApproved()) {
                continue;
            }
            String previousChecksum = isNew ? null : section.getSha256();
            boolean checksumBelongsToThisSection = previousChecksum != null
                    && previousChecksum.equalsIgnoreCase(declaredChecksum);
            if (!checksumBelongsToThisSection && !checksums.add(declaredChecksum)) {
                duplicateSkipped++;
                continue;
            }
            if (previousChecksum != null && !previousChecksum.isBlank()
                    && !previousChecksum.equalsIgnoreCase(declaredChecksum)) {
                checksums.remove(previousChecksum.toUpperCase());
            }
            int previousCharacters = isNew || section.getNormalizedText() == null
                    ? 0 : section.getNormalizedText().length();
            long nextProjectedCharacters = projectedCharacterCount
                    - previousCharacters + normalizedText.length();
            if (nextProjectedCharacters > maxTotalCharacters) {
                throw new IllegalStateException("Knowledge source would exceed total character limit: "
                        + nextProjectedCharacters + " > " + maxTotalCharacters);
            }
            if (isNew && ++projectedSectionCount > maxSectionsPerSource) {
                throw new IllegalStateException("Knowledge source would exceed section limit: "
                        + projectedSectionCount + " > " + maxSectionsPerSource);
            }
            projectedCharacterCount = nextProjectedCharacters;
            section.setOrdinal(ordinal);
            section.setChapter(textOrNull(node, "chapter"));
            section.setHeading(textOrNull(node, "heading"));
            section.setPageStart(integerOrNull(node, "pageStart"));
            section.setPageEnd(integerOrNull(node, "pageEnd"));
            section.setLanguage(QuestionLanguage.valueOf(requiredText(node, "language")));
            section.setNormalizedText(normalizedText);
            section.setSha256(declaredChecksum);
            section.setSubjectCode(subjectCode);
            section.setTopicCode(topicCode);
            section.setSubTopicCode(subTopicCode);
            section.setConceptCodes(conceptCodes);
            section.setRightsStatus(source.getRightsStatus());
            section.setApproved(false);
            section.setVerificationEligible(false);
            section.setTrainingEligible(false);
            section.setExtractionMethod(extractionMethod);
            section.setExtractionVersion(extractionVersion);
            section.setExtractionQuality(extractionQuality);
            section.setRequiresOcr(requiresOcr);
            buffer.add(section);
            if (isNew) {
                inserted++;
            } else {
                updated++;
            }
            if (buffer.size() >= SAVE_CHUNK_SIZE) {
                sections.saveAll(buffer);
                buffer.clear();
            }
        }
        if (!buffer.isEmpty()) {
            sections.saveAll(buffer);
        }

        if (source.getSha256() == null) {
            source.setSha256(documentSha256);
        }
        source.setIngestionStatus(KnowledgeIngestionStatus.NEEDS_REVIEW);
        source.setVerificationEligible(false);
        source.setTrainingEligible(false);
        List<KnowledgeSection> retainedSections = sections.findBySourceId(sourceId);
        source.setRetainedSectionCount(retainedSections.size());
        source.setRetainedCharacterCount(retainedSections.stream()
                .map(KnowledgeSection::getNormalizedText)
                .filter(value -> value != null)
                .mapToLong(String::length)
                .sum());
        sources.save(source);
        log.info("BCS knowledge sections imported: sourceId={}, received={}, inserted={}, "
                        + "updated={}, duplicateSkipped={}, retainedSections={}, retainedCharacters={}",
                sourceId, sectionNodes.size(), inserted, updated, duplicateSkipped,
                source.getRetainedSectionCount(), source.getRetainedCharacterCount());
    }

    private void validateTextSize(String sectionId, String normalizedText) {
        if (normalizedText.length() > maxSectionCharacters) {
            throw new IllegalStateException("Knowledge section exceeds character limit: " + sectionId);
        }
        int normalizedBytes = normalizedText.getBytes(StandardCharsets.UTF_8).length;
        if (normalizedBytes > maxSectionUtf8Bytes) {
            throw new IllegalStateException("Knowledge section exceeds UTF-8 byte limit: " + sectionId);
        }
    }

    private void validateTaxonomy(String subjectCode, String topicCode, String subTopicCode,
                                  List<String> conceptCodes,
                                  Map<String, BcsTaxonomyNode> taxonomy) {
        BcsTaxonomyNode subject = requireNode(taxonomy, subjectCode, TaxonomyLevel.SUBJECT);
        if (topicCode == null) {
            if (subTopicCode != null || !conceptCodes.isEmpty()) {
                throw new IllegalStateException("Subtopic/concepts require a topic");
            }
            return;
        }
        BcsTaxonomyNode topic = requireNode(taxonomy, topicCode, TaxonomyLevel.TOPIC);
        requireParent(topic, subject.getCode());
        String conceptParent = topic.getCode();
        if (subTopicCode != null) {
            BcsTaxonomyNode subTopic = requireNode(taxonomy, subTopicCode, TaxonomyLevel.SUBTOPIC);
            requireParent(subTopic, topic.getCode());
            conceptParent = subTopic.getCode();
        }
        for (String code : conceptCodes) {
            BcsTaxonomyNode concept = requireNode(taxonomy, code, TaxonomyLevel.CONCEPT);
            if (!conceptParent.equals(concept.getParentCode())
                    && !topic.getCode().equals(concept.getParentCode())) {
                throw new IllegalStateException("Concept is outside section hierarchy: " + code);
            }
        }
    }

    private BcsTaxonomyNode requireNode(Map<String, BcsTaxonomyNode> taxonomy,
                                        String code, TaxonomyLevel level) {
        BcsTaxonomyNode node = taxonomy.get(code);
        if (node == null || node.getLevel() != level) {
            throw new IllegalStateException("Unknown knowledge-section taxonomy node: " + code);
        }
        return node;
    }

    private void requireParent(BcsTaxonomyNode node, String parentCode) {
        if (!parentCode.equals(node.getParentCode())) {
            throw new IllegalStateException("Invalid taxonomy parent for " + node.getCode());
        }
    }

    private String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        return java.util.HexFormat.of().withUpperCase().formatHex(digest);
    }

    private List<String> textList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(value -> values.add(value.asText()));
        return List.copyOf(values);
    }

    private Integer integerOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asInt();
    }

    private String requiredText(JsonNode node, String field) {
        String value = textOrNull(node, field);
        if (value == null) {
            throw new IllegalStateException("Required knowledge-section field is missing: " + field);
        }
        return value;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }
}
