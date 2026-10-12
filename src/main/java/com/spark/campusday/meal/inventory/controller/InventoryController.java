package com.spark.campusday.meal.inventory.controller;

import com.spark.campusday.meal.inventory.dto.IngredientRequest;
import com.spark.campusday.meal.inventory.dto.InventoryResponses.IngredientResponse;
import com.spark.campusday.meal.inventory.dto.InventoryResponses.IssueHistoryResponse;
import com.spark.campusday.meal.inventory.dto.InventoryResponses.MovementHistoryResponse;
import com.spark.campusday.meal.inventory.dto.InventoryResponses.MovementResponse;
import com.spark.campusday.meal.inventory.dto.KitchenIssueRequest;
import com.spark.campusday.meal.inventory.dto.OpeningStockRequest;
import com.spark.campusday.meal.inventory.dto.StockAdjustmentRequest;
import com.spark.campusday.meal.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Development mutation APIs use an explicit Student registration ID until Security is integrated. */
@RestController
@RequestMapping("/api/meal")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService service;

    @PostMapping("/ingredients")
    public IngredientResponse create(@RequestParam String managerStudentId,
                                     @Valid @RequestBody IngredientRequest request) {
        return service.createIngredient(managerStudentId, request);
    }

    @GetMapping("/ingredients")
    public List<IngredientResponse> list() {
        return service.listIngredients();
    }

    @GetMapping("/ingredients/{id}")
    public IngredientResponse get(@PathVariable String id) {
        return service.getIngredient(id);
    }

    @PutMapping("/ingredients/{id}")
    public IngredientResponse update(@PathVariable String id, @RequestParam String managerStudentId,
                                     @Valid @RequestBody IngredientRequest request) {
        return service.updateIngredient(managerStudentId, id, request);
    }

    @PostMapping("/stock/opening")
    public MovementResponse opening(@RequestParam String managerStudentId,
                                    @Valid @RequestBody OpeningStockRequest request) {
        return service.open(managerStudentId, request);
    }

    @PostMapping("/stock/adjustments")
    public MovementResponse adjustment(@RequestParam String managerStudentId,
                                       @Valid @RequestBody StockAdjustmentRequest request) {
        return service.adjust(managerStudentId, request);
    }

    @PostMapping("/stock/issues")
    public MovementResponse issue(@RequestParam String managerStudentId,
                                  @Valid @RequestBody KitchenIssueRequest request) {
        return service.issue(managerStudentId, request);
    }

    @GetMapping("/stock/{ingredientId}/movements")
    public MovementHistoryResponse history(@PathVariable String ingredientId,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return service.history(ingredientId, page, size);
    }

    @GetMapping("/stock/issues")
    public IssueHistoryResponse issues(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return service.issuesForDate(date, page, size);
    }
}
