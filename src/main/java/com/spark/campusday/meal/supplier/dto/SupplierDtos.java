package com.spark.campusday.meal.supplier.dto;

import com.spark.campusday.meal.supplier.model.SupplierAccountEntryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class SupplierDtos {
    private SupplierDtos() { }

    public record SupplierRequest(@NotBlank String name, String contact, String note) { }
    public record SupplierResponse(String id, String name, String contact, String note,
                                   BigDecimal outstanding, Instant createdAt, Instant updatedAt) { }
    public record SupplierPage(List<SupplierResponse> suppliers, int page, int size,
                               long totalElements, int totalPages) { }
    public record PaymentRequest(@NotBlank String requestId, @NotNull @DecimalMin("0.01") BigDecimal amount,
                                 @NotNull LocalDate paymentDate, String note) { }
    public record AccountEntryResponse(String id, String supplierId, SupplierAccountEntryType type,
                                       BigDecimal amount, BigDecimal balanceAfter, LocalDate date,
                                       String note, Instant createdAt) { }
    public record PaymentPage(List<AccountEntryResponse> payments, int page, int size,
                              long totalElements, int totalPages) { }
    public record DueResponse(String supplierId, BigDecimal outstanding) { }
}
