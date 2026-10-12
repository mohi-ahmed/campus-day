package com.spark.campusday.meal.supplier.model;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Append-only supplier account; latest balanceAfter is the authoritative due. */
@Getter
@Document(collection = "meal_supplier_account_entries")
@CompoundIndexes({
        @CompoundIndex(name = "supplier_account_sequence", def = "{'supplierId': 1, 'sequence': -1}", unique = true),
        @CompoundIndex(name = "supplier_account_history", def = "{'supplierId': 1, 'type': 1, 'businessDate': -1}")
})
public class SupplierAccountEntry {
    @Id private String id;
    private String supplierId;
    private long sequence;
    private SupplierAccountEntryType type;
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal amount;
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal balanceAfter;
    @Indexed private LocalDate businessDate;
    private String managerStudentId;
    private String note;
    @Indexed(unique = true) private String sourceReference;
    @CreatedDate private Instant createdAt;

    protected SupplierAccountEntry() { }

    public SupplierAccountEntry(String supplierId, long sequence, SupplierAccountEntryType type,
                                BigDecimal amount, BigDecimal balanceAfter, LocalDate businessDate,
                                String managerStudentId, String note, String sourceReference) {
        this.supplierId = supplierId;
        this.sequence = sequence;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.businessDate = businessDate;
        this.managerStudentId = managerStudentId;
        this.note = note;
        this.sourceReference = sourceReference;
    }
}
