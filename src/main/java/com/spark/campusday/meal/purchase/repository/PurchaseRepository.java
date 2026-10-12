package com.spark.campusday.meal.purchase.repository;

import com.spark.campusday.meal.purchase.model.Purchase;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.time.LocalDate;
import java.util.List;

public interface PurchaseRepository extends MongoRepository<Purchase, String>, PurchaseSearchRepository {
    List<Purchase> findByPurchaseDateBetween(LocalDate from, LocalDate to);
    Optional<Purchase> findByRequestId(String requestId);
}
