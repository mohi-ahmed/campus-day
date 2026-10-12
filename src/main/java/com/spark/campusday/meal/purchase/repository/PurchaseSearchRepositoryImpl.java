package com.spark.campusday.meal.purchase.repository;

import com.spark.campusday.meal.purchase.model.Purchase;
import com.spark.campusday.meal.purchase.model.PurchasePaymentMode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class PurchaseSearchRepositoryImpl implements PurchaseSearchRepository {
    private final MongoTemplate mongo;

    @Override
    public Page<Purchase> search(String supplierId, LocalDate from, LocalDate to,
                                 PurchasePaymentMode mode, Pageable pageable) {
        List<Criteria> criteria = new ArrayList<>();
        if (supplierId != null && !supplierId.isBlank()) criteria.add(Criteria.where("supplierId").is(supplierId));
        if (from != null || to != null) {
            Criteria date = Criteria.where("purchaseDate");
            if (from != null) date.gte(from);
            if (to != null) date.lte(to);
            criteria.add(date);
        }
        if (mode != null) criteria.add(Criteria.where("paymentMode").is(mode));
        Query query = criteria.isEmpty() ? new Query() : new Query(new Criteria().andOperator(criteria));
        long count = mongo.count(query, Purchase.class);
        query.with(pageable);
        return new PageImpl<>(mongo.find(query, Purchase.class), pageable, count);
    }
}
