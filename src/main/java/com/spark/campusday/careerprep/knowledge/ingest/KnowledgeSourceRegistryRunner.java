package com.spark.campusday.careerprep.knowledge.ingest;

import com.spark.campusday.careerprep.knowledge.model.KnowledgeAuthority;
import com.spark.campusday.careerprep.knowledge.model.KnowledgeIngestionStatus;
import com.spark.campusday.careerprep.knowledge.model.KnowledgeSource;
import com.spark.campusday.careerprep.knowledge.model.KnowledgeSourceType;
import com.spark.campusday.careerprep.knowledge.repository.KnowledgeSectionRepository;
import com.spark.campusday.careerprep.knowledge.repository.KnowledgeSourceRepository;
import com.spark.campusday.careerprep.question.model.ContentRightsStatus;
import com.spark.campusday.careerprep.question.model.QuestionLanguage;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Order(300)
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = {"app.career-prep.enabled", "app.career-prep.knowledge.registry.enabled"},
        havingValue = "true")
public class KnowledgeSourceRegistryRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeSourceRegistryRunner.class);
    private static final Set<String> SUBJECT_CODES = Set.of(
            "BANGLA", "ENGLISH", "BANGLADESH_AFFAIRS", "INTERNATIONAL_AFFAIRS",
            "GEOGRAPHY_ENVIRONMENT_DISASTER", "GENERAL_SCIENCE", "ICT",
            "MATHEMATICAL_REASONING", "MENTAL_ABILITY", "ETHICS_GOVERNANCE");

    private final ObjectMapper objectMapper;
    private final KnowledgeSourceRepository sources;
    private final KnowledgeSectionRepository sections;

    @Value("${app.career-prep.knowledge.registry.resource:data/bcs/bcs-knowledge-source-registry-v1.json}")
    private String registryResource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        JsonNode root = readResource();
        String registryVersion = requiredText(root, "registryVersion");
        String taxonomyVersion = requiredText(root, "taxonomyVersion");
        JsonNode sourceNodes = root.path("sources");
        if (!sourceNodes.isArray()) {
            throw new IllegalStateException("BCS knowledge registry sources must be an array");
        }

        Map<String, KnowledgeSource> existing = new HashMap<>();
        sources.findAll().forEach(source -> existing.put(source.getSourceId(), source));
        Set<String> seen = new HashSet<>();
        List<KnowledgeSource> changed = new ArrayList<>();
        int inserted = 0;
        int updated = 0;

        for (JsonNode node : sourceNodes) {
            String sourceId = requiredText(node, "sourceId");
            if (!seen.add(sourceId)) {
                throw new IllegalStateException("Duplicate knowledge source id: " + sourceId);
            }
            List<String> subjectCodes = readSubjectCodes(node.path("subjectCodes"), sourceId);
            KnowledgeSource source = existing.get(sourceId);
            boolean isNew = source == null;
            if (isNew) {
                source = new KnowledgeSource();
                source.setSourceId(sourceId);
            }

            KnowledgeIngestionStatus requestedStatus = KnowledgeIngestionStatus.valueOf(
                    requiredText(node, "ingestionStatus"));
            source.setSourceType(KnowledgeSourceType.valueOf(requiredText(node, "sourceType")));
            source.setAuthority(KnowledgeAuthority.valueOf(requiredText(node, "authority")));
            source.setTitle(requiredText(node, "title"));
            source.setPublisher(requiredText(node, "publisher"));
            source.setEdition(textOrNull(node, "edition"));
            source.setAcademicYear(textOrNull(node, "academicYear"));
            source.setLanguage(QuestionLanguage.valueOf(requiredText(node, "language")));
            source.setOfficialLandingPageUrl(requiredText(node, "officialLandingPageUrl"));
            source.setDownloadUrl(textOrNull(node, "downloadUrl"));
            source.setStorageUri(preferExisting(source.getStorageUri(), textOrNull(node, "storageUri")));
            source.setMediaType(requiredText(node, "mediaType"));
            source.setSha256(preferExisting(source.getSha256(), textOrNull(node, "sha256")));
            source.setRightsStatus(ContentRightsStatus.valueOf(requiredText(node, "rightsStatus")));
            source.setRightsNote(requiredText(node, "rightsNote"));
            source.setProvenanceNote(requiredText(node, "provenanceNote")
                    + " Registry: " + registryVersion);
            source.setSubjectCodes(subjectCodes);
            source.setTaxonomyVersion(taxonomyVersion);
            source.setIngestionStatus(mostAdvanced(source.getIngestionStatus(), requestedStatus));
            applyEligibility(source);
            changed.add(source);
            if (isNew) {
                inserted++;
            } else {
                updated++;
            }
        }

        sources.saveAll(changed);
        log.info("BCS knowledge registry imported: version={}, total={}, inserted={}, updated={}, "
                        + "sourceMetadataCount={}, retainedSectionCount={}",
                registryVersion, changed.size(), inserted, updated, sources.count(), sections.count());
    }

    private void applyEligibility(KnowledgeSource source) {
        boolean approved = source.getIngestionStatus() == KnowledgeIngestionStatus.APPROVED;
        source.setVerificationEligible(approved
                && source.getAuthority() == KnowledgeAuthority.PRIMARY_OFFICIAL);
        source.setTrainingEligible(approved
                && source.getRightsStatus() == ContentRightsStatus.CLEARED
                && source.getSha256() != null);
    }

    private KnowledgeIngestionStatus mostAdvanced(KnowledgeIngestionStatus current,
                                                   KnowledgeIngestionStatus requested) {
        if (current == null || current == KnowledgeIngestionStatus.FAILED) {
            return requested;
        }
        return current.ordinal() >= requested.ordinal() ? current : requested;
    }

    private List<String> readSubjectCodes(JsonNode node, String sourceId) {
        if (!node.isArray() || node.isEmpty()) {
            throw new IllegalStateException("Knowledge source requires subjects: " + sourceId);
        }
        List<String> result = new ArrayList<>();
        node.forEach(value -> {
            String code = value.asText();
            if (!SUBJECT_CODES.contains(code)) {
                throw new IllegalStateException(
                        "Unknown knowledge-source subject " + code + " for " + sourceId);
            }
            result.add(code);
        });
        return List.copyOf(result);
    }

    private JsonNode readResource() throws Exception {
        ClassPathResource resource = new ClassPathResource(registryResource);
        if (!resource.exists()) {
            throw new IllegalStateException(
                    "BCS knowledge registry resource not found: " + registryResource);
        }
        try (var input = resource.getInputStream()) {
            return objectMapper.readTree(input);
        }
    }

    private String preferExisting(String existing, String requested) {
        return existing == null || existing.isBlank() ? requested : existing;
    }

    private String requiredText(JsonNode node, String field) {
        String value = textOrNull(node, field);
        if (value == null) {
            throw new IllegalStateException("Required knowledge-source field is missing: " + field);
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
