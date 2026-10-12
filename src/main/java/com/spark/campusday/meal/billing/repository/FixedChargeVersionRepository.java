package com.spark.campusday.meal.billing.repository;
import com.spark.campusday.meal.billing.model.FixedChargeVersion;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;
public interface FixedChargeVersionRepository extends MongoRepository<FixedChargeVersion, String> {
    Optional<FixedChargeVersion> findFirstByYearAndMonthOrderByEffectiveAtDesc(int year, int month);
    List<FixedChargeVersion> findByYearAndMonthOrderByEffectiveAtDesc(int year, int month);
}
