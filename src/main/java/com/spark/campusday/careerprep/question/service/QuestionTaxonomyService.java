package com.spark.campusday.careerprep.question.service;

import com.spark.campusday.careerprep.question.dto.TaxonomyNodeRequest;
import com.spark.campusday.careerprep.question.dto.TaxonomyNodeResponse;
import com.spark.campusday.careerprep.question.model.BcsQuestion;
import com.spark.campusday.careerprep.question.model.BcsTaxonomyNode;
import com.spark.campusday.careerprep.question.model.TaxonomyLevel;
import com.spark.campusday.careerprep.question.repository.BcsTaxonomyNodeRepository;
import com.spark.campusday.common.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionTaxonomyService {
    private final BcsTaxonomyNodeRepository nodes;

    public List<TaxonomyNodeResponse> upsert(List<TaxonomyNodeRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new InvalidRequestException("At least one taxonomy node is required");
        }
        Map<String, BcsTaxonomyNode> known = new HashMap<>();
        nodes.findAll().forEach(node -> known.put(node.getCode(), node));

        List<TaxonomyNodeRequest> ordered = requests.stream()
                .sorted(Comparator.comparing(request -> request.level().ordinal()))
                .toList();
        Map<String, BcsTaxonomyNode> changed = new LinkedHashMap<>();
        for (TaxonomyNodeRequest request : ordered) {
            String code = normalizeCode(request.code(), request.level());
            String parentCode = normalizeParent(request.parentCode());
            requireValidParent(request.level(), parentCode, known);

            BcsTaxonomyNode node = known.getOrDefault(code, new BcsTaxonomyNode());
            node.setCode(code);
            node.setParentCode(parentCode);
            node.setLevel(request.level());
            node.setNameEn(request.nameEn().trim());
            node.setNameBn(normalize(request.nameBn()));
            node.setSyllabusMarks(request.syllabusMarks());
            node.setSyllabusVersion(request.syllabusVersion().trim());
            node.setDisplayOrder(request.displayOrder());
            node.setActive(request.active());
            known.put(code, node);
            changed.put(code, node);
        }
        return nodes.saveAll(changed.values()).stream().map(this::response).toList();
    }

    public List<TaxonomyNodeResponse> children(String parentCode) {
        String normalizedParent = normalizeParent(parentCode);
        return nodes.findByParentCodeAndActiveTrueOrderByDisplayOrderAscCodeAsc(normalizedParent)
                .stream().map(this::response).toList();
    }

    public Map<String, BcsTaxonomyNode> activeNodeMap() {
        Map<String, BcsTaxonomyNode> result = new LinkedHashMap<>();
        nodes.findByActiveTrue().forEach(node -> result.put(node.getCode(), node));
        return Map.copyOf(result);
    }

    public void validateQuestion(BcsQuestion question,
                                 Map<String, BcsTaxonomyNode> activeNodes) {
        BcsTaxonomyNode subject = requireNode(
                question.getSubjectCode(), TaxonomyLevel.SUBJECT, activeNodes);
        BcsTaxonomyNode topic = requireNode(
                question.getTopicCode(), TaxonomyLevel.TOPIC, activeNodes);
        requireParent(topic, subject.getCode());

        String expectedConceptParent = topic.getCode();
        if (question.getSubTopicCode() != null) {
            BcsTaxonomyNode subTopic = requireNode(
                    question.getSubTopicCode(), TaxonomyLevel.SUBTOPIC, activeNodes);
            requireParent(subTopic, topic.getCode());
            expectedConceptParent = subTopic.getCode();
        }
        for (String conceptCode : question.getConceptCodes()) {
            BcsTaxonomyNode concept = requireNode(
                    conceptCode, TaxonomyLevel.CONCEPT, activeNodes);
            boolean belongsToTopic = topic.getCode().equals(concept.getParentCode());
            boolean belongsToSubTopic = expectedConceptParent.equals(concept.getParentCode());
            if (!belongsToTopic && !belongsToSubTopic) {
                throw new InvalidRequestException(
                        "Concept " + conceptCode + " does not belong to question topic hierarchy");
            }
        }
    }

    private BcsTaxonomyNode requireNode(String code, TaxonomyLevel level,
                                        Map<String, BcsTaxonomyNode> activeNodes) {
        BcsTaxonomyNode node = activeNodes.get(code);
        if (node == null || node.getLevel() != level) {
            throw new InvalidRequestException(
                    "Unknown or inactive " + level + " taxonomy code: " + code);
        }
        return node;
    }

    private void requireParent(BcsTaxonomyNode node, String expectedParent) {
        if (!expectedParent.equals(node.getParentCode())) {
            throw new InvalidRequestException(
                    "Taxonomy node " + node.getCode() + " must belong to " + expectedParent);
        }
    }

    private void requireValidParent(TaxonomyLevel level, String parentCode,
                                    Map<String, BcsTaxonomyNode> known) {
        if (level == TaxonomyLevel.SUBJECT) {
            if (parentCode != null) {
                throw new InvalidRequestException("SUBJECT taxonomy nodes cannot have a parent");
            }
            return;
        }
        if (parentCode == null) {
            throw new InvalidRequestException(level + " taxonomy nodes require a parent");
        }
        BcsTaxonomyNode parent = known.get(parentCode);
        if (parent == null) {
            throw new InvalidRequestException("Unknown taxonomy parent: " + parentCode);
        }
        if (!parent.isActive()) {
            throw new InvalidRequestException("Taxonomy parent is inactive: " + parentCode);
        }
        boolean valid = switch (level) {
            case TOPIC -> parent.getLevel() == TaxonomyLevel.SUBJECT;
            case SUBTOPIC -> parent.getLevel() == TaxonomyLevel.TOPIC;
            case CONCEPT -> parent.getLevel() == TaxonomyLevel.TOPIC
                    || parent.getLevel() == TaxonomyLevel.SUBTOPIC;
            case SUBJECT -> false;
        };
        if (!valid) {
            throw new InvalidRequestException(
                    "Invalid parent level for taxonomy node " + level);
        }
    }

    private String normalizeCode(String value, TaxonomyLevel level) {
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replaceAll(level == TaxonomyLevel.CONCEPT
                        ? "[^A-Z0-9_.]+" : "[^A-Z0-9]+", "_")
                .replaceAll("^[_.]+|[_.]+$", "");
        if (normalized.length() < 2) {
            throw new InvalidRequestException("Invalid taxonomy code: " + value);
        }
        return normalized;
    }

    private String normalizeParent(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9_.]+", "_")
                .replaceAll("^[_.]+|[_.]+$", "");
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private TaxonomyNodeResponse response(BcsTaxonomyNode node) {
        return new TaxonomyNodeResponse(node.getCode(), node.getParentCode(), node.getLevel(),
                node.getNameEn(), node.getNameBn(), node.getSyllabusMarks(),
                node.getSyllabusVersion(), node.getDisplayOrder(), node.isActive());
    }
}
