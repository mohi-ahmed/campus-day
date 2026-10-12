package com.spark.campusday.meal.purchase.dto;

import com.spark.campusday.meal.inventory.model.IngredientUnit;
import com.spark.campusday.meal.purchase.model.PurchaseLine;
import com.spark.campusday.meal.purchase.model.PurchasePaymentMode;
import com.spark.campusday.meal.purchase.model.PurchaseStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class PurchaseDtos {
    private PurchaseDtos() { }

    public record LineRequest(@NotBlank String ingredientId, @NotNull IngredientUnit unit,
                              @NotNull @DecimalMin("0.001") BigDecimal quantity,
                              @NotNull @DecimalMin("0.00") BigDecimal unitCost) { }
    public record CreateRequest(@NotBlank String requestId, @NotBlank String supplierId,
                                @NotNull LocalDate purchaseDate, @NotEmpty List<@Valid LineRequest> lines,
                                @NotNull PurchasePaymentMode paymentMode, BigDecimal initialPaid,
                                String note) { }
    public record PurchaseResponse(String id, String requestId, String supplierId, String supplierName,
                                   LocalDate purchaseDate, List<PurchaseLine> lines,
                                   PurchasePaymentMode paymentMode, BigDecimal total,
                                   BigDecimal initialPaid, BigDecimal initialDue,
                                   PurchaseStatus status, String note, String managerStudentId,
                                   Instant createdAt, Instant updatedAt) { }
    public record PurchasePage(List<PurchaseResponse> purchases, int page, int size,
                               long totalElements, int totalPages) { }
}
