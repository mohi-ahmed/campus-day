package com.spark.campusday.meal.billing.model;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Document(collection = "fixed_monthly_charge_versions")
@CompoundIndex(name = "charge_month_revision", def = "{'year': 1, 'month': 1, 'effectiveAt': -1}", unique = true)
public class FixedChargeVersion {
    @Id private String id;
    private int year;
    private int month;
    private BigDecimal amount;
    private String recordedByStudentId;
    private Instant effectiveAt;
    protected FixedChargeVersion() {}
    public FixedChargeVersion(int year, int month, BigDecimal amount, String recordedByStudentId, Instant effectiveAt) {
        this.year = year; this.month = month; this.amount = amount;
        this.recordedByStudentId = recordedByStudentId; this.effectiveAt = effectiveAt;
    }
}
