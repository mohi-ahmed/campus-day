package com.spark.campusday.meal.purchase.model;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Document(collection = "meal_purchases")
@CompoundIndexes({
        @CompoundIndex(name = "purchase_supplier_date", def = "{'supplierId': 1, 'purchaseDate': -1}"),
        @CompoundIndex(name = "purchase_date", def = "{'purchaseDate': -1}")
})
public class Purchase {
    @Id private String id;
    @Indexed(unique = true) private String requestId;
    private String supplierId;
    private LocalDate purchaseDate;
    private List<PurchaseLine> lines;
    private PurchasePaymentMode paymentMode;
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal total;
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal initialPaid;
    private String note;
    private String managerStudentId;
    private PurchaseStatus status;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    protected Purchase() { }

    public Purchase(String requestId, String supplierId, LocalDate purchaseDate, List<PurchaseLine> lines,
                    PurchasePaymentMode paymentMode, BigDecimal total, BigDecimal initialPaid,
                    String note, String managerStudentId) {
        this.requestId = requestId;
        this.supplierId = supplierId;
        this.purchaseDate = purchaseDate;
        this.lines = List.copyOf(lines);
        this.paymentMode = paymentMode;
        this.total = total;
        this.initialPaid = initialPaid;
        this.note = note;
        this.managerStudentId = managerStudentId;
        this.status = PurchaseStatus.PROCESSING;
    }

    public void complete() { this.status = PurchaseStatus.COMPLETED; }
}
