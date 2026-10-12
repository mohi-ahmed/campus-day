package com.spark.campusday.meal.supplier.repository;

import com.spark.campusday.meal.supplier.model.SupplierAccountEntry;
import com.spark.campusday.meal.supplier.model.SupplierAccountEntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SupplierAccountEntryRepository extends MongoRepository<SupplierAccountEntry, String> {
    List<SupplierAccountEntry> findByBusinessDateBetween(LocalDate from, LocalDate to);
    Optional<SupplierAccountEntry> findFirstBySupplierIdOrderBySequenceDesc(String supplierId);
    Optional<SupplierAccountEntry> findBySourceReference(String sourceReference);
    Page<SupplierAccountEntry> findBySupplierIdAndTypeOrderByBusinessDateDescSequenceDesc(
            String supplierId, SupplierAccountEntryType type, Pageable pageable);

    @Aggregation(pipeline = {
            "{'$sort': {'supplierId': 1, 'sequence': -1}}",
            "{'$group': {'_id': '$supplierId', 'latestBalance': {'$first': '$balanceAfter'}}}"
    })
    List<BalanceSnapshot> latestBalances();

    record BalanceSnapshot(@Field("_id") String id, BigDecimal latestBalance) { }
}
