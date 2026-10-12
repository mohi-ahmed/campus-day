package com.spark.campusday.currentaffairs.repository;

import com.spark.campusday.currentaffairs.model.CurrentAffairsArticle;
import com.spark.campusday.currentaffairs.model.CurrentAffairsProvider;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface CurrentAffairsArticleRepository extends MongoRepository<CurrentAffairsArticle, String> {
    List<CurrentAffairsArticle> findAllByProviderAndExternalArticleIdIn(
            CurrentAffairsProvider provider,
            Collection<String> externalArticleIds);
}
