package com.spark.campusday.meal.inventory.repository;

import com.spark.campusday.meal.inventory.model.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockMovementRepository extends MongoRepository<StockMovement, String> {
    @Query(value = "{'businessDate': {'$gte': ?0, '$lte': ?1}}")
    List<StockMovement> findInMonth(LocalDate from, LocalDate to);
    Optional<StockMovement> findBySourceReference(String sourceReference);
    Optional<StockMovement> findFirstByIngredientIdOrderBySequenceDesc(String ingredientId);

    Page<StockMovement> findByIngredientIdOrderBySequenceDesc(String ingredientId, Pageable pageable);

    @Query(value = "{'type': 'KITCHEN_ISSUE', 'businessDate': ?0}", sort = "{'occurredAt': -1}")
    Page<StockMovement> findIssuesForDate(LocalDate date, Pageable pageable);

    @Aggregation(pipeline = {
            "{'$sort': {'ingredientId': 1, 'sequence': -1}}",
            "{'$group': {'_id': '$ingredientId', 'balanceAfter': {'$first': '$balanceAfter'}}}"
    })
    List<BalanceSnapshot> latestBalances();

    record BalanceSnapshot(String id, BigDecimal balanceAfter) {}
}
