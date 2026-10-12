package com.spark.campusday.meal.purchase.controller;

import com.spark.campusday.meal.purchase.dto.PurchaseDtos.*;
import com.spark.campusday.meal.purchase.model.PurchasePaymentMode;
import com.spark.campusday.meal.purchase.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Unsecured development API; caller identity will come from Security in a later phase. */
@RestController
@RequestMapping("/api/meal/purchases")
@RequiredArgsConstructor
public class PurchaseController {
    private final PurchaseService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseResponse create(@RequestParam String managerStudentId, @Valid @RequestBody CreateRequest request) {
        return service.create(managerStudentId, request);
    }

    @GetMapping
    public PurchasePage list(@RequestParam(required = false) String supplierId,
                             @RequestParam(required = false) LocalDate from,
                             @RequestParam(required = false) LocalDate to,
                             @RequestParam(required = false) PurchasePaymentMode paymentMode,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return service.list(supplierId, from, to, paymentMode, page, size);
    }

    @GetMapping("/{id}")
    public PurchaseResponse get(@PathVariable String id) { return service.get(id); }

    @PostMapping("/{id}/resume")
    public PurchaseResponse resume(@RequestParam String managerStudentId, @PathVariable String id) {
        return service.resume(managerStudentId, id);
    }
}
