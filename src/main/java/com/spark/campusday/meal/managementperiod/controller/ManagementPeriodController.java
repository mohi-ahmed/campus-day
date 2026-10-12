package com.spark.campusday.meal.managementperiod.controller;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.managementperiod.repository.MealManagementPeriodRepository;
import com.spark.campusday.student.service.StudentService;
import com.spark.campusday.meal.managementperiod.dto.ManagementPeriodContextResponse;
import com.spark.campusday.meal.managementperiod.dto.ManagementPeriodHistoryResponse;
import com.spark.campusday.meal.managementperiod.dto.ManagementPeriodRequest;
import com.spark.campusday.meal.managementperiod.dto.ManagementPeriodResponse;
import com.spark.campusday.meal.managementperiod.service.ManagementPeriodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

/** Unsecured development API until the dedicated Security phase. */
@RestController
@RequestMapping("/api/meal/management-periods")
@RequiredArgsConstructor
public class ManagementPeriodController {
    private final ManagementPeriodService service;
    private final MealManagementPeriodRepository periods;
    private final StudentService students;
    private final MealManagerAccess managerAccess;

    @PostMapping
    public ResponseEntity<ManagementPeriodResponse> create(@Valid @RequestBody ManagementPeriodRequest request) {
        ManagementPeriodResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/meal/management-periods/" + created.id())).body(created);
    }

    @GetMapping
    public ManagementPeriodHistoryResponse history(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidRequestException("Page must be nonnegative and size must be between 1 and 100");
        }
        return service.history(PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("year"), Sort.Order.desc("month"))));
    }

    @GetMapping("/context")
    public ManagementPeriodContextResponse context() {
        return service.context();
    }

    @GetMapping("/current")
    public ManagementPeriodResponse current() {
        return service.getCurrent();
    }

    @GetMapping("/{id}")
    public ManagementPeriodResponse byId(@PathVariable String id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public ManagementPeriodResponse update(@PathVariable String id, @Valid @RequestBody ManagementPeriodRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/close")
    public ManagementPeriodResponse close(@PathVariable String id, @RequestParam String managerStudentId) {
        var actor = students.getByStudentId(managerStudentId);
        if (!managerAccess.hasCurrentAccess(actor.getId()))
            throw new InvalidBusinessStateException("Only a currently assigned Meal Manager may close a period");
        var period = periods.findById(id).orElseThrow(() -> new ResourceNotFoundException("Management period not found: " + id));
        if (!period.getManagerStudentIds().contains(actor.getId()) || !service.getCurrent().id().equals(id))
            throw new InvalidBusinessStateException("Only a manager of the current period may close it");
        return service.close(id);
    }
}
