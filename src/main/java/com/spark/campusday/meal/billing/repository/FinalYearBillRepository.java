package com.spark.campusday.meal.billing.repository;
import com.spark.campusday.meal.billing.model.FinalYearBill;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
import java.util.List;
public interface FinalYearBillRepository extends MongoRepository<FinalYearBill, String> {
    List<FinalYearBill> findByYearAndMonth(int year, int month);
    Optional<FinalYearBill> findByStudentIdAndYearAndMonth(String studentId, int year, int month);
}
