package com.spark.campusday.meal.supplier.controller;

import com.spark.campusday.meal.supplier.dto.SupplierDtos.*;
import com.spark.campusday.meal.supplier.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Unsecured development API; caller identity will come from Security in a later phase. */
@RestController
@RequestMapping("/api/meal/suppliers")
@RequiredArgsConstructor
public class SupplierController {
    private final SupplierService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(@RequestParam String managerStudentId, @Valid @RequestBody SupplierRequest request) {
        return service.create(managerStudentId, request);
    }

    @GetMapping
    public SupplierPage list(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return service.list(page, size);
    }

    @GetMapping("/{id}")
    public SupplierResponse get(@PathVariable String id) { return service.get(id); }

    @PutMapping("/{id}")
    public SupplierResponse update(@RequestParam String managerStudentId, @PathVariable String id,
                                   @Valid @RequestBody SupplierRequest request) {
        return service.update(managerStudentId, id, request);
    }

    @GetMapping("/{id}/dues")
    public DueResponse due(@PathVariable String id) { return service.due(id); }

    @GetMapping("/{id}/payments")
    public PaymentPage payments(@PathVariable String id,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        return service.payments(id, page, size);
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountEntryResponse pay(@RequestParam String managerStudentId, @PathVariable String id,
                                    @Valid @RequestBody PaymentRequest request) {
        return service.pay(managerStudentId, id, request);
    }
}
