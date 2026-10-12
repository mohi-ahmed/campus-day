package com.spark.campusday.meal.menu.controller;

import com.spark.campusday.meal.menu.dto.MenuRequest;
import com.spark.campusday.meal.menu.dto.MenuResponse;
import com.spark.campusday.meal.menu.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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

/** Mutation and management routes are unsecured development APIs until the Security phase. */
@RestController
@RequestMapping("/api/meal/menus")
@RequiredArgsConstructor
public class MenuController {
    private final MenuService service;

    @PostMapping
    public ResponseEntity<MenuResponse> create(@Valid @RequestBody MenuRequest request) {
        MenuResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/meal/menus/manage/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public MenuResponse update(@PathVariable String id, @Valid @RequestBody MenuRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public MenuResponse publish(@PathVariable String id) {
        return service.publish(id);
    }

    @GetMapping
    public List<MenuResponse> published(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.publishedBetween(from, to);
    }

    @GetMapping("/manage")
    public List<MenuResponse> managed(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.managedBetween(from, to);
    }

    @GetMapping("/manage/{id}")
    public MenuResponse managedById(@PathVariable String id) {
        return service.getManaged(id);
    }

    @GetMapping("/{date}")
    public MenuResponse publishedByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getPublished(date);
    }
}
