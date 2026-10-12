package com.spark.campusday.meal.order;

import com.spark.campusday.meal.order.dto.OrderCutoffRequest;
import com.spark.campusday.meal.order.model.OrderCutoffConfiguration;
import com.spark.campusday.meal.order.repository.OrderCutoffConfigurationRepository;
import com.spark.campusday.meal.order.service.OrderCutoffService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderCutoffServiceTest {
    @Test
    void resolvesConfiguredCutoffInDhaka() {
        var repository = mock(OrderCutoffConfigurationRepository.class);
        when(repository.save(any(OrderCutoffConfiguration.class))).thenAnswer(call -> call.getArgument(0));
        var service = new OrderCutoffService(repository,
                Clock.fixed(Instant.parse("2026-09-24T00:00:00Z"), ZoneId.of("Asia/Dhaka")));
        var rule = new OrderCutoffRequest.Rule(1, LocalTime.of(22, 30));

        var response = service.configure(new OrderCutoffRequest(rule, rule, rule), LocalDate.of(2026, 9, 25));

        assertEquals(Instant.parse("2026-09-24T16:30:00Z"), response.orderCutoffAt());
    }
}
