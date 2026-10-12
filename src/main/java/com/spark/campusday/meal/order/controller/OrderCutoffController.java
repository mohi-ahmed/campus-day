package com.spark.campusday.meal.order.controller;

import com.spark.campusday.meal.order.dto.OrderCutoffRequest;
import com.spark.campusday.meal.order.dto.OrderCutoffResponse;
import com.spark.campusday.meal.order.service.OrderCutoffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Unsecured development configuration API until Meal Manager authorization is available. */
@RestController
@RequestMapping("/api/meal/order-configuration")
@RequiredArgsConstructor
public class OrderCutoffController {
    private final OrderCutoffService service;

    @GetMapping
    public OrderCutoffResponse get(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mealDate) {
        return service.get(mealDate);
    }

    @PutMapping
    public OrderCutoffResponse configure(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mealDate,
            @Valid @RequestBody OrderCutoffRequest request) {
        return service.configure(request, mealDate);
    }
}
