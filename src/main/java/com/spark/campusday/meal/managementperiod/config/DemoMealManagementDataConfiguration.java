package com.spark.campusday.meal.managementperiod.config;

import com.spark.campusday.meal.managementperiod.model.MealManagementPeriod;
import com.spark.campusday.meal.managementperiod.repository.MealManagementPeriodRepository;
import com.spark.campusday.meal.order.model.OrderCutoffConfiguration;
import com.spark.campusday.meal.order.repository.OrderCutoffConfigurationRepository;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@Profile("!prod")
@ConditionalOnProperty(
        name = {"app.demo.seed-students", "app.demo.seed-meal-management"},
        havingValue = "true"
)
public class DemoMealManagementDataConfiguration {
    static final List<String> DEMO_MANAGER_REGISTRATION_IDS = List.of("jimc-23011", "jimc-23012");

    @Bean
    @DependsOn("demoStudentSeeder")
    ApplicationRunner demoMealManagementSeeder(MealManagementPeriodRepository periods,
                                                OrderCutoffConfigurationRepository cutoffConfigurations,
                                                StudentRepository students,
                                                @Qualifier("mealClock") Clock mealClock) {
        return arguments -> {
            YearMonth month = YearMonth.now(mealClock);
            if (periods.findByYearAndMonth(month.getYear(), month.getMonthValue()).isEmpty()) {
                List<String> managerDocumentIds = DEMO_MANAGER_REGISTRATION_IDS.stream()
                        .map(registrationId -> students.findByStudentId(registrationId)
                                .orElseThrow(() -> new IllegalStateException(
                                        "Demo Meal Manager student is missing: " + registrationId)))
                        .map(Student::getId)
                        .toList();

                periods.save(new MealManagementPeriod(month.getYear(), month.getMonthValue(),
                        month.atDay(1), month.atEndOfMonth(), managerDocumentIds));
            }

            if (cutoffConfigurations.findById(OrderCutoffConfiguration.ID).isEmpty()) {
                cutoffConfigurations.save(new OrderCutoffConfiguration(
                        new OrderCutoffConfiguration.CutoffRule(0, LocalTime.of(23, 0)),
                        new OrderCutoffConfiguration.CutoffRule(0, LocalTime.of(23, 30)),
                        new OrderCutoffConfiguration.CutoffRule(0, LocalTime.of(23, 45))));
            }
        };
    }
}
