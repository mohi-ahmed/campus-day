package com.spark.campusday.meal.billing;

import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.billing.model.FinalYearBill;
import com.spark.campusday.meal.billing.model.FixedChargeVersion;
import com.spark.campusday.meal.billing.model.MonthlyPayment;
import com.spark.campusday.meal.billing.repository.FinalYearBillRepository;
import com.spark.campusday.meal.billing.repository.FixedChargeVersionRepository;
import com.spark.campusday.meal.billing.repository.MonthlyPaymentRepository;
import com.spark.campusday.meal.billing.service.BillingService;
import com.spark.campusday.meal.menu.model.DailyMealMenu;
import com.spark.campusday.meal.menu.repository.DailyMealMenuRepository;
import com.spark.campusday.meal.order.model.FinalYearMealOrder;
import com.spark.campusday.meal.order.model.MealChoiceKind;
import com.spark.campusday.meal.order.model.OrderStatus;
import com.spark.campusday.meal.order.repository.FinalYearMealOrderRepository;
import com.spark.campusday.meal.price.model.MealPriceVersion;
import com.spark.campusday.meal.price.model.PriceTarget;
import com.spark.campusday.meal.price.repository.MealPriceVersionRepository;
import com.spark.campusday.student.model.AcademicYear;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import com.mongodb.client.result.UpdateResult;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BillingServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 12);
    private final StudentService students = mock(StudentService.class);
    private final MealManagerAccess access = mock(MealManagerAccess.class);
    private final FixedChargeVersionRepository charges = mock(FixedChargeVersionRepository.class);
    private final MonthlyPaymentRepository payments = mock(MonthlyPaymentRepository.class);
    private final FinalYearBillRepository finalBills = mock(FinalYearBillRepository.class);
    private final FinalYearMealOrderRepository orders = mock(FinalYearMealOrderRepository.class);
    private final DailyMealMenuRepository menus = mock(DailyMealMenuRepository.class);
    private final MealPriceVersionRepository prices = mock(MealPriceVersionRepository.class);
    private final MongoOperations mongo = mock(MongoOperations.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-25T10:00:00Z"), ZoneId.of("Asia/Dhaka"));
    private final BillingService service = new BillingService(students, access, charges, payments, finalBills, orders, menus, prices, mongo, clock);

    private void student(String registrationId, AcademicYear year) {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn("doc-" + registrationId);
        when(student.getAcademicYear()).thenReturn(year);
        when(students.getByStudentId(registrationId)).thenReturn(student);
    }

    private void manager() { student("M", AcademicYear.FOURTH_YEAR); when(access.hasCurrentAccess("doc-M")).thenReturn(true); }

    @Test void currentManagerSetsChargeAndEarlierRevisionRemains() {
        manager();
        when(charges.save(any())).thenAnswer(call -> call.getArgument(0));
        var first = service.setCharge("M", 2026, 8, new BigDecimal("3200.00"));
        when(charges.findFirstByYearAndMonthOrderByEffectiveAtDesc(2026, 8)).thenReturn(Optional.of(
                new FixedChargeVersion(2026, 8, first.getAmount(), "doc-M", Instant.now(clock).minusSeconds(1))));
        var second = service.setCharge("M", 2026, 8, new BigDecimal("3300.00"));
        assertEquals(new BigDecimal("3300.00"), second.getAmount());
        verify(charges, times(2)).save(any());
    }

    @Test void fourthYearWithoutAssignmentCannotSetCharge() {
        student("M", AcademicYear.FOURTH_YEAR);
        assertThrows(InvalidBusinessStateException.class, () -> service.setCharge("M", 2026, 8, new BigDecimal("3200")));
        verifyNoInteractions(charges);
    }

    @Test void fixedBillIgnoresAbsenceAndPreservesPaymentsAfterChargeReduction() {
        student("S", AcademicYear.THIRD_YEAR);
        when(charges.findFirstByYearAndMonthOrderByEffectiveAtDesc(2026, 8)).thenReturn(Optional.of(
                new FixedChargeVersion(2026, 8, new BigDecimal("3000.00"), "doc-M", Instant.now(clock))));
        when(payments.findByStudentIdAndYearAndMonthOrderByPaymentDateAscRecordedAtAsc("doc-S", 2026, 8))
                .thenReturn(List.of(new MonthlyPayment("doc-S", 2026, 8, new BigDecimal("3200.00"), DATE,
                        "COLLEGE_OFFICE", null, "R1", Instant.now(clock))));
        var bill = service.bill("S", 2026, 8);
        assertEquals(new BigDecimal("0.00"), bill.outstanding());
        assertEquals(new BigDecimal("200.00"), bill.credit());
        assertEquals(1, bill.paymentHistory().size());
        verifyNoInteractions(orders);
    }

    @Test void officePaymentReducesFixedRemainingBalance() {
        student("S", AcademicYear.FIRST_YEAR);
        when(charges.findFirstByYearAndMonthOrderByEffectiveAtDesc(2026, 8)).thenReturn(Optional.of(
                new FixedChargeVersion(2026, 8, new BigDecimal("3000.00"), "doc-M", Instant.now(clock))));
        when(payments.save(any())).thenAnswer(call -> call.getArgument(0));
        MonthlyPayment recorded = service.recordOfficePayment("S", 2026, 8, new BigDecimal("1000.00"), DATE, "R1");
        when(payments.findByStudentIdAndYearAndMonthOrderByPaymentDateAscRecordedAtAsc("doc-S", 2026, 8))
                .thenReturn(List.of(recorded));
        assertEquals(new BigDecimal("2000.00"), service.bill("S", 2026, 8).outstanding());
        verifyNoInteractions(access);
    }

    private FinalYearMealOrder order(OrderStatus status) {
        FinalYearMealOrder order = mock(FinalYearMealOrder.class);
        when(order.getStatus()).thenReturn(status);
        when(order.getMealDate()).thenReturn(DATE);
        when(order.getId()).thenReturn("order-1");
        when(order.getMenuId()).thenReturn("menu-1");
        when(order.isBreakfast()).thenReturn(true);
        when(order.getLunch()).thenReturn(new FinalYearMealOrder.MealChoice(MealChoiceKind.NOT_EATING, List.of()));
        when(order.getDinner()).thenReturn(new FinalYearMealOrder.MealChoice(MealChoiceKind.NOT_EATING, List.of()));
        return order;
    }

    private void finalYear(OrderStatus status, boolean priced) {
        student("F", AcademicYear.FINAL_YEAR);
        FinalYearMealOrder selectedOrder = order(status);
        when(orders.findHistory("doc-F", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(selectedOrder));
        DailyMealMenu menu = mock(DailyMealMenu.class);
        when(menu.getId()).thenReturn("menu-1");
        when(menu.getBreakfast()).thenReturn(new DailyMealMenu.Breakfast("Breakfast package", true, true));
        when(menus.findAllById(any())).thenReturn(List.of(menu));
        when(prices.findForMenusNewestFirst(any())).thenReturn(priced ? List.of(new MealPriceVersion("menu-1", DATE,
                PriceTarget.BREAKFAST_PACKAGE, null, new BigDecimal("55.00"), "doc-M", Instant.now(clock))) : List.of());
    }

    @Test void cancelledOrMissingOrderHasNoLines() {
        finalYear(OrderStatus.CANCELLED, true);
        assertTrue(service.bill("F", 2026, 8).lines().isEmpty());
        when(orders.findHistory(anyString(), any(), any())).thenReturn(List.of());
        assertTrue(service.bill("F", 2026, 8).lines().isEmpty());
    }

    @Test void submittedOrderWithoutPriceRemainsPending() {
        finalYear(OrderStatus.SUBMITTED, false);
        var bill = service.bill("F", 2026, 8);
        assertEquals("PENDING_FINALIZATION", bill.status());
        assertNull(bill.amount());
        assertTrue(bill.lines().getFirst().pricePending());
        manager();
        assertThrows(InvalidBusinessStateException.class, () -> service.finalizeBill("M", "F", 2026, 8));
        assertThrows(InvalidBusinessStateException.class, () -> service.recordFinalYearCollection("M", "F", 2026, 8,
                new BigDecimal("10.00"), LocalDate.of(2026, 9, 25), null));
    }

    @Test void finalizedSubmittedOrderKeepsOriginalPriceAndCollectionRejectsOverpayment() {
        finalYear(OrderStatus.SUBMITTED, true);
        manager();
        AtomicReference<FinalYearBill> saved = new AtomicReference<>();
        when(finalBills.save(any())).thenAnswer(call -> { saved.set(call.getArgument(0)); return saved.get(); });
        when(finalBills.findByStudentIdAndYearAndMonth("doc-F", 2026, 8))
                .thenAnswer(call -> Optional.ofNullable(saved.get()));
        var bill = service.finalizeBill("M", "F", 2026, 8);
        assertEquals("FINALIZED", bill.status());
        assertEquals(new BigDecimal("55.00"), bill.amount());
        when(prices.findForMenusNewestFirst(any())).thenReturn(List.of(new MealPriceVersion("menu-1", DATE,
                PriceTarget.BREAKFAST_PACKAGE, null, new BigDecimal("75.00"), "doc-M", Instant.now(clock))));
        assertEquals(new BigDecimal("55.00"), service.bill("F", 2026, 8).amount());
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(FinalYearBill.class)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, null), UpdateResult.acknowledged(1, 1L, null));
        assertThrows(InvalidBusinessStateException.class, () -> service.recordFinalYearCollection("M", "F", 2026, 8,
                new BigDecimal("56.00"), LocalDate.of(2026, 9, 25), null));
        var payment = service.recordFinalYearCollection("M", "F", 2026, 8,
                new BigDecimal("20.00"), LocalDate.of(2026, 9, 25), "R2");
        assertEquals("MEAL_MANAGER", payment.getChannel());
        ReflectionTestUtils.setField(saved.get(), "paid", new BigDecimal("20.00"));
        ReflectionTestUtils.setField(saved.get(), "paymentHistory", List.of(payment));
        assertEquals(new BigDecimal("35.00"), service.bill("F", 2026, 8).outstanding());
        assertEquals(1, service.bill("F", 2026, 8).paymentHistory().size());
    }
}
