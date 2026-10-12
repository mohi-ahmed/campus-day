package com.spark.campusday.meal.handover;

import com.mongodb.client.result.UpdateResult;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.handover.model.StockHandover;
import com.spark.campusday.meal.handover.repository.StockHandoverRepository;
import com.spark.campusday.meal.handover.service.StockHandoverService;
import com.spark.campusday.meal.inventory.dto.InventoryResponses.IngredientResponse;
import com.spark.campusday.meal.inventory.model.IngredientUnit;
import com.spark.campusday.meal.inventory.service.InventoryService;
import com.spark.campusday.meal.managementperiod.model.MealManagementPeriod;
import com.spark.campusday.meal.managementperiod.repository.MealManagementPeriodRepository;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StockHandoverServiceTest {
    private final StockHandoverRepository handovers = mock(StockHandoverRepository.class);
    private final MealManagementPeriodRepository periods = mock(MealManagementPeriodRepository.class);
    private final InventoryService inventory = mock(InventoryService.class);
    private final StudentService students = mock(StudentService.class);
    private final MealManagerAccess access = mock(MealManagerAccess.class);
    private final MongoOperations mongo = mock(MongoOperations.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-02T08:00:00Z"), ZoneId.of("Asia/Dhaka"));
    private final StockHandoverService service = new StockHandoverService(handovers, periods, inventory, students, access, mongo, clock);
    private final LocalDate date = LocalDate.of(2026, 10, 1);

    private void setup(boolean authorized) {
        Student actor = mock(Student.class);
        when(actor.getId()).thenReturn("manager-doc");
        when(students.getByStudentId("M")).thenReturn(actor);
        when(access.hasCurrentAccess("manager-doc")).thenReturn(authorized);
        if (!authorized) return;
        MealManagementPeriod outgoing = mock(MealManagementPeriod.class);
        when(outgoing.getId()).thenReturn("old"); when(outgoing.getYear()).thenReturn(2026);
        when(outgoing.getMonth()).thenReturn(9);
        when(outgoing.getStartDate()).thenReturn(LocalDate.of(2026, 9, 1));
        when(outgoing.getEndDate()).thenReturn(LocalDate.of(2026, 9, 30));
        when(outgoing.getManagerStudentIds()).thenReturn(List.of("manager-doc", "other"));
        MealManagementPeriod incoming = mock(MealManagementPeriod.class);
        when(incoming.getId()).thenReturn("new"); when(incoming.getYear()).thenReturn(2026);
        when(incoming.getMonth()).thenReturn(10);
        when(incoming.getStartDate()).thenReturn(LocalDate.of(2026, 10, 1));
        when(incoming.getEndDate()).thenReturn(LocalDate.of(2026, 10, 31));
        when(incoming.getManagerStudentIds()).thenReturn(List.of("manager-doc", "next"));
        when(periods.findById("old")).thenReturn(Optional.of(outgoing));
        when(periods.findById("new")).thenReturn(Optional.of(incoming));
        when(inventory.listIngredients()).thenReturn(List.of(new IngredientResponse("rice", "Rice", IngredientUnit.KG,
                new BigDecimal("5.000"), false, null, null)));
    }

    private StockHandoverService.CreateRequest request(String quantity) {
        return new StockHandoverService.CreateRequest("request-1", "old", "new", date,
                List.of(new StockHandoverService.LineRequest("rice", IngredientUnit.KG, new BigDecimal(quantity))),
                new BigDecimal("3500.00"), null);
    }

    @Test void createsAndRetriesWithoutChangingInventory() {
        setup(true);
        when(handovers.save(any())).thenAnswer(call -> call.getArgument(0));
        StockHandover saved = service.create("M", request("5.000"));
        assertEquals(new BigDecimal("3500.00"), saved.getRecordedValue());
        assertEquals(new BigDecimal("5.000"), saved.getLines().getFirst().quantity());
        when(handovers.findByRequestId("request-1")).thenReturn(Optional.of(saved));
        assertSame(saved, service.create("M", request("5.000")));
        verify(handovers, times(1)).save(any());
        verify(inventory, times(1)).listIngredients();
    }

    @Test void rejectsExcessQuantityAndNonCurrentManager() {
        setup(true);
        assertThrows(InvalidBusinessStateException.class, () -> service.create("M", request("5.001")));
        verify(handovers, never()).save(any());
        setup(false);
        assertThrows(InvalidBusinessStateException.class, () -> service.create("M", request("1.000")));
    }

    @Test void paymentMustBeFullAndRetryDoesNotRecordTwice() {
        setup(true);
        when(handovers.save(any())).thenAnswer(call -> call.getArgument(0));
        StockHandover saved = service.create("M", request("5.000"));
        when(handovers.findById(saved.getId())).thenReturn(Optional.of(saved));
        assertThrows(InvalidBusinessStateException.class, () -> service.pay("M", saved.getId(),
                new StockHandoverService.PaymentRequest("payment-1", new BigDecimal("3501.00"), date)));
        assertThrows(InvalidBusinessStateException.class, () -> service.pay("M", saved.getId(),
                new StockHandoverService.PaymentRequest("payment-1", new BigDecimal("1000.00"), date)));
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(StockHandover.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        service.pay("M", saved.getId(), new StockHandoverService.PaymentRequest("payment-1", new BigDecimal("3500.00"), date));
        ReflectionTestUtils.setField(saved, "paymentRequestId", "payment-1");
        ReflectionTestUtils.setField(saved, "paidAmount", new BigDecimal("3500.00"));
        ReflectionTestUtils.setField(saved, "paymentDate", date);
        assertSame(saved, service.pay("M", saved.getId(), new StockHandoverService.PaymentRequest(
                "payment-1", new BigDecimal("3500.00"), date)));
        verify(mongo, times(1)).updateFirst(any(Query.class), any(Update.class), eq(StockHandover.class));
    }
}
