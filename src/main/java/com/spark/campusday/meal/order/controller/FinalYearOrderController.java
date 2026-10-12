package com.spark.campusday.meal.order.controller;

import com.spark.campusday.meal.order.dto.OrderRequest;
import com.spark.campusday.meal.order.dto.OrderResponse;
import com.spark.campusday.meal.order.service.FinalYearOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/** Unsecured development API. Replace the explicit student ID at the Security boundary later. */
@RestController
@RequestMapping("/api/meal/orders")
@RequiredArgsConstructor
public class FinalYearOrderController {
    private final FinalYearOrderService service;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestParam String studentId,
                                                @Valid @RequestBody OrderRequest request) {
        OrderResponse created = service.create(studentId, request);
        return ResponseEntity.created(URI.create("/api/meal/orders/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public OrderResponse update(@RequestParam String studentId, @PathVariable String id,
                                @Valid @RequestBody OrderRequest request) {
        return service.update(studentId, id, request);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@RequestParam String studentId, @PathVariable String id) {
        return service.cancel(studentId, id);
    }

    @GetMapping("/by-date")
    public ResponseEntity<OrderResponse> byDate(@RequestParam String studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mealDate) {
        return service.getByDate(studentId, mealDate)
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping
    public List<OrderResponse> history(@RequestParam String studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.history(studentId, from, to);
    }

    @GetMapping("/{id}")
    public OrderResponse byId(@RequestParam String studentId, @PathVariable String id) {
        return service.getById(studentId, id);
    }
}
