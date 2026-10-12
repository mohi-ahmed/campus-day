package com.spark.campusday.meal.purchase.repository;

import com.spark.campusday.meal.purchase.model.Purchase;
import com.spark.campusday.meal.purchase.model.PurchasePaymentMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface PurchaseSearchRepository {
    Page<Purchase> search(String supplierId, LocalDate from, LocalDate to,
                          PurchasePaymentMode mode, Pageable pageable);
}
