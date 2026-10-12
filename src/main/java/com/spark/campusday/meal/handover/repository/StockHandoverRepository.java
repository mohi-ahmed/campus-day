package com.spark.campusday.meal.handover.repository;

import com.spark.campusday.meal.handover.model.StockHandover;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface StockHandoverRepository extends MongoRepository<StockHandover, String> {
    Optional<StockHandover> findByRequestId(String requestId);
    Optional<StockHandover> findByOutgoingPeriodId(String outgoingPeriodId);
    List<StockHandover> findByOutgoingPeriodIdOrIncomingPeriodIdOrderByHandoverDateDesc(String outgoingPeriodId, String incomingPeriodId);
    List<StockHandover> findAllByOrderByHandoverDateDesc();
}
