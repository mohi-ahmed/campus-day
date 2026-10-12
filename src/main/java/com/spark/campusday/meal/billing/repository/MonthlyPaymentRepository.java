package com.spark.campusday.meal.billing.repository;
import com.spark.campusday.meal.billing.model.MonthlyPayment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface MonthlyPaymentRepository extends MongoRepository<MonthlyPayment, String> {
    List<MonthlyPayment> findByStudentIdAndYearAndMonthOrderByPaymentDateAscRecordedAtAsc(String studentId, int year, int month);
    List<MonthlyPayment> findByYearAndMonthAndChannelOrderByPaymentDateAsc(int year, int month, String channel);
}
