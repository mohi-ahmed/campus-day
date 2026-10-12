package com.spark.campusday.meal.handover.controller;

import com.spark.campusday.meal.handover.model.StockHandover;
import com.spark.campusday.meal.handover.service.StockHandoverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/meal/stock-handovers")
@RequiredArgsConstructor
public class StockHandoverController {
    private final StockHandoverService service;
    @PostMapping
    public ResponseEntity<StockHandover> create(@RequestParam String managerStudentId,
                                                @RequestBody StockHandoverService.CreateRequest request) {
        StockHandover saved = service.create(managerStudentId, request);
        return ResponseEntity.created(URI.create("/api/meal/stock-handovers/" + saved.getId())).body(saved);
    }
    @GetMapping
    public List<StockHandover> history() { return service.history(); }
    @GetMapping("/{id}")
    public StockHandover get(@PathVariable String id) { return service.get(id); }
    @PostMapping("/{id}/payment")
    public StockHandover pay(@PathVariable String id, @RequestParam String managerStudentId,
                             @RequestBody StockHandoverService.PaymentRequest request) {
        return service.pay(managerStudentId, id, request);
    }
}
