package com.spark.campusday.meal.order.service;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.meal.menu.dto.MenuResponse;
import com.spark.campusday.meal.menu.model.DailyMealMenu;
import com.spark.campusday.meal.menu.service.MenuService;
import com.spark.campusday.meal.order.dto.OrderRequest;
import com.spark.campusday.meal.order.dto.OrderResponse;
import com.spark.campusday.meal.order.model.FinalYearMealOrder;
import com.spark.campusday.meal.order.model.MealChoiceKind;
import com.spark.campusday.meal.order.model.OrderCutoffConfiguration;
import com.spark.campusday.meal.order.model.OrderStatus;
import com.spark.campusday.meal.order.repository.FinalYearMealOrderRepository;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FinalYearOrderService {
    private final FinalYearMealOrderRepository orders;
    private final StudentService students;
    private final MenuService menus;
    private final OrderCutoffService cutoffs;

    /** studentId is the central Student registration ID until authenticated identity is available. */
    public OrderResponse create(String studentId, OrderRequest request) {
        Student student = requireFinalYear(studentId);
        requireRequest(request);
        OrderCutoffConfiguration configuration = cutoffs.requireConfiguration();
        cutoffs.requireOpen(request.mealDate(), configuration.getOrderCutoff(), "Order");
        if (orders.findByStudentIdAndMealDate(student.getId(), request.mealDate()).isPresent()) {
            throw new ConflictException("An order already exists for this student and date");
        }
        MenuResponse menu = menus.getPublished(request.mealDate());
        FinalYearMealOrder order = new FinalYearMealOrder(student.getId(), request.mealDate(), menu.id(),
                breakfast(request.breakfast(), menu), meal(request.lunch(), menu.lunch(), "Lunch"),
                meal(request.dinner(), menu.dinner(), "Dinner"));
        return response(save(order), configuration);
    }

    public OrderResponse update(String studentId, String orderId, OrderRequest request) {
        Student student = requireFinalYear(studentId);
        FinalYearMealOrder order = requireOwnedOrder(student.getId(), orderId);
        requireRequest(request);
        if (!order.getMealDate().equals(request.mealDate())) {
            throw new InvalidBusinessStateException("Order meal date cannot be changed");
        }
        OrderCutoffConfiguration configuration = cutoffs.requireConfiguration();
        cutoffs.requireOpen(order.getMealDate(), configuration.getEditCutoff(), "Edit");
        MenuResponse menu = menus.getPublished(order.getMealDate());
        order.revise(menu.id(), breakfast(request.breakfast(), menu),
                meal(request.lunch(), menu.lunch(), "Lunch"),
                meal(request.dinner(), menu.dinner(), "Dinner"));
        return response(orders.save(order), configuration);
    }

    public OrderResponse cancel(String studentId, String orderId) {
        Student student = requireFinalYear(studentId);
        FinalYearMealOrder order = requireOwnedOrder(student.getId(), orderId);
        OrderCutoffConfiguration configuration = cutoffs.requireConfiguration();
        cutoffs.requireOpen(order.getMealDate(), configuration.getCancelCutoff(), "Cancel");
        order.cancel();
        return response(orders.save(order), configuration);
    }

    public OrderResponse getById(String studentId, String orderId) {
        Student student = requireFinalYear(studentId);
        return response(requireOwnedOrder(student.getId(), orderId), cutoffs.requireConfiguration());
    }

    public Optional<OrderResponse> getByDate(String studentId, LocalDate date) {
        Student student = requireFinalYear(studentId);
        if (date == null) throw new InvalidRequestException("Meal date is required");
        Optional<FinalYearMealOrder> order = orders.findByStudentIdAndMealDate(student.getId(), date);
        return order.map(found -> response(found, cutoffs.requireConfiguration()));
    }

    public List<OrderResponse> history(String studentId, LocalDate from, LocalDate to) {
        Student student = requireFinalYear(studentId);
        if (from == null || to == null || from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > 366) {
            throw new InvalidRequestException("A date range of at most 367 days is required");
        }
        List<FinalYearMealOrder> found = orders.findHistory(student.getId(), from, to);
        if (found.isEmpty()) return List.of();
        OrderCutoffConfiguration configuration = cutoffs.requireConfiguration();
        return found.stream().map(order -> response(order, configuration)).toList();
    }

    private Student requireFinalYear(String studentId) {
        Student student = students.getByStudentId(studentId);
        if (!student.getAcademicYear().isFinalYear()) {
            throw new InvalidBusinessStateException("Final Year ordering is only for Final Year students");
        }
        return student;
    }

    private void requireRequest(OrderRequest request) {
        if (request == null || request.mealDate() == null || request.lunch() == null || request.dinner() == null) {
            throw new InvalidRequestException("Meal date, lunch and dinner selections are required");
        }
    }

    private FinalYearMealOrder requireOwnedOrder(String studentDbId, String orderId) {
        FinalYearMealOrder order = orders.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (!order.getStudentId().equals(studentDbId)) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        return order;
    }

    private boolean breakfast(boolean selected, MenuResponse menu) {
        if (selected && (!menu.breakfast().available() || !menu.breakfast().orderingEnabled())) {
            throw new InvalidBusinessStateException("Breakfast ordering is unavailable");
        }
        return selected;
    }

    private FinalYearMealOrder.MealChoice meal(OrderRequest.MealChoiceRequest request,
                                               DailyMealMenu.MainMeal menu, String label) {
        if (request.kind() == null) {
            throw new InvalidBusinessStateException(label + " selection type is required");
        }
        List<OrderRequest.ItemQuantity> selected = request.items() == null ? List.of() : request.items();
        if (request.kind() == MealChoiceKind.NOT_EATING) {
            requireNoItems(selected, label);
            return new FinalYearMealOrder.MealChoice(MealChoiceKind.NOT_EATING, List.of());
        }
        if (!menu.available() || !menu.orderingEnabled()) {
            throw new InvalidBusinessStateException(label + " ordering is unavailable");
        }
        if (request.kind() == MealChoiceKind.FULL_MEAL) {
            requireNoItems(selected, label);
            if (!menu.fullMealAvailable()) {
                throw new InvalidBusinessStateException(label + " full meal is unavailable");
            }
            return new FinalYearMealOrder.MealChoice(MealChoiceKind.FULL_MEAL, List.of());
        }
        if (selected.isEmpty()) {
            throw new InvalidBusinessStateException(label + " item selection cannot be empty");
        }
        Map<String, DailyMealMenu.Item> allowed = new HashMap<>();
        menu.items().forEach(item -> allowed.put(item.id(), item));
        Set<String> unique = new HashSet<>();
        List<FinalYearMealOrder.SelectedItem> items = selected.stream().map(choice -> {
            if (choice == null || choice.itemId() == null || choice.itemId().isBlank()
                    || choice.quantity() <= 0 || !unique.add(choice.itemId())) {
                throw new InvalidBusinessStateException(label + " items need distinct IDs and positive quantities");
            }
            DailyMealMenu.Item item = allowed.get(choice.itemId());
            if (item == null || !item.available()) {
                throw new InvalidBusinessStateException(label + " item is unavailable: " + choice.itemId());
            }
            return new FinalYearMealOrder.SelectedItem(item.id(), item.name(), choice.quantity());
        }).toList();
        return new FinalYearMealOrder.MealChoice(MealChoiceKind.ITEMS, items);
    }

    private void requireNoItems(List<OrderRequest.ItemQuantity> items, String label) {
        if (!items.isEmpty()) {
            throw new InvalidBusinessStateException(label + " items are only valid for individual selection");
        }
    }

    private FinalYearMealOrder save(FinalYearMealOrder order) {
        try {
            return orders.save(order);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("An order already exists for this student and date", exception);
        }
    }

    private OrderResponse response(FinalYearMealOrder order, OrderCutoffConfiguration configuration) {
        boolean active = order.getStatus() == OrderStatus.SUBMITTED;
        LocalDate date = order.getMealDate();
        return new OrderResponse(order.getId(), order.getStudentId(), date, order.getMenuId(),
                order.isBreakfast(), order.getLunch(), order.getDinner(), order.getStatus(),
                active && cutoffs.isOpen(date, configuration.getEditCutoff()),
                active && cutoffs.isOpen(date, configuration.getCancelCutoff()),
                cutoffs.deadline(date, configuration.getEditCutoff()),
                cutoffs.deadline(date, configuration.getCancelCutoff()),
                order.getCreatedAt(), order.getUpdatedAt());
    }
}
