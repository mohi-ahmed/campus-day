package com.spark.campusday.meal.price.controller;

import com.spark.campusday.meal.price.dto.ApplicablePriceResponse;
import com.spark.campusday.meal.price.dto.PriceVersionResponse;
import com.spark.campusday.meal.price.dto.SetPriceRequest;
import com.spark.campusday.meal.price.model.PriceTarget;
import com.spark.campusday.meal.price.service.MealPriceService;
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
import java.util.List;

/** Development API: mutation actor is explicit until authenticated identity is available. */
@RestController
@RequestMapping("/api/meal/prices")
@RequiredArgsConstructor
public class MealPriceController {
    private final MealPriceService service;

    @PutMapping
    public PriceVersionResponse set(@RequestParam String managerStudentId,
                                    @Valid @RequestBody SetPriceRequest request) {
        return service.setPrice(managerStudentId, request);
    }

    @GetMapping
    public ApplicablePriceResponse applicable(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mealDate) {
        return service.applicable(mealDate);
    }

    @GetMapping("/history")
    public List<PriceVersionResponse> history(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mealDate,
            @RequestParam PriceTarget target,
            @RequestParam(required = false) String itemId) {
        return service.history(mealDate, target, itemId);
    }
}
