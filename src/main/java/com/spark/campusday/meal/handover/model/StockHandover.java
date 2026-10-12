package com.spark.campusday.meal.handover.model;

import com.spark.campusday.meal.inventory.model.IngredientUnit;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Document(collection = "meal_stock_handovers")
@CompoundIndexes({
        @CompoundIndex(name = "handover_periods", def = "{'outgoingPeriodId': 1, 'incomingPeriodId': 1}")
})
public class StockHandover {
    @Id private String id;
    @Indexed(unique = true) private String requestId;
    @Indexed(unique = true) private String outgoingPeriodId;
    private String incomingPeriodId;
    private LocalDate handoverDate;
    private List<Line> lines;
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal recordedValue;
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal paidAmount;
    private LocalDate paymentDate;
    private String paymentRequestId;
    private String paidByStudentId;
    private String recordedByStudentId;
    private String note;
    private Instant createdAt;
    private Instant paidAt;

    protected StockHandover() {}
    public StockHandover(String requestId, String outgoingPeriodId, String incomingPeriodId,
                         LocalDate handoverDate, List<Line> lines, BigDecimal recordedValue,
                         String recordedByStudentId, String note, Instant createdAt) {
        this.id = UUID.randomUUID().toString();
        this.requestId = requestId; this.outgoingPeriodId = outgoingPeriodId;
        this.incomingPeriodId = incomingPeriodId; this.handoverDate = handoverDate;
        this.lines = List.copyOf(lines); this.recordedValue = recordedValue;
        this.paidAmount = new BigDecimal("0.00"); this.recordedByStudentId = recordedByStudentId;
        this.note = note; this.createdAt = createdAt;
    }
    public record Line(String ingredientId, String ingredientName, IngredientUnit unit, BigDecimal quantity) {}
}
