package com.spark.campusday.careerprep.question.ingest;

import com.spark.campusday.careerprep.question.dto.TaxonomyNodeRequest;
import com.spark.campusday.careerprep.question.model.AnswerVerificationStatus;
import com.spark.campusday.careerprep.question.model.BcsQuestion;
import com.spark.campusday.careerprep.question.model.BcsTaxonomyNode;
import com.spark.campusday.careerprep.question.model.ClassificationReviewStatus;
import com.spark.campusday.careerprep.question.model.QuestionAnswerVerification;
import com.spark.campusday.careerprep.question.model.QuestionClassificationProposal;
import com.spark.campusday.careerprep.question.model.TaxonomyLevel;
import com.spark.campusday.careerprep.question.repository.BcsQuestionRepository;
import com.spark.campusday.careerprep.question.repository.QuestionAnswerVerificationRepository;
import com.spark.campusday.careerprep.question.repository.QuestionClassificationProposalRepository;
import com.spark.campusday.careerprep.question.service.QuestionTaxonomyService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Order(200)
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = {"app.career-prep.enabled", "app.career-prep.enrichment.enabled"},
        havingValue = "true")
public class BcsQuestionEnrichmentRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BcsQuestionEnrichmentRunner.class);
    private static final int SAVE_CHUNK_SIZE = 500;
    private static final String UNCLASSIFIED_SUFFIX = "_UNCLASSIFIED";

    private final ObjectMapper objectMapper;
    private final QuestionTaxonomyService taxonomyService;
    private final BcsQuestionRepository questions;
    private final QuestionClassificationProposalRepository classifications;
    private final QuestionAnswerVerificationRepository verifications;

    @Value("${app.career-prep.enrichment.taxonomy-resource:data/bcs/bpsc-50-preliminary-taxonomy.json}")
    private String taxonomyResource;

    @Value("${app.career-prep.enrichment.rules-resource:data/bcs/bcs-classification-rules-v1.json}")
    private String rulesResource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String taxonomyVersion = seedOfficialTaxonomy();
        RuleSet ruleSet = readRules();
        if (!taxonomyVersion.equals(ruleSet.version())) {
            throw new IllegalStateException("Taxonomy and classification rule versions do not match");
        }

        Map<String, BcsTaxonomyNode> taxonomy = taxonomyService.activeNodeMap();
        validateRules(ruleSet, taxonomy);

        Map<String, QuestionClassificationProposal> existingClassifications =
                classifications.findAll().stream().collect(Collectors.toMap(
                        QuestionClassificationProposal::getQuestionId,
                        Function.identity(), (left, right) -> left));
        Map<String, QuestionAnswerVerification> existingVerifications =
                verifications.findAll().stream().collect(Collectors.toMap(
                        QuestionAnswerVerification::getQuestionId,
                        Function.identity(), (left, right) -> left));

        List<QuestionClassificationProposal> classificationBuffer = new ArrayList<>(SAVE_CHUNK_SIZE);
        List<QuestionAnswerVerification> verificationBuffer = new ArrayList<>(SAVE_CHUNK_SIZE);
        int assignedTopics = 0;
        int assignedSubTopics = 0;
        int assignedConcepts = 0;
        int unclassified = 0;
        int verificationCreated = 0;

        for (BcsQuestion question : questions.findAll()) {
            QuestionClassificationProposal current = existingClassifications.get(question.getQuestionId());
            if (current == null || current.getReviewStatus() == ClassificationReviewStatus.PROPOSED) {
                ClassificationResult result = classify(question, ruleSet);
                QuestionClassificationProposal proposal = current == null
                        ? new QuestionClassificationProposal() : current;
                proposal.setQuestionId(question.getQuestionId());
                proposal.setSubjectCode(question.getSubjectCode());
                proposal.setTaxonomyVersion(ruleSet.version());
                proposal.setProposedTopicCode(result.topicCode());
                proposal.setProposedSubTopicCode(result.subTopicCode());
                proposal.setProposedConceptCodes(result.conceptCodes());
                proposal.setMethod(ruleSet.method());
                proposal.setConfidence(result.confidence());
                proposal.setMatchedSignals(result.matchedSignals());
                proposal.setReviewStatus(ClassificationReviewStatus.PROPOSED);
                classificationBuffer.add(proposal);

                if (result.topicCode().endsWith(UNCLASSIFIED_SUFFIX)) {
                    unclassified++;
                } else {
                    assignedTopics++;
                    if (result.subTopicCode() != null) {
                        assignedSubTopics++;
                    }
                    if (!result.conceptCodes().isEmpty()) {
                        assignedConcepts++;
                    }
                }
            }

            if (!existingVerifications.containsKey(question.getQuestionId())) {
                verificationBuffer.add(QuestionAnswerVerification.builder()
                        .questionId(question.getQuestionId())
                        .claimedOptionKey(question.getCorrectOptionKey())
                        .status(AnswerVerificationStatus.PENDING_EVIDENCE)
                        .evidence(List.of())
                        .notes("Imported answer key is a claim until supported by an approved source")
                        .build());
                verificationCreated++;
            }

            if (classificationBuffer.size() >= SAVE_CHUNK_SIZE) {
                classifications.saveAll(classificationBuffer);
                classificationBuffer.clear();
            }
            if (verificationBuffer.size() >= SAVE_CHUNK_SIZE) {
                verifications.saveAll(verificationBuffer);
                verificationBuffer.clear();
            }
        }
        if (!classificationBuffer.isEmpty()) {
            classifications.saveAll(classificationBuffer);
        }
        if (!verificationBuffer.isEmpty()) {
            verifications.saveAll(verificationBuffer);
        }

        log.info("BCS enrichment finished: taxonomyVersion={}, topicAssigned={}, "
                        + "subTopicAssigned={}, conceptAssigned={}, unclassified={}, "
                        + "verificationRecordsCreated={}",
                taxonomyVersion, assignedTopics, assignedSubTopics, assignedConcepts,
                unclassified, verificationCreated);
    }

    private String seedOfficialTaxonomy() throws Exception {
        JsonNode root = readResource(taxonomyResource);
        String version = requiredText(root, "version");
        JsonNode nodes = root.path("nodes");
        if (!nodes.isArray()) {
            throw new IllegalStateException("Official BCS taxonomy nodes must be an array");
        }
        List<TaxonomyNodeRequest> requests = new ArrayList<>();
        for (JsonNode node : nodes) {
            requests.add(new TaxonomyNodeRequest(
                    requiredText(node, "code"),
                    textOrNull(node, "parentCode"),
                    TaxonomyLevel.valueOf(requiredText(node, "level")),
                    requiredText(node, "nameEn"),
                    textOrNull(node, "nameBn"),
                    node.has("syllabusMarks") ? node.path("syllabusMarks").asDouble() : null,
                    version,
                    node.path("displayOrder").asInt(),
                    true));
        }
        taxonomyService.upsert(requests);
        log.info("Official BPSC taxonomy seeded: version={}, nodes={}", version, requests.size());
        return version;
    }

    private RuleSet readRules() throws Exception {
        JsonNode root = readResource(rulesResource);
        List<ClassificationRule> rules = new ArrayList<>();
        for (JsonNode rule : root.path("rules")) {
            List<String> conceptCodes = new ArrayList<>();
            rule.path("conceptCodes").forEach(value -> conceptCodes.add(value.asText()));
            List<Signal> signals = new ArrayList<>();
            for (JsonNode signal : rule.path("signals")) {
                signals.add(new Signal(
                        normalize(requiredText(signal, "value")),
                        signal.path("weight").asDouble(1.0)));
            }
            rules.add(new ClassificationRule(
                    requiredText(rule, "subjectCode"),
                    requiredText(rule, "topicCode"),
                    textOrNull(rule, "subTopicCode"),
                    List.copyOf(conceptCodes),
                    List.copyOf(signals)));
        }
        return new RuleSet(
                requiredText(root, "version"),
                requiredText(root, "method"),
                root.path("minimumScore").asDouble(1.5),
                root.path("minimumMargin").asDouble(0.5),
                List.copyOf(rules));
    }

    private ClassificationResult classify(BcsQuestion question, RuleSet ruleSet) {
        String content = searchableContent(question);
        List<ScoredRule> candidates = ruleSet.rules().stream()
                .filter(rule -> rule.subjectCode().equals(question.getSubjectCode()))
                .map(rule -> score(rule, content))
                .filter(candidate -> candidate.score() > 0)
                .sorted(Comparator.comparingDouble(ScoredRule::score).reversed())
                .toList();

        if (candidates.isEmpty() || candidates.getFirst().score() < ruleSet.minimumScore()) {
            return unclassified(question);
        }

        ScoredRule best = candidates.getFirst();
        ScoredRule second = candidates.size() > 1 ? candidates.get(1) : null;
        double margin = second == null ? best.score() : best.score() - second.score();
        if (second != null && margin < ruleSet.minimumMargin()) {
            if (!best.rule().topicCode().equals(second.rule().topicCode())) {
                return unclassified(question);
            }
            return new ClassificationResult(
                    best.rule().topicCode(),
                    Objects.equals(best.rule().subTopicCode(), second.rule().subTopicCode())
                            ? best.rule().subTopicCode() : null,
                    List.of(),
                    confidence(best.score(), margin, 0.08),
                    mergeSignals(best, second));
        }

        return new ClassificationResult(
                best.rule().topicCode(),
                best.rule().subTopicCode(),
                best.rule().conceptCodes(),
                confidence(best.score(), margin, 0.0),
                best.matchedSignals());
    }

    private ScoredRule score(ClassificationRule rule, String content) {
        double score = 0;
        List<String> matched = new ArrayList<>();
        for (Signal signal : rule.signals()) {
            if (content.contains(signal.value())) {
                score += signal.weight();
                matched.add(signal.value());
            }
        }
        return new ScoredRule(rule, score, List.copyOf(matched));
    }

    private ClassificationResult unclassified(BcsQuestion question) {
        return new ClassificationResult(
                question.getSubjectCode() + UNCLASSIFIED_SUFFIX,
                null, List.of(), 0.0, List.of());
    }

    private double confidence(double score, double margin, double ambiguityPenalty) {
        double raw = 0.55 + Math.min(0.28, score * 0.08)
                + Math.min(0.12, Math.max(0, margin) * 0.04) - ambiguityPenalty;
        return Math.round(Math.min(0.95, Math.max(0.0, raw)) * 100.0) / 100.0;
    }

    private List<String> mergeSignals(ScoredRule first, ScoredRule second) {
        Set<String> signals = new LinkedHashSet<>(first.matchedSignals());
        signals.addAll(second.matchedSignals());
        return List.copyOf(signals);
    }

    private String searchableContent(BcsQuestion question) {
        StringBuilder content = new StringBuilder(question.getQuestionText());
        question.getOptions().forEach(option -> content.append(' ').append(option.text()));
        if (question.getExplanation() != null) {
            content.append(' ').append(question.getExplanation());
        }
        return normalize(content.toString());
    }

    private void validateRules(RuleSet ruleSet, Map<String, BcsTaxonomyNode> taxonomy) {
        for (ClassificationRule rule : ruleSet.rules()) {
            BcsTaxonomyNode subject = requireNode(taxonomy, rule.subjectCode(), TaxonomyLevel.SUBJECT);
            BcsTaxonomyNode topic = requireNode(taxonomy, rule.topicCode(), TaxonomyLevel.TOPIC);
            requireParent(topic, subject.getCode());
            String conceptParent = topic.getCode();
            if (rule.subTopicCode() != null) {
                BcsTaxonomyNode subTopic = requireNode(
                        taxonomy, rule.subTopicCode(), TaxonomyLevel.SUBTOPIC);
                requireParent(subTopic, topic.getCode());
                conceptParent = subTopic.getCode();
            }
            for (String conceptCode : rule.conceptCodes()) {
                BcsTaxonomyNode concept = requireNode(taxonomy, conceptCode, TaxonomyLevel.CONCEPT);
                if (!conceptParent.equals(concept.getParentCode())
                        && !topic.getCode().equals(concept.getParentCode())) {
                    throw new IllegalStateException(
                            "Classification concept does not belong to its rule: " + conceptCode);
                }
            }
        }
    }

    private BcsTaxonomyNode requireNode(Map<String, BcsTaxonomyNode> taxonomy,
                                        String code, TaxonomyLevel level) {
        BcsTaxonomyNode node = taxonomy.get(code);
        if (node == null || node.getLevel() != level) {
            throw new IllegalStateException("Invalid classification taxonomy node: " + code);
        }
        return node;
    }

    private void requireParent(BcsTaxonomyNode node, String expectedParent) {
        if (!expectedParent.equals(node.getParentCode())) {
            throw new IllegalStateException("Invalid taxonomy parent for " + node.getCode());
        }
    }

    private JsonNode readResource(String resourcePath) throws Exception {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            throw new IllegalStateException("BCS enrichment resource not found: " + resourcePath);
        }
        try (var input = resource.getInputStream()) {
            return objectMapper.readTree(input);
        }
    }

    private String requiredText(JsonNode node, String field) {
        String value = textOrNull(node, field);
        if (value == null) {
            throw new IllegalStateException("Required enrichment field is missing: " + field);
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

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\u00A0]+", " ")
                .trim();
    }

    private record RuleSet(
            String version,
            String method,
            double minimumScore,
            double minimumMargin,
            List<ClassificationRule> rules) {
    }

    private record ClassificationRule(
            String subjectCode,
            String topicCode,
            String subTopicCode,
            List<String> conceptCodes,
            List<Signal> signals) {
    }

    private record Signal(String value, double weight) {
    }

    private record ScoredRule(
            ClassificationRule rule,
            double score,
            List<String> matchedSignals) {
    }

    private record ClassificationResult(
            String topicCode,
            String subTopicCode,
            List<String> conceptCodes,
            double confidence,
            List<String> matchedSignals) {
    }
}
