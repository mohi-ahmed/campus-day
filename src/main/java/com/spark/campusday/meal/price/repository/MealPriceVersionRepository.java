package com.spark.campusday.meal.price.repository;

import com.spark.campusday.meal.price.model.MealPriceVersion;
import com.spark.campusday.meal.price.model.PriceTarget;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MealPriceVersionRepository extends MongoRepository<MealPriceVersion, String> {
    void deleteAllByMenuId(String menuId);
    @Query(value = "{'menuId': {'$in': ?0}}", sort = "{'menuId': 1, 'target': 1, 'itemId': 1, 'effectiveAt': -1}")
    List<MealPriceVersion> findForMenusNewestFirst(Collection<String> menuIds);
    @Query(value = "{'menuId': ?0}", sort = "{'target': 1, 'itemId': 1, 'effectiveAt': -1}")
    List<MealPriceVersion> findForMenuNewestFirst(String menuId);

    @Query(value = "{'menuId': ?0, 'target': ?1, 'itemId': ?2}", sort = "{'effectiveAt': -1, '_id': -1}")
    List<MealPriceVersion> findHistory(String menuId, PriceTarget target, String itemId);

    @Query(value = "{'menuId': ?0, 'target': ?1, 'itemId': ?2}", sort = "{'effectiveAt': -1, '_id': -1}")
    Optional<MealPriceVersion> findLatest(String menuId, PriceTarget target, String itemId);

    @Query(value = "{'menuId': ?0, 'target': ?1, 'itemId': ?2, 'effectiveAt': {'$lte': ?3}}",
            sort = "{'effectiveAt': -1, '_id': -1}")
    Optional<MealPriceVersion> findAsOf(String menuId, PriceTarget target, String itemId, Instant asOf);
}
