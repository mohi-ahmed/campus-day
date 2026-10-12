package com.spark.campusday.meal.report.service;

import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.meal.billing.model.FinalYearBill;
import com.spark.campusday.meal.billing.model.MonthlyPayment;
import com.spark.campusday.meal.billing.repository.FinalYearBillRepository;
import com.spark.campusday.meal.billing.repository.FixedChargeVersionRepository;
import com.spark.campusday.meal.billing.repository.MonthlyPaymentRepository;
import com.spark.campusday.meal.handover.model.StockHandover;
import com.spark.campusday.meal.handover.repository.StockHandoverRepository;
import com.spark.campusday.meal.inventory.dto.InventoryResponses.IngredientResponse;
import com.spark.campusday.meal.inventory.model.IngredientUnit;
import com.spark.campusday.meal.inventory.model.StockMovement;
import com.spark.campusday.meal.inventory.model.StockMovementType;
import com.spark.campusday.meal.inventory.repository.StockMovementRepository;
import com.spark.campusday.meal.inventory.service.InventoryService;
import com.spark.campusday.meal.managementperiod.model.MealManagementPeriod;
import com.spark.campusday.meal.managementperiod.repository.MealManagementPeriodRepository;
import com.spark.campusday.meal.order.model.FinalYearMealOrder;
import com.spark.campusday.meal.order.model.MealChoiceKind;
import com.spark.campusday.meal.order.model.OrderStatus;
import com.spark.campusday.meal.order.repository.FinalYearMealOrderRepository;
import com.spark.campusday.meal.purchase.model.Purchase;
import com.spark.campusday.meal.purchase.model.PurchasePaymentMode;
import com.spark.campusday.meal.purchase.model.PurchaseStatus;
import com.spark.campusday.meal.purchase.repository.PurchaseRepository;
import com.spark.campusday.meal.supplier.model.SupplierAccountEntry;
import com.spark.campusday.meal.supplier.model.SupplierAccountEntryType;
import com.spark.campusday.meal.supplier.repository.SupplierAccountEntryRepository;
import com.spark.campusday.meal.supplier.service.SupplierAccountService;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonthlyReportService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final MealManagementPeriodRepository periods;
    private final StudentRepository students;
    private final InventoryService inventory;
    private final StockMovementRepository movements;
    private final PurchaseRepository purchases;
    private final SupplierAccountEntryRepository supplierEntries;
    private final SupplierAccountService supplierAccounts;
    private final FinalYearMealOrderRepository orders;
    private final FixedChargeVersionRepository charges;
    private final MonthlyPaymentRepository payments;
    private final FinalYearBillRepository bills;
    private final StockHandoverRepository handovers;

    public record Manager(String id, String studentId, String name) {}
    public record OrderItem(String meal, String itemId, String name, int quantity) {}
    public record OrderSummary(int validOrders, int cancelledOrders, int breakfastPackages,
                               int lunchFullMeals, int dinnerFullMeals, List<OrderItem> items) {}
    public record PurchaseSummary(int count, BigDecimal total, BigDecimal cashTotal, BigDecimal creditTotal) {}
    public record SupplierSummary(BigDecimal paymentTotal, BigDecimal currentOutstanding) {}
    public record MovementSummary(String ingredientId, StockMovementType type, IngredientUnit unit, BigDecimal quantity) {}
    public record InventorySummary(List<MovementSummary> movements, List<IngredientResponse> liveStock,
                                   List<StockHandover.Line> transferredStock) {}
    public record BillingSummary(BigDecimal fixedMonthlyCharge, BigDecimal officeCollected,
                                 int finalizedFinalYearBills, BigDecimal finalYearBilled,
                                 BigDecimal finalYearCollected, BigDecimal finalYearOutstanding) {}
    public record Report(int year, int month, String periodId, String periodStatus, List<Manager> managers,
                         OrderSummary orders, PurchaseSummary purchases, SupplierSummary suppliers,
                         InventorySummary inventory, BillingSummary billing, StockHandover handover) {}

    public Report monthly(int year, int month) {
        if (year < 2000 || year > 9999 || month < 1 || month > 12) throw new InvalidRequestException("Invalid report month");
        YearMonth selected = YearMonth.of(year, month);
        MealManagementPeriod period = periods.findByYearAndMonth(year, month)
                .orElseThrow(() -> new ResourceNotFoundException("Management period not found for month"));
        Map<String, Student> managers = students.findAllById(period.getManagerStudentIds()).stream()
                .collect(Collectors.toMap(Student::getId, student -> student));
        List<Manager> team = period.getManagerStudentIds().stream().map(id -> {
            Student student = managers.get(id);
            if (student == null) throw new ResourceNotFoundException("Manager Student not found: " + id);
            return new Manager(id, student.getStudentId(), student.getName());
        }).toList();

        List<FinalYearMealOrder> monthOrders = orders.findInMonth(selected.atDay(1), selected.atEndOfMonth());
        int valid = 0, cancelled = 0, breakfast = 0, lunchFull = 0, dinnerFull = 0;
        Map<String, OrderItem> itemTotals = new HashMap<>();
        for (FinalYearMealOrder order : monthOrders) {
            if (order.getStatus() != OrderStatus.SUBMITTED) { cancelled++; continue; }
            valid++;
            if (order.isBreakfast()) breakfast++;
            if (order.getLunch().kind() == MealChoiceKind.FULL_MEAL) lunchFull++;
            if (order.getDinner().kind() == MealChoiceKind.FULL_MEAL) dinnerFull++;
            accumulateItems(itemTotals, "LUNCH", order.getLunch());
            accumulateItems(itemTotals, "DINNER", order.getDinner());
        }
        OrderSummary orderSummary = new OrderSummary(valid, cancelled, breakfast, lunchFull, dinnerFull,
                itemTotals.values().stream().sorted((a, b) -> (a.meal() + a.name()).compareTo(b.meal() + b.name())).toList());

        List<Purchase> completed = purchases.findByPurchaseDateBetween(selected.atDay(1), selected.atEndOfMonth())
                .stream().filter(purchase -> purchase.getStatus() == PurchaseStatus.COMPLETED).toList();
        PurchaseSummary purchaseSummary = new PurchaseSummary(completed.size(), sum(completed.stream().map(Purchase::getTotal).toList()),
                sum(completed.stream().filter(p -> p.getPaymentMode() == PurchasePaymentMode.CASH).map(Purchase::getTotal).toList()),
                sum(completed.stream().filter(p -> p.getPaymentMode() == PurchasePaymentMode.CREDIT).map(Purchase::getTotal).toList()));
        List<SupplierAccountEntry> monthEntries = supplierEntries.findByBusinessDateBetween(selected.atDay(1), selected.atEndOfMonth());
        SupplierSummary supplierSummary = new SupplierSummary(sum(monthEntries.stream()
                .filter(entry -> entry.getType() == SupplierAccountEntryType.PAYMENT).map(SupplierAccountEntry::getAmount).toList()),
                sum(supplierAccounts.allDues().values().stream().toList()));

        Map<String, MovementSummary> movementTotals = new HashMap<>();
        for (StockMovement movement : movements.findInMonth(selected.atDay(1), selected.atEndOfMonth())) {
            String key = movement.getIngredientId() + "|" + movement.getType();
            MovementSummary prior = movementTotals.get(key);
            movementTotals.put(key, new MovementSummary(movement.getIngredientId(), movement.getType(), movement.getUnit(),
                    prior == null ? movement.getQuantity() : prior.quantity().add(movement.getQuantity())));
        }
        StockHandover handover = handovers.findByOutgoingPeriodId(period.getId()).orElse(null);
        InventorySummary inventorySummary = new InventorySummary(new ArrayList<>(movementTotals.values()),
                inventory.listIngredients(), handover == null ? List.of() : handover.getLines());

        List<MonthlyPayment> office = payments.findByYearAndMonthAndChannelOrderByPaymentDateAsc(year, month, "COLLEGE_OFFICE");
        List<FinalYearBill> finalized = bills.findByYearAndMonth(year, month);
        BigDecimal billed = sum(finalized.stream().map(FinalYearBill::getTotal).toList());
        BigDecimal collected = sum(finalized.stream().map(FinalYearBill::getPaid).toList());
        BillingSummary billingSummary = new BillingSummary(charges.findFirstByYearAndMonthOrderByEffectiveAtDesc(year, month)
                .map(charge -> charge.getAmount()).orElse(null), sum(office.stream().map(MonthlyPayment::getAmount).toList()),
                finalized.size(), billed, collected, billed.subtract(collected));
        return new Report(year, month, period.getId(), period.getStatus().name(), team, orderSummary,
                purchaseSummary, supplierSummary, inventorySummary, billingSummary, handover);
    }

    private void accumulateItems(Map<String, OrderItem> totals, String meal, FinalYearMealOrder.MealChoice choice) {
        if (choice.kind() != MealChoiceKind.ITEMS) return;
        choice.items().forEach(item -> {
            String key = meal + "|" + item.itemId();
            OrderItem prior = totals.get(key);
            totals.put(key, new OrderItem(meal, item.itemId(), item.name(),
                    (prior == null ? 0 : prior.quantity()) + item.quantity()));
        });
    }
    private BigDecimal sum(List<BigDecimal> amounts) { return amounts.stream().reduce(ZERO, BigDecimal::add); }
}
