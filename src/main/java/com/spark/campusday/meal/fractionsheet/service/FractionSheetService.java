package com.spark.campusday.meal.fractionsheet.service;

import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.fractionsheet.dto.FractionSheetResponse;
import com.spark.campusday.meal.fractionsheet.dto.FractionSheetResponse.ItemQuantity;
import com.spark.campusday.meal.fractionsheet.dto.FractionSheetResponse.MealSelection;
import com.spark.campusday.meal.fractionsheet.dto.FractionSheetResponse.RowStatus;
import com.spark.campusday.meal.fractionsheet.dto.FractionSheetResponse.StudentRow;
import com.spark.campusday.meal.fractionsheet.dto.FractionSheetResponse.Totals;
import com.spark.campusday.meal.order.model.FinalYearMealOrder;
import com.spark.campusday.meal.order.model.MealChoiceKind;
import com.spark.campusday.meal.order.model.OrderStatus;
import com.spark.campusday.meal.order.repository.FinalYearMealOrderRepository;
import com.spark.campusday.meal.order.service.OrderCutoffService;
import com.spark.campusday.student.model.AcademicYear;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.repository.StudentRepository;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FractionSheetService {
    private static final MealSelection NOT_EATING = new MealSelection(MealChoiceKind.NOT_EATING, List.of());

    private final StudentService studentService;
    private final StudentRepository students;
    private final FinalYearMealOrderRepository orders;
    private final OrderCutoffService cutoffs;
    private final MealManagerAccess managerAccess;
    @Qualifier("mealClock")
    private final Clock mealClock;

    /** managerStudentId is a registration ID until Security provides the acting Student. */
    public FractionSheetResponse get(LocalDate date, String managerStudentId) {
        if (date == null) throw new InvalidRequestException("Meal date is required");
        Student manager = studentService.getByStudentId(managerStudentId);
        if (!managerAccess.hasCurrentAccess(manager.getId())) {
            throw new InvalidBusinessStateException("Only a currently assigned Meal Manager may view the fraction sheet");
        }

        List<Student> roster = students.findByAcademicYearOrderByNameAsc(AcademicYear.FINAL_YEAR);
        if (roster.isEmpty()) return new FractionSheetResponse(date, List.of(),
                new Totals(0, 0, 0, 0, 0, 0, 0, 0, List.of(), List.of()));

        List<String> ids = roster.stream().map(Student::getId).toList();
        Map<String, FinalYearMealOrder> byStudent = orders.findByStudentIdInAndMealDate(ids, date).stream()
                .collect(Collectors.toMap(FinalYearMealOrder::getStudentId, Function.identity()));
        boolean locked = false;
        if (byStudent.values().stream().anyMatch(order -> order.getStatus() == OrderStatus.SUBMITTED)) {
            var deadlines = cutoffs.get(date);
            Instant now = Instant.now(mealClock);
            locked = !now.isBefore(deadlines.editCutoffAt()) && !now.isBefore(deadlines.cancelCutoffAt());
        }

        List<StudentRow> rows = new ArrayList<>(roster.size());
        Map<String, ItemQuantity> lunchItems = new LinkedHashMap<>();
        Map<String, ItemQuantity> dinnerItems = new LinkedHashMap<>();
        int submitted = 0, cancelled = 0, notEating = 0, breakfast = 0, lunchFull = 0, dinnerFull = 0;
        for (Student student : roster) {
            FinalYearMealOrder order = byStudent.get(student.getId());
            if (order == null) {
                notEating++;
                rows.add(new StudentRow(student.getStudentId(), student.getName(), RowStatus.NOT_EATING,
                        false, NOT_EATING, NOT_EATING));
            } else if (order.getStatus() == OrderStatus.CANCELLED) {
                cancelled++;
                rows.add(new StudentRow(student.getStudentId(), student.getName(), RowStatus.CANCELLED,
                        false, NOT_EATING, NOT_EATING));
            } else {
                submitted++;
                if (order.isBreakfast()) breakfast++;
                if (order.getLunch().kind() == MealChoiceKind.FULL_MEAL) lunchFull++;
                if (order.getDinner().kind() == MealChoiceKind.FULL_MEAL) dinnerFull++;
                addItems(lunchItems, order.getLunch());
                addItems(dinnerItems, order.getDinner());
                rows.add(new StudentRow(student.getStudentId(), student.getName(),
                        locked ? RowStatus.LOCKED : RowStatus.SUBMITTED, order.isBreakfast(),
                        selection(order.getLunch()), selection(order.getDinner())));
            }
        }
        Totals totals = new Totals(roster.size(), submitted, cancelled, notEating, locked ? submitted : 0,
                breakfast, lunchFull, dinnerFull, List.copyOf(lunchItems.values()), List.copyOf(dinnerItems.values()));
        return new FractionSheetResponse(date, List.copyOf(rows), totals);
    }

    private MealSelection selection(FinalYearMealOrder.MealChoice choice) {
        return new MealSelection(choice.kind(), choice.items().stream()
                .map(item -> new ItemQuantity(item.itemId(), item.name(), item.quantity())).toList());
    }

    private void addItems(Map<String, ItemQuantity> totals, FinalYearMealOrder.MealChoice choice) {
        if (choice.kind() != MealChoiceKind.ITEMS) return;
        choice.items().forEach(item -> totals.merge(item.itemId(),
                new ItemQuantity(item.itemId(), item.name(), item.quantity()),
                (existing, added) -> new ItemQuantity(existing.itemId(), existing.name(),
                        existing.quantity() + added.quantity())));
    }
}
