package com.spark.campusday.careerprep.question.ingest;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.spark.campusday.careerprep.question.dto.QuestionImportItem;
import com.spark.campusday.careerprep.question.dto.QuestionImportRequest;
import com.spark.campusday.careerprep.question.dto.TaxonomyNodeRequest;
import com.spark.campusday.careerprep.question.model.BcsExamType;
import com.spark.campusday.careerprep.question.model.ContentRightsStatus;
import com.spark.campusday.careerprep.question.model.QuestionDifficulty;
import com.spark.campusday.careerprep.question.model.QuestionKind;
import com.spark.campusday.careerprep.question.model.QuestionLanguage;
import com.spark.campusday.careerprep.question.model.QuestionOption;
import com.spark.campusday.careerprep.question.model.QuestionSourceType;
import com.spark.campusday.careerprep.question.model.TaxonomyLevel;
import com.spark.campusday.careerprep.question.service.QuestionImportService;
import com.spark.campusday.careerprep.question.service.QuestionTaxonomyService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = {"app.career-prep.enabled", "app.career-prep.import.sangbitdas.enabled"},
        havingValue = "true")
public class SangbitDasDatasetImportRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SangbitDasDatasetImportRunner.class);
    private static final Pattern EXAM_NUMBER = Pattern.compile("^(\\d+)");
    private static final Pattern BANGLA_CHARACTER = Pattern.compile("[\\u0980-\\u09FF]");
    private static final Pattern LATIN_CHARACTER = Pattern.compile("[A-Za-z]");
    private static final Set<Integer> SPECIAL_BCS = Set.of(39, 42, 48, 49);
    private static final int IMPORT_CHUNK_SIZE = 500;
    private static final String SOURCE_NAME = "SangbitDas/bcs-dataset";
    private static final String SOURCE_URL = "https://github.com/SangbitDas/bcs-dataset";
    private static final String SYLLABUS_VERSION = "BPSC_PRELIMINARY_CURRENT";
    private static final TypeReference<Map<String, Object>> RAW_MAP = new TypeReference<>() { };

    private static final Map<Integer, SubjectDefinition> SUBJECTS = Map.ofEntries(
            Map.entry(1, new SubjectDefinition("BANGLA", "Bangla Language & Literature",
                    "বাংলা ভাষা ও সাহিত্য", 35.0)),
            Map.entry(2, new SubjectDefinition("ENGLISH", "English Language & Literature",
                    "English Language & Literature", 35.0)),
            Map.entry(3, new SubjectDefinition("BANGLADESH_AFFAIRS", "Bangladesh Affairs",
                    "বাংলাদেশ বিষয়াবলি", 30.0)),
            Map.entry(4, new SubjectDefinition("INTERNATIONAL_AFFAIRS", "International Affairs",
                    "আন্তর্জাতিক বিষয়াবলি", 20.0)),
            Map.entry(5, new SubjectDefinition("GEOGRAPHY_ENVIRONMENT_DISASTER",
                    "Geography, Environment & Disaster Management",
                    "ভূগোল, পরিবেশ ও দুর্যোগ ব্যবস্থাপনা", 10.0)),
            Map.entry(6, new SubjectDefinition("GENERAL_SCIENCE", "General Science",
                    "সাধারণ বিজ্ঞান", 15.0)),
            Map.entry(7, new SubjectDefinition("ICT", "Computer & Information Technology",
                    "কম্পিউটার ও তথ্যপ্রযুক্তি", 15.0)),
            Map.entry(8, new SubjectDefinition("MATHEMATICAL_REASONING", "Mathematical Reasoning",
                    "গাণিতিক যুক্তি", 15.0)),
            Map.entry(9, new SubjectDefinition("MENTAL_ABILITY", "Mental Ability",
                    "মানসিক দক্ষতা", 15.0)),
            Map.entry(10, new SubjectDefinition("ETHICS_GOVERNANCE",
                    "Ethics, Values & Good Governance", "নৈতিকতা, মূল্যবোধ ও সুশাসন", 10.0)));

    private final ObjectMapper objectMapper;
    private final QuestionTaxonomyService taxonomyService;
    private final QuestionImportService importService;

    @Value("${app.career-prep.import.sangbitdas.path:data/import/staging/sangbitdas-bcs-dataset/bcs_preliminary_question_bank.json}")
    private String datasetPath;

    @Value("${app.career-prep.import.sangbitdas.batch-prefix:sangbitdas-bcs-dataset-v1}")
    private String batchPrefix;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path path = Path.of(datasetPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("BCS dataset file does not exist: " + path);
        }

        JsonNode root = objectMapper.readTree(path.toFile());
        JsonNode questions = root.path("questions");
        if (!questions.isArray()) {
            throw new IllegalStateException("BCS dataset questions must be an array");
        }

        registerStagingTaxonomy();
        Map<String, ExamDefinition> exams = readExams(root.path("exams"));
        List<QuestionImportItem> chunk = new ArrayList<>(IMPORT_CHUNK_SIZE);
        int part = 1;
        int received = 0;
        int inserted = 0;
        int skipped = 0;
        int failed = 0;

        for (JsonNode question : questions) {
            chunk.add(mapQuestion(question, exams));
            if (chunk.size() == IMPORT_CHUNK_SIZE) {
                var result = importChunk(chunk, part++);
                received += result.receivedCount();
                inserted += result.insertedCount();
                skipped += result.skippedCount();
                failed += result.failedCount();
                chunk = new ArrayList<>(IMPORT_CHUNK_SIZE);
            }
        }
        if (!chunk.isEmpty()) {
            var result = importChunk(chunk, part);
            received += result.receivedCount();
            inserted += result.insertedCount();
            skipped += result.skippedCount();
            failed += result.failedCount();
        }

        log.info("BCS staging import finished: received={}, inserted={}, skipped={}, failed={}",
                received, inserted, skipped, failed);
    }

    private com.spark.campusday.careerprep.question.dto.QuestionImportResponse importChunk(
            List<QuestionImportItem> questions, int part) {
        String batchKey = "%s-part-%02d".formatted(batchPrefix, part);
        QuestionImportRequest request = new QuestionImportRequest(
                batchKey,
                SOURCE_NAME,
                QuestionSourceType.PREVIOUS_BCS,
                SOURCE_URL,
                ContentRightsStatus.REVIEW_REQUIRED,
                "Dataset is not openly licensed; see repository DATA_LICENSE.md",
                "Dataset: SangbitDas/bcs-dataset; underlying source: uttoron.academy/BPSC",
                Instant.now(),
                List.copyOf(questions));
        return importService.importQuestions(request);
    }

    private QuestionImportItem mapQuestion(JsonNode node,
                                           Map<String, ExamDefinition> exams) {
        String examSlug = requiredText(node, "exam_slug");
        ExamDefinition exam = exams.get(examSlug);
        if (exam == null) {
            throw new IllegalStateException("Missing exam metadata for " + examSlug);
        }
        int subjectId = node.path("subject_id").asInt();
        SubjectDefinition subject = SUBJECTS.get(subjectId);
        if (subject == null) {
            throw new IllegalStateException("Unknown source subject id: " + subjectId);
        }
        int questionNumber = node.path("question_number").asInt();
        String questionText = textOrEmpty(node, "question");
        List<QuestionOption> options = List.of(
                new QuestionOption("A", textOrEmpty(node, "option_a")),
                new QuestionOption("B", textOrEmpty(node, "option_b")),
                new QuestionOption("C", textOrEmpty(node, "option_c")),
                new QuestionOption("D", textOrEmpty(node, "option_d")));

        return new QuestionImportItem(
                examSlug + "-q" + questionNumber,
                exam.examType(),
                exam.examNumber(),
                null,
                exam.heldDate(),
                exam.setCode(),
                questionNumber,
                detectLanguage(questionText, options),
                subject.code(),
                subject.code() + "_UNCLASSIFIED",
                null,
                List.of(),
                QuestionKind.OTHER,
                questionText,
                options,
                textOrEmpty(node, "correct_answer").toUpperCase(Locale.ROOT),
                textOrNull(node, "solve_note"),
                QuestionDifficulty.UNKNOWN,
                List.of(),
                List.of(),
                List.of("SANGBITDAS_BCS_DATASET", examSlug.toUpperCase(Locale.ROOT)),
                false,
                null,
                null,
                null,
                null,
                objectMapper.convertValue(node, RAW_MAP));
    }

    private Map<String, ExamDefinition> readExams(JsonNode examsNode) {
        if (!examsNode.isArray()) {
            throw new IllegalStateException("BCS dataset exams must be an array");
        }
        Map<String, ExamDefinition> exams = new HashMap<>();
        for (JsonNode node : examsNode) {
            String slug = requiredText(node, "slug");
            Matcher matcher = EXAM_NUMBER.matcher(slug);
            if (!matcher.find()) {
                throw new IllegalStateException("Cannot parse BCS exam number from " + slug);
            }
            int number = Integer.parseInt(matcher.group(1));
            exams.put(slug, new ExamDefinition(
                    number,
                    SPECIAL_BCS.contains(number) ? BcsExamType.SPECIAL : BcsExamType.GENERAL,
                    LocalDate.parse(requiredText(node, "date")),
                    textOrNull(node, "set_code")));
        }
        return Map.copyOf(exams);
    }

    private void registerStagingTaxonomy() {
        List<TaxonomyNodeRequest> nodes = new ArrayList<>();
        SUBJECTS.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    SubjectDefinition subject = entry.getValue();
                    nodes.add(new TaxonomyNodeRequest(
                            subject.code(), null, TaxonomyLevel.SUBJECT,
                            subject.nameEn(), subject.nameBn(), subject.syllabusMarks(),
                            SYLLABUS_VERSION, entry.getKey(), true));
                    nodes.add(new TaxonomyNodeRequest(
                            subject.code() + "_UNCLASSIFIED", subject.code(), TaxonomyLevel.TOPIC,
                            "Unclassified", "শ্রেণিবিন্যাস বাকি", null,
                            SYLLABUS_VERSION, 999, true));
                });
        taxonomyService.upsert(nodes);
    }

    private QuestionLanguage detectLanguage(String questionText,
                                            List<QuestionOption> options) {
        StringBuilder content = new StringBuilder(questionText);
        options.forEach(option -> content.append(' ').append(option.text()));
        boolean bangla = BANGLA_CHARACTER.matcher(content).find();
        boolean latin = LATIN_CHARACTER.matcher(content).find();
        if (bangla && latin) {
            return QuestionLanguage.BILINGUAL;
        }
        return bangla ? QuestionLanguage.BANGLA : QuestionLanguage.ENGLISH;
    }

    private String requiredText(JsonNode node, String field) {
        String value = textOrNull(node, field);
        if (value == null) {
            throw new IllegalStateException("Required dataset field is missing: " + field);
        }
        return value;
    }

    private String textOrEmpty(JsonNode node, String field) {
        String value = textOrNull(node, field);
        return value == null ? "" : value;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }

    private record SubjectDefinition(
            String code,
            String nameEn,
            String nameBn,
            Double syllabusMarks) {
    }

    private record ExamDefinition(
            int examNumber,
            BcsExamType examType,
            LocalDate heldDate,
            String setCode) {
    }
}
