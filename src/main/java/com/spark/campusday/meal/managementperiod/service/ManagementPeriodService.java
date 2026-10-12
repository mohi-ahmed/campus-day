package com.spark.campusday.meal.managementperiod.service;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.meal.managementperiod.dto.ManagementPeriodContextResponse;
import com.spark.campusday.meal.managementperiod.dto.ManagementPeriodHistoryResponse;
import com.spark.campusday.meal.managementperiod.dto.ManagementPeriodRequest;
import com.spark.campusday.meal.managementperiod.dto.ManagementPeriodResponse;
import com.spark.campusday.meal.managementperiod.dto.ManagerSummaryResponse;
import com.spark.campusday.meal.managementperiod.model.ManagementPeriodStatus;
import com.spark.campusday.meal.managementperiod.model.MealManagementPeriod;
import com.spark.campusday.meal.managementperiod.repository.MealManagementPeriodRepository;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ManagementPeriodService {
    private final MealManagementPeriodRepository periods;
    private final StudentRepository students;
    @Qualifier("mealClock")
    private final Clock mealClock;

    public ManagementPeriodResponse create(ManagementPeriodRequest request) {
        validateRequiredMonth(request);
        MealManagementPeriod period = new MealManagementPeriod(request.year(), request.month(),
                request.startDate(), request.endDate(), request.managerStudentIds());
        Map<String, Student> managers = loadEligibleManagers(period.getManagerStudentIds());
        rejectActiveOverlap(period, "");
        return response(save(period), managers);
    }

    public ManagementPeriodResponse update(String id, ManagementPeriodRequest request) {
        MealManagementPeriod period = requirePeriod(id);
        validateRequiredMonth(request);
        period.reviseDetails(request.year(), request.month(), request.startDate(), request.endDate(),
                request.managerStudentIds());
        Map<String, Student> managers = loadEligibleManagers(period.getManagerStudentIds());
        rejectActiveOverlap(period, id);
        return response(save(period), managers);
    }

    public ManagementPeriodResponse close(String id) {
        MealManagementPeriod period = requirePeriod(id);
        Map<String, Student> managers = loadStudents(period.getManagerStudentIds());
        period.close();
        return response(periods.save(period), managers);
    }

    public ManagementPeriodResponse getById(String id) {
        MealManagementPeriod period = requirePeriod(id);
        return response(period, loadStudents(period.getManagerStudentIds()));
    }

    public ManagementPeriodResponse getCurrent() {
        MealManagementPeriod period = currentPeriod()
                .orElseThrow(() -> new ResourceNotFoundException("No current management period"));
        return response(period, loadStudents(period.getManagerStudentIds()));
    }

    public ManagementPeriodHistoryResponse history(Pageable pageable) {
        Page<MealManagementPeriod> page = periods.findAll(pageable);
        Map<String, Student> managers = loadStudents(managerIds(page.getContent()));
        List<ManagementPeriodResponse> content = page.getContent().stream()
                .map(period -> response(period, managers)).toList();
        return new ManagementPeriodHistoryResponse(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    public ManagementPeriodContextResponse context() {
        Optional<MealManagementPeriod> current = currentPeriod();
        LocalDate anchor = current.map(MealManagementPeriod::getStartDate).orElse(LocalDate.now(mealClock));
        Optional<MealManagementPeriod> previous = periods.findFirstByStartDateLessThanOrderByStartDateDesc(anchor);
        Optional<MealManagementPeriod> next = periods.findFirstByStartDateGreaterThanOrderByStartDateAsc(anchor);
        List<MealManagementPeriod> selected = new ArrayList<>(3);
        current.ifPresent(selected::add);
        previous.ifPresent(selected::add);
        next.ifPresent(selected::add);
        Map<String, Student> managers = loadStudents(managerIds(selected));
        return new ManagementPeriodContextResponse(
                current.map(period -> response(period, managers)).orElse(null),
                previous.map(period -> response(period, managers)).orElse(null),
                next.map(period -> response(period, managers)).orElse(null));
    }

    private Optional<MealManagementPeriod> currentPeriod() {
        LocalDate today = LocalDate.now(mealClock);
        return periods.findFirstByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                ManagementPeriodStatus.ACTIVE, today, today);
    }

    private void validateRequiredMonth(ManagementPeriodRequest request) {
        if (request == null || request.year() == null || request.month() == null) {
            throw new InvalidBusinessStateException("Management year and month are required");
        }
    }

    private MealManagementPeriod requirePeriod(String id) {
        return periods.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Management period not found: " + id));
    }

    private void rejectActiveOverlap(MealManagementPeriod period, String excludedId) {
        if (periods.existsByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
                ManagementPeriodStatus.ACTIVE, period.getEndDate(), period.getStartDate(), excludedId)) {
            throw new ConflictException("An active management period overlaps these dates");
        }
    }

    private MealManagementPeriod save(MealManagementPeriod period) {
        try {
            return periods.save(period);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("A management period already exists for this month", exception);
        }
    }

    private Map<String, Student> loadEligibleManagers(List<String> ids) {
        Map<String, Student> managers = loadStudents(ids);
        for (String id : ids) {
            if (!managers.get(id).getAcademicYear().isFourthYear()) {
                throw new InvalidBusinessStateException("Assigned Meal Managers must be Fourth Year students");
            }
        }
        return managers;
    }

    private Map<String, Student> loadStudents(Iterable<String> ids) {
        Set<String> distinctIds = new LinkedHashSet<>();
        ids.forEach(distinctIds::add);
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        Map<String, Student> found = new HashMap<>();
        students.findAllById(distinctIds).forEach(student -> found.put(student.getId(), student));
        for (String id : distinctIds) {
            if (!found.containsKey(id)) {
                throw new ResourceNotFoundException("Student not found: " + id);
            }
        }
        return found;
    }

    private Set<String> managerIds(Iterable<MealManagementPeriod> selected) {
        Set<String> ids = new LinkedHashSet<>();
        selected.forEach(period -> ids.addAll(period.getManagerStudentIds()));
        return ids;
    }

    private ManagementPeriodResponse response(MealManagementPeriod period, Map<String, Student> managers) {
        List<ManagerSummaryResponse> summaries = period.getManagerStudentIds().stream()
                .map(id -> managers.get(id))
                .map(student -> new ManagerSummaryResponse(student.getId(), student.getStudentId(),
                        student.getName(), student.getAcademicYear()))
                .toList();
        return new ManagementPeriodResponse(period.getId(), period.getYear(), period.getMonth(),
                period.getStartDate(), period.getEndDate(), period.getStatus(), summaries,
                period.getCreatedAt(), period.getUpdatedAt());
    }
}
