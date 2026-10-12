package com.spark.campusday.meal.order.repository;

import com.spark.campusday.meal.order.model.FinalYearMealOrder;
import com.spark.campusday.meal.order.model.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FinalYearMealOrderRepository extends MongoRepository<FinalYearMealOrder, String> {
    @Query(value = "{'mealDate': {'$gte': ?0, '$lte': ?1}}")
    List<FinalYearMealOrder> findInMonth(LocalDate from, LocalDate to);
    Optional<FinalYearMealOrder> findByStudentIdAndMealDate(String studentId, LocalDate mealDate);
    boolean existsByMenuId(String menuId);
    boolean existsByMenuIdAndStatus(String menuId, OrderStatus status);

    List<FinalYearMealOrder> findByStudentIdInAndMealDate(Collection<String> studentIds, LocalDate mealDate);

    @Query(value = "{'studentId': ?0, 'mealDate': {'$gte': ?1, '$lte': ?2}}", sort = "{'mealDate': -1}")
    List<FinalYearMealOrder> findHistory(String studentId, LocalDate from, LocalDate to);
}
