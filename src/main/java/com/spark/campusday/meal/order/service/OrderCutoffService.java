package com.spark.campusday.meal.order.service;

import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.meal.config.MealTimeConfiguration;
import com.spark.campusday.meal.order.dto.OrderCutoffRequest;
import com.spark.campusday.meal.order.dto.OrderCutoffResponse;
import com.spark.campusday.meal.order.model.OrderCutoffConfiguration;
import com.spark.campusday.meal.order.repository.OrderCutoffConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class OrderCutoffService {
    private final OrderCutoffConfigurationRepository configurations;
    @Qualifier("mealClock")
    private final Clock mealClock;

    public OrderCutoffResponse configure(OrderCutoffRequest request, LocalDate mealDate) {
        if (mealDate == null || request == null || request.orderCutoff() == null || request.editCutoff() == null
                || request.cancelCutoff() == null) {
            throw new InvalidBusinessStateException("Meal date and order, edit and cancel cutoffs are required");
        }
        var order = rule(request.orderCutoff());
        var edit = rule(request.editCutoff());
        var cancel = rule(request.cancelCutoff());
        OrderCutoffConfiguration configuration = configurations.findById(OrderCutoffConfiguration.ID)
                .orElseGet(() -> new OrderCutoffConfiguration(order, edit, cancel));
        configuration.revise(order, edit, cancel);
        return response(configurations.save(configuration), mealDate);
    }

    public OrderCutoffResponse get(LocalDate mealDate) {
        return response(requireConfiguration(), mealDate);
    }

    public OrderCutoffConfiguration requireConfiguration() {
        return configurations.findById(OrderCutoffConfiguration.ID)
                .orElseThrow(() -> new InvalidBusinessStateException("Final Year order cutoffs are not configured"));
    }

    public void requireOpen(LocalDate mealDate, OrderCutoffConfiguration.CutoffRule rule, String action) {
        if (!Instant.now(mealClock).isBefore(deadline(mealDate, rule))) {
            throw new InvalidBusinessStateException(action + " cutoff has passed");
        }
    }

    public boolean isOpen(LocalDate mealDate, OrderCutoffConfiguration.CutoffRule rule) {
        return Instant.now(mealClock).isBefore(deadline(mealDate, rule));
    }

    public Instant deadline(LocalDate mealDate, OrderCutoffConfiguration.CutoffRule rule) {
        return mealDate.minusDays(rule.daysBeforeMeal()).atTime(rule.time())
                .atZone(MealTimeConfiguration.BUSINESS_ZONE).toInstant();
    }

    public OrderCutoffResponse response(OrderCutoffConfiguration configuration, LocalDate mealDate) {
        if (mealDate == null) {
            throw new InvalidBusinessStateException("Meal date is required");
        }
        return new OrderCutoffResponse(mealDate, configuration.getOrderCutoff(),
                configuration.getEditCutoff(), configuration.getCancelCutoff(),
                deadline(mealDate, configuration.getOrderCutoff()),
                deadline(mealDate, configuration.getEditCutoff()),
                deadline(mealDate, configuration.getCancelCutoff()), configuration.getUpdatedAt());
    }

    private OrderCutoffConfiguration.CutoffRule rule(OrderCutoffRequest.Rule input) {
        if (input.time() == null || input.daysBeforeMeal() < 0) {
            throw new InvalidBusinessStateException("Cutoff time and nonnegative days before meal are required");
        }
        return new OrderCutoffConfiguration.CutoffRule(input.daysBeforeMeal(), input.time());
    }
}
