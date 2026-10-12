package com.spark.campusday.meal.fractionsheet.controller;

import com.spark.campusday.meal.fractionsheet.dto.FractionSheetResponse;
import com.spark.campusday.meal.fractionsheet.service.FractionSheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Development API: explicit manager identity is replaced when Security is added. */
@RestController
@RequestMapping("/api/meal/fraction-sheet")
@RequiredArgsConstructor
public class FractionSheetController {
    private final FractionSheetService service;

    @GetMapping
    public FractionSheetResponse get(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                     @RequestParam String managerStudentId) {
        return service.get(date, managerStudentId);
    }
}
