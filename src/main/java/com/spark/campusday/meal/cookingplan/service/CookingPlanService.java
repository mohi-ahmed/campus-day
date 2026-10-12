package com.spark.campusday.meal.cookingplan.service;

import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.meal.absence.repository.MealAbsenceNoticeRepository;
import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.cookingplan.dto.CookingPlanResponse;
import com.spark.campusday.meal.cookingplan.dto.CookingPlanResponse.ItemDemand;
import com.spark.campusday.meal.cookingplan.dto.CookingPlanResponse.MealDemand;
import com.spark.campusday.meal.menu.dto.MenuResponse;
import com.spark.campusday.meal.menu.model.DailyMealMenu;
import com.spark.campusday.meal.menu.service.MenuService;
import com.spark.campusday.meal.model.MealType;
import com.spark.campusday.meal.order.model.FinalYearMealOrder;
import com.spark.campusday.meal.order.model.MealChoiceKind;
import com.spark.campusday.meal.order.model.OrderStatus;
import com.spark.campusday.meal.order.repository.FinalYearMealOrderRepository;
import com.spark.campusday.student.model.AcademicYear;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.repository.StudentRepository;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CookingPlanService {
    private final StudentService studentService;
    private final StudentRepository students;
    private final MealAbsenceNoticeRepository absences;
    private final FinalYearMealOrderRepository orders;
    private final MenuService menus;
    private final MealManagerAccess managerAccess;

    /** managerStudentId is a registration ID until Security provides the acting Student. */
    public CookingPlanResponse get(LocalDate date, String managerStudentId) {
        if (date == null) throw new InvalidRequestException("Meal date is required");
        Student manager = studentService.getByStudentId(managerStudentId);
        if (!managerAccess.hasCurrentAccess(manager.getId())) {
            throw new InvalidBusinessStateException("Approved Meal Manager access is required to view the cooking plan");
        }

        MenuResponse menu = menus.getPublished(date);
        List<AcademicYear> fixedYears = Arrays.stream(AcademicYear.values())
                .filter(AcademicYear::isFixedChargeYear).toList();
        List<Student> fixedStudents = students.findByAcademicYearIn(fixedYears);
        List<Student> finalYearStudents = students.findByAcademicYearOrderByNameAsc(AcademicYear.FINAL_YEAR);

        EnumMap<MealType, Set<String>> absentIds = new EnumMap<>(MealType.class);
        for (MealType meal : MealType.values()) absentIds.put(meal, new HashSet<>());
        if (!fixedStudents.isEmpty()) {
            List<String> fixedIds = fixedStudents.stream().map(Student::getId).toList();
            absences.findActiveForStudents(fixedIds, date).forEach(notice ->
                    notice.getMeals().forEach(meal -> absentIds.get(meal).add(notice.getStudentId())));
        }

        EnumMap<MealType, Integer> finalYearFull = new EnumMap<>(MealType.class);
        EnumMap<MealType, Map<String, Integer>> individualItems = new EnumMap<>(MealType.class);
        for (MealType meal : MealType.values()) {
            finalYearFull.put(meal, 0);
            individualItems.put(meal, new HashMap<>());
        }
        if (!finalYearStudents.isEmpty()) {
            List<String> finalIds = finalYearStudents.stream().map(Student::getId).toList();
            orders.findByStudentIdInAndMealDate(finalIds, date).stream()
                    .filter(order -> order.getStatus() == OrderStatus.SUBMITTED)
                    .forEach(order -> addOrder(order, menu, finalYearFull, individualItems));
        }

        int baseline = fixedStudents.size();
        return new CookingPlanResponse(date,
                demand(MealType.BREAKFAST, menu.breakfast().available(), menu.breakfast().packageDescription(),
                        null, List.of(), baseline, absentIds, finalYearFull, individualItems),
                demand(MealType.LUNCH, menu.lunch().available(), menu.lunch().description(),
                        menu.lunch().fullMealDescription(), menu.lunch().items(), baseline,
                        absentIds, finalYearFull, individualItems),
                demand(MealType.DINNER, menu.dinner().available(), menu.dinner().description(),
                        menu.dinner().fullMealDescription(), menu.dinner().items(), baseline,
                        absentIds, finalYearFull, individualItems));
    }

    private void addOrder(FinalYearMealOrder order, MenuResponse menu,
                          Map<MealType, Integer> full, Map<MealType, Map<String, Integer>> items) {
        if (!menu.id().equals(order.getMenuId())) {
            throw new InvalidBusinessStateException("Order references a different menu for this date");
        }
        if (order.isBreakfast()) full.merge(MealType.BREAKFAST, 1, Integer::sum);
        addMealOrder(order.getLunch(), menu.lunch(), MealType.LUNCH, full, items);
        addMealOrder(order.getDinner(), menu.dinner(), MealType.DINNER, full, items);
    }

    private void addMealOrder(FinalYearMealOrder.MealChoice choice, DailyMealMenu.MainMeal menu,
                              MealType meal, Map<MealType, Integer> full,
                              Map<MealType, Map<String, Integer>> items) {
        if (choice.kind() == MealChoiceKind.FULL_MEAL) {
            full.merge(meal, 1, Integer::sum);
        } else if (choice.kind() == MealChoiceKind.ITEMS) {
            Set<String> allowedIds = menu.items().stream().map(DailyMealMenu.Item::id)
                    .collect(java.util.stream.Collectors.toSet());
            choice.items().forEach(item -> {
                if (!allowedIds.contains(item.itemId())) {
                    throw new InvalidBusinessStateException("Ordered item is missing from the published menu");
                }
                items.get(meal).merge(item.itemId(), item.quantity(), Integer::sum);
            });
        }
    }

    private MealDemand demand(MealType meal, boolean available, String description, String fullDescription,
                              List<DailyMealMenu.Item> menuItems, int baseline,
                              Map<MealType, Set<String>> absentIds, Map<MealType, Integer> finalYearFull,
                              Map<MealType, Map<String, Integer>> individualItems) {
        int absent = absentIds.get(meal).size();
        int fixedPlanned = baseline - absent;
        int full = finalYearFull.get(meal);
        List<ItemDemand> itemDemand = menuItems.stream()
                .filter(item -> individualItems.get(meal).getOrDefault(item.id(), 0) > 0)
                .map(item -> new ItemDemand(item.id(), item.name(), item.quantity(),
                        individualItems.get(meal).get(item.id())))
                .toList();
        return new MealDemand(meal, available, description, fullDescription, baseline,
                absent, fixedPlanned, full, fixedPlanned + full, itemDemand);
    }
}
