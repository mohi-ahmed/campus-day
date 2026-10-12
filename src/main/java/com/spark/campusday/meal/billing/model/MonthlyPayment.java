package com.spark.campusday.meal.billing.model;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Document(collection = "monthly_meal_payments")
@CompoundIndex(name = "payment_student_month_date", def = "{'studentId': 1, 'year': 1, 'month': 1, 'paymentDate': 1}")
@CompoundIndex(name = "payment_office_month_date", def = "{'year': 1, 'month': 1, 'channel': 1, 'paymentDate': 1}")
public class MonthlyPayment {
    @Id private String id;
    private String studentId;
    private int year;
    private int month;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String channel;
    private String recordedByStudentId;
    private String reference;
    private Instant recordedAt;
    protected MonthlyPayment() {}
    public MonthlyPayment(String studentId, int year, int month, BigDecimal amount, LocalDate paymentDate,
                          String channel, String recordedByStudentId, String reference, Instant recordedAt) {
        this.id = UUID.randomUUID().toString();
        this.studentId = studentId; this.year = year; this.month = month; this.amount = amount;
        this.paymentDate = paymentDate; this.channel = channel; this.recordedByStudentId = recordedByStudentId;
        this.reference = reference; this.recordedAt = recordedAt;
    }
}
