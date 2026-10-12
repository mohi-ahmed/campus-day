package com.spark.campusday.careerprep.question.config;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;

import java.util.ArrayList;
import java.util.stream.StreamSupport;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.career-prep.enabled", havingValue = "true")
public class BcsMongoIndexMigrationConfiguration {
    private static final Logger log = LoggerFactory.getLogger(
            BcsMongoIndexMigrationConfiguration.class);
    private static final String BCS_QUESTION_COLLECTION = "bcs_questions";
    private static final String LEGACY_PREDICTION_INDEX = "question_prediction_export";

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory databaseFactory,
                                       MongoConverter converter) {
        MongoDatabase database = databaseFactory.getMongoDatabase();
        removeLegacyPredictionIndex(database);
        return new MongoTemplate(databaseFactory, converter);
    }

    private void removeLegacyPredictionIndex(MongoDatabase database) {
        boolean collectionExists = StreamSupport.stream(
                        database.listCollectionNames().spliterator(), false)
                .anyMatch(BCS_QUESTION_COLLECTION::equals);
        if (!collectionExists) {
            return;
        }

        var collection = database.getCollection(BCS_QUESTION_COLLECTION);
        boolean legacyIndexExists = collection.listIndexes()
                .into(new ArrayList<Document>())
                .stream()
                .anyMatch(index -> LEGACY_PREDICTION_INDEX.equals(index.getString("name")));
        if (!legacyIndexExists) {
            return;
        }

        collection.dropIndex(LEGACY_PREDICTION_INDEX);
        log.info("Removed obsolete MongoDB index {} before current index initialization",
                LEGACY_PREDICTION_INDEX);
    }
}
