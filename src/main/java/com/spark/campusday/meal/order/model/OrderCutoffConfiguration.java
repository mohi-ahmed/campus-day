package com.spark.campusday.meal.order.model;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalTime;

@Getter
@Document(collection = "meal_order_configuration")
public class OrderCutoffConfiguration {
    public static final String ID = "final-year-ordering";

    @Id
    private String id = ID;
    private CutoffRule orderCutoff;
    private CutoffRule editCutoff;
    private CutoffRule cancelCutoff;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    protected OrderCutoffConfiguration() {
        // Spring Data MongoDB
    }

    public OrderCutoffConfiguration(CutoffRule orderCutoff, CutoffRule editCutoff, CutoffRule cancelCutoff) {
        revise(orderCutoff, editCutoff, cancelCutoff);
    }

    public void revise(CutoffRule orderCutoff, CutoffRule editCutoff, CutoffRule cancelCutoff) {
        this.orderCutoff = orderCutoff;
        this.editCutoff = editCutoff;
        this.cancelCutoff = cancelCutoff;
    }

    public record CutoffRule(int daysBeforeMeal, LocalTime time) {}
}
