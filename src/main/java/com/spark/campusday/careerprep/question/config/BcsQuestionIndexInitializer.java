package com.spark.campusday.careerprep.question.config;

import com.spark.campusday.careerprep.knowledge.model.KnowledgeSection;
import com.spark.campusday.careerprep.knowledge.model.KnowledgeSource;
import com.spark.campusday.careerprep.question.model.BcsQuestion;
import com.spark.campusday.careerprep.question.model.BcsTaxonomyNode;
import com.spark.campusday.careerprep.question.model.QuestionImportBatch;
import com.spark.campusday.careerprep.question.model.QuestionAnswerVerification;
import com.spark.campusday.careerprep.question.model.QuestionClassificationProposal;
import com.spark.campusday.careerprep.question.model.RawQuestionImportRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.career-prep.enabled", havingValue = "true")
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class BcsQuestionIndexInitializer implements ApplicationRunner {
    private static final List<Class<?>> INDEXED_TYPES = List.of(
            BcsQuestion.class,
            BcsTaxonomyNode.class,
            QuestionImportBatch.class,
            RawQuestionImportRecord.class,
            QuestionClassificationProposal.class,
            QuestionAnswerVerification.class,
            KnowledgeSource.class,
            KnowledgeSection.class);

    private final MongoTemplate mongo;

    @Override
    public void run(ApplicationArguments args) {
        var resolver = new MongoPersistentEntityIndexResolver(
                mongo.getConverter().getMappingContext());
        for (Class<?> indexedType : INDEXED_TYPES) {
            var operations = mongo.indexOps(indexedType);
            resolver.resolveIndexFor(indexedType).forEach(operations::createIndex);
        }
    }
}
