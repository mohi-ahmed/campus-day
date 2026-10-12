package com.spark.campusday.meal.billing.model;

import com.spark.campusday.meal.price.model.PriceTarget;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Document(collection = "final_year_monthly_bills")
@CompoundIndexes({
    @CompoundIndex(name = "final_bill_student_month", def = "{'studentId': 1, 'year': 1, 'month': 1}", unique = true),
    @CompoundIndex(name = "final_bill_month", def = "{'year': 1, 'month': 1}")
})
public class FinalYearBill {
    @Id private String id;
    private String studentId;
    private int year;
    private int month;
    private List<Line> lines;
    private BigDecimal total;
    private BigDecimal paid;
    private List<MonthlyPayment> paymentHistory;
    private Instant finalizedAt;
    protected FinalYearBill() {}
    public FinalYearBill(String studentId, int year, int month, List<Line> lines, BigDecimal total, Instant finalizedAt) {
        this.studentId = studentId; this.year = year; this.month = month;
        this.lines = List.copyOf(lines); this.total = total; this.paid = new BigDecimal("0.00");
        this.paymentHistory = List.of(); this.finalizedAt = finalizedAt;
    }
    public record Line(LocalDate mealDate, String orderId, String menuId, PriceTarget target, String itemId,
                       String label, int quantity, String priceVersionId, BigDecimal unitPrice, BigDecimal lineTotal) {}
}
