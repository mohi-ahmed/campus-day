package com.spark.campusday.meal.report;

import com.spark.campusday.meal.billing.model.FinalYearBill;
import com.spark.campusday.meal.billing.model.FixedChargeVersion;
import com.spark.campusday.meal.billing.model.MonthlyPayment;
import com.spark.campusday.meal.billing.repository.FinalYearBillRepository;
import com.spark.campusday.meal.billing.repository.FixedChargeVersionRepository;
import com.spark.campusday.meal.billing.repository.MonthlyPaymentRepository;
import com.spark.campusday.meal.handover.repository.StockHandoverRepository;
import com.spark.campusday.meal.inventory.model.StockMovement;
import com.spark.campusday.meal.inventory.model.StockMovementType;
import com.spark.campusday.meal.inventory.repository.StockMovementRepository;
import com.spark.campusday.meal.inventory.service.InventoryService;
import com.spark.campusday.meal.managementperiod.model.ManagementPeriodStatus;
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
import com.spark.campusday.meal.report.service.MonthlyReportService;
import com.spark.campusday.meal.supplier.model.SupplierAccountEntry;
import com.spark.campusday.meal.supplier.model.SupplierAccountEntryType;
import com.spark.campusday.meal.supplier.repository.SupplierAccountEntryRepository;
import com.spark.campusday.meal.supplier.service.SupplierAccountService;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MonthlyReportServiceTest {
    private final MealManagementPeriodRepository periods = mock(MealManagementPeriodRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final InventoryService inventory = mock(InventoryService.class);
    private final StockMovementRepository movements = mock(StockMovementRepository.class);
    private final PurchaseRepository purchases = mock(PurchaseRepository.class);
    private final SupplierAccountEntryRepository supplierEntries = mock(SupplierAccountEntryRepository.class);
    private final SupplierAccountService supplierAccounts = mock(SupplierAccountService.class);
    private final FinalYearMealOrderRepository orders = mock(FinalYearMealOrderRepository.class);
    private final FixedChargeVersionRepository charges = mock(FixedChargeVersionRepository.class);
    private final MonthlyPaymentRepository payments = mock(MonthlyPaymentRepository.class);
    private final FinalYearBillRepository bills = mock(FinalYearBillRepository.class);
    private final StockHandoverRepository handovers = mock(StockHandoverRepository.class);
    private final MonthlyReportService service = new MonthlyReportService(periods, students, inventory, movements,
            purchases, supplierEntries, supplierAccounts, orders, charges, payments, bills, handovers);

    @Test void aggregatesAuthoritativeMonthRecordsInBatches() {
        MealManagementPeriod period = mock(MealManagementPeriod.class);
        when(period.getId()).thenReturn("period"); when(period.getStatus()).thenReturn(ManagementPeriodStatus.CLOSED);
        when(period.getManagerStudentIds()).thenReturn(List.of("a", "b"));
        when(periods.findByYearAndMonth(2026, 9)).thenReturn(Optional.of(period));
        Student a = mock(Student.class), b = mock(Student.class);
        when(a.getId()).thenReturn("a"); when(a.getStudentId()).thenReturn("A"); when(a.getName()).thenReturn("A Manager");
        when(b.getId()).thenReturn("b"); when(b.getStudentId()).thenReturn("B"); when(b.getName()).thenReturn("B Manager");
        when(students.findAllById(any())).thenReturn(List.of(a, b));
        FinalYearMealOrder ordered = mock(FinalYearMealOrder.class), cancelled = mock(FinalYearMealOrder.class);
        when(ordered.getStatus()).thenReturn(OrderStatus.SUBMITTED);
        when(ordered.isBreakfast()).thenReturn(true);
        when(ordered.getLunch()).thenReturn(new FinalYearMealOrder.MealChoice(MealChoiceKind.FULL_MEAL, List.of()));
        when(ordered.getDinner()).thenReturn(new FinalYearMealOrder.MealChoice(MealChoiceKind.NOT_EATING, List.of()));
        when(cancelled.getStatus()).thenReturn(OrderStatus.CANCELLED);
        when(orders.findInMonth(any(), any())).thenReturn(List.of(ordered, cancelled));
        Purchase cash = mock(Purchase.class), credit = mock(Purchase.class);
        when(cash.getStatus()).thenReturn(PurchaseStatus.COMPLETED); when(cash.getPaymentMode()).thenReturn(PurchasePaymentMode.CASH);
        when(cash.getTotal()).thenReturn(new BigDecimal("100.00"));
        when(credit.getStatus()).thenReturn(PurchaseStatus.COMPLETED); when(credit.getPaymentMode()).thenReturn(PurchasePaymentMode.CREDIT);
        when(credit.getTotal()).thenReturn(new BigDecimal("40.00"));
        when(purchases.findByPurchaseDateBetween(any(), any())).thenReturn(List.of(cash, credit));
        SupplierAccountEntry supplierPayment = mock(SupplierAccountEntry.class);
        when(supplierPayment.getType()).thenReturn(SupplierAccountEntryType.PAYMENT);
        when(supplierPayment.getAmount()).thenReturn(new BigDecimal("15.00"));
        when(supplierEntries.findByBusinessDateBetween(any(), any())).thenReturn(List.of(supplierPayment));
        when(supplierAccounts.allDues()).thenReturn(Map.of("supplier", new BigDecimal("25.00")));
        StockMovement issue = mock(StockMovement.class);
        when(issue.getIngredientId()).thenReturn("rice"); when(issue.getType()).thenReturn(StockMovementType.KITCHEN_ISSUE);
        when(issue.getQuantity()).thenReturn(new BigDecimal("2.000"));
        when(movements.findInMonth(any(), any())).thenReturn(List.of(issue));
        when(inventory.listIngredients()).thenReturn(List.of());
        when(charges.findFirstByYearAndMonthOrderByEffectiveAtDesc(2026, 9)).thenReturn(Optional.of(
                new FixedChargeVersion(2026, 9, new BigDecimal("3000.00"), "a", Instant.now())));
        when(payments.findByYearAndMonthAndChannelOrderByPaymentDateAsc(2026, 9, "COLLEGE_OFFICE"))
                .thenReturn(List.of(new MonthlyPayment("student", 2026, 9, new BigDecimal("200.00"),
                        LocalDate.of(2026, 9, 10), "COLLEGE_OFFICE", null, null, Instant.now())));
        FinalYearBill bill = mock(FinalYearBill.class);
        when(bill.getTotal()).thenReturn(new BigDecimal("55.00")); when(bill.getPaid()).thenReturn(new BigDecimal("20.00"));
        when(bills.findByYearAndMonth(2026, 9)).thenReturn(List.of(bill));

        var report = service.monthly(2026, 9);
        assertEquals(1, report.orders().validOrders());
        assertEquals(1, report.orders().cancelledOrders());
        assertEquals(1, report.orders().breakfastPackages());
        assertEquals(new BigDecimal("140.00"), report.purchases().total());
        assertEquals(new BigDecimal("15.00"), report.suppliers().paymentTotal());
        assertEquals(new BigDecimal("200.00"), report.billing().officeCollected());
        assertEquals(new BigDecimal("35.00"), report.billing().finalYearOutstanding());
        assertEquals(new BigDecimal("2.000"), report.inventory().movements().getFirst().quantity());
        verify(students, times(1)).findAllById(any());
    }
}
