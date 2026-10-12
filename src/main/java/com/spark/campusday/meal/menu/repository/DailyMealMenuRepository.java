package com.spark.campusday.meal.menu.repository;

import com.spark.campusday.meal.menu.model.DailyMealMenu;
import com.spark.campusday.meal.menu.model.MenuStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyMealMenuRepository extends MongoRepository<DailyMealMenu, String> {
    Optional<DailyMealMenu> findByMealDate(LocalDate mealDate);

    Optional<DailyMealMenu> findByMealDateAndStatus(LocalDate mealDate, MenuStatus status);

    @Query(value = "{ 'status': ?0, 'mealDate': { '$gte': ?1, '$lte': ?2 } }", sort = "{ 'mealDate': 1 }")
    List<DailyMealMenu> findPublishedInRange(MenuStatus status, LocalDate from, LocalDate to);

    @Query(value = "{ 'mealDate': { '$gte': ?0, '$lte': ?1 } }", sort = "{ 'mealDate': 1 }")
    List<DailyMealMenu> findManagedInRange(LocalDate from, LocalDate to);

    boolean existsByMealDate(LocalDate mealDate);

    boolean existsByMealDateAndIdNot(LocalDate mealDate, String id);
}
