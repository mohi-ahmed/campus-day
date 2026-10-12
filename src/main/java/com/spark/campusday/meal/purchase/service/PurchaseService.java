package com.spark.campusday.meal.purchase.service;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.inventory.model.Ingredient;
import com.spark.campusday.meal.inventory.repository.IngredientRepository;
import com.spark.campusday.meal.inventory.service.InventoryService;
import com.spark.campusday.meal.purchase.dto.PurchaseDtos.*;
import com.spark.campusday.meal.purchase.model.*;
import com.spark.campusday.meal.purchase.repository.PurchaseRepository;
import com.spark.campusday.meal.supplier.model.Supplier;
import com.spark.campusday.meal.supplier.repository.SupplierRepository;
import com.spark.campusday.meal.supplier.service.SupplierAccountService;
import com.spark.campusday.meal.supplier.service.SupplierService;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseService {
    private final PurchaseRepository purchases;
    private final IngredientRepository ingredients;
    private final SupplierRepository suppliers;
    private final SupplierService supplierService;
    private final SupplierAccountService accounts;
    private final InventoryService inventory;
    private final StudentService students;
    private final MealManagerAccess managerAccess;

    public PurchaseResponse create(String managerStudentId, CreateRequest request) {
        String managerId = requireManager(managerStudentId);
        validateStructure(request);
        Supplier supplier = supplierService.requireSupplier(request.supplierId());
        Map<String, Ingredient> ingredientMap = ingredients.findAllById(request.lines().stream()
                .map(LineRequest::ingredientId).toList()).stream()
                .collect(Collectors.toMap(Ingredient::getId, ingredient -> ingredient));
        List<PurchaseLine> lines = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (LineRequest input : request.lines()) {
            Ingredient ingredient = ingredientMap.get(input.ingredientId());
            if (ingredient == null) throw new ResourceNotFoundException("Ingredient not found: " + input.ingredientId());
            if (!seen.add(ingredient.getId())) throw new InvalidRequestException("Duplicate ingredient line");
            if (input.unit() != ingredient.getUnit()) {
                throw new InvalidBusinessStateException("Purchase unit must match ingredient stock unit");
            }
            BigDecimal quantity = quantity(input.quantity());
            BigDecimal unitCost = money(input.unitCost(), false);
            BigDecimal exactTotal = quantity.multiply(unitCost);
            if (exactTotal.stripTrailingZeros().scale() > 2) {
                throw new InvalidRequestException("Line total cannot contain a fraction of a paisa");
            }
            BigDecimal lineTotal = exactTotal.setScale(2, RoundingMode.UNNECESSARY);
            lines.add(new PurchaseLine(ingredient.getId(), ingredient.getName(), ingredient.getUnit(),
                    quantity, unitCost, lineTotal));
            total = total.add(lineTotal);
        }
        BigDecimal initialPaid = request.initialPaid() == null ? BigDecimal.ZERO.setScale(2)
                : money(request.initialPaid(), false);
        if (request.paymentMode() == PurchasePaymentMode.CASH) {
            if (request.initialPaid() != null && initialPaid.compareTo(total) != 0) {
                throw new InvalidRequestException("Cash purchases must be fully paid");
            }
            initialPaid = total;
        }
        if (initialPaid.compareTo(total) > 0) throw new InvalidRequestException("Initial paid amount exceeds purchase total");
        Purchase candidate = new Purchase(request.requestId(), supplier.getId(), request.purchaseDate(),
                lines, request.paymentMode(), total, initialPaid, normalize(request.note()), managerId);
        Purchase purchase = purchases.findByRequestId(candidate.getRequestId()).orElse(null);
        if (purchase == null) {
            try {
                purchase = purchases.save(candidate);
            } catch (DuplicateKeyException collision) {
                purchase = purchases.findByRequestId(candidate.getRequestId())
                        .orElseThrow(() -> new ConflictException("Purchase request collided; retry", collision));
            }
        }
        if (!sameRequest(purchase, candidate)) throw new ConflictException("Purchase request ID was used for different data");
        return response(finish(purchase, ingredientMap, managerId), supplier.getName());
    }

    /** Resumes a visible PROCESSING record after an interrupted multi-document write. */
    public PurchaseResponse resume(String managerStudentId, String id) {
        String managerId = requireManager(managerStudentId);
        Purchase purchase = purchases.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found: " + id));
        Supplier supplier = supplierService.requireSupplier(purchase.getSupplierId());
        Map<String, Ingredient> ingredientMap = ingredients.findAllById(purchase.getLines().stream()
                .map(PurchaseLine::ingredientId).toList()).stream()
                .collect(Collectors.toMap(Ingredient::getId, ingredient -> ingredient));
        if (ingredientMap.size() != purchase.getLines().size()) {
            throw new InvalidBusinessStateException("Purchase ingredient no longer exists; cannot resume");
        }
        return response(finish(purchase, ingredientMap, managerId), supplier.getName());
    }

    private Purchase finish(Purchase purchase, Map<String, Ingredient> ingredientMap, String managerId) {
        if (purchase.getStatus() == PurchaseStatus.COMPLETED) return purchase;
        for (int i = 0; i < purchase.getLines().size(); i++) {
            PurchaseLine line = purchase.getLines().get(i);
            Ingredient ingredient = ingredientMap.get(line.ingredientId());
            if (ingredient == null || ingredient.getUnit() != line.unit()) {
                throw new InvalidBusinessStateException("Purchase ingredient or unit no longer matches");
            }
            inventory.creditPurchaseLine(ingredient, purchase.getId(), i,
                    line.quantity(), purchase.getPurchaseDate(), managerId);
        }
        accounts.creditPurchase(purchase.getSupplierId(), purchase.getId(),
                purchase.getTotal().subtract(purchase.getInitialPaid()), purchase.getPurchaseDate(), managerId);
        purchase.complete();
        return purchases.save(purchase);
    }

    public PurchaseResponse get(String id) {
        Purchase purchase = purchases.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found: " + id));
        String supplierName = suppliers.findById(purchase.getSupplierId()).map(Supplier::getName)
                .orElse("Supplier unavailable");
        return response(purchase, supplierName);
    }

    public PurchasePage list(String supplierId, LocalDate from, LocalDate to,
                             PurchasePaymentMode mode, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("Invalid purchase page or date range");
        }
        Page<Purchase> result = purchases.search(supplierId, from, to, mode,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("purchaseDate"), Sort.Order.desc("createdAt"))));
        List<String> ids = result.getContent().stream().map(Purchase::getSupplierId).distinct().toList();
        Map<String, String> names = suppliers.findAllById(ids).stream()
                .collect(Collectors.toMap(Supplier::getId, Supplier::getName));
        return new PurchasePage(result.getContent().stream()
                .map(purchase -> response(purchase, names.getOrDefault(purchase.getSupplierId(), "Supplier unavailable")))
                .toList(), page, size, result.getTotalElements(), result.getTotalPages());
    }

    private boolean sameRequest(Purchase a, Purchase b) {
        return Objects.equals(a.getSupplierId(), b.getSupplierId())
                && Objects.equals(a.getPurchaseDate(), b.getPurchaseDate())
                && Objects.equals(a.getLines(), b.getLines())
                && a.getPaymentMode() == b.getPaymentMode()
                && a.getTotal().compareTo(b.getTotal()) == 0
                && a.getInitialPaid().compareTo(b.getInitialPaid()) == 0
                && Objects.equals(a.getNote(), b.getNote())
                && Objects.equals(a.getManagerStudentId(), b.getManagerStudentId());
    }

    private void validateStructure(CreateRequest request) {
        if (request == null || request.requestId() == null || request.requestId().isBlank()
                || request.requestId().length() > 100 || request.supplierId() == null
                || request.supplierId().isBlank() || request.purchaseDate() == null
                || request.paymentMode() == null || request.lines() == null || request.lines().isEmpty()
                || request.lines().size() > 100 || request.note() != null && request.note().length() > 500) {
            throw new InvalidRequestException("Valid purchase date, supplier, payment mode and lines are required");
        }
        for (LineRequest line : request.lines()) {
            if (line == null || line.ingredientId() == null || line.ingredientId().isBlank()
                    || line.unit() == null) throw new InvalidRequestException("Ingredient and unit are required for every line");
        }
    }

    private BigDecimal quantity(BigDecimal value) {
        if (value == null || value.signum() <= 0 || value.scale() > 3
                || value.precision() - value.scale() > 9) {
            throw new InvalidRequestException("Quantity must be positive with at most three decimal places");
        }
        return value.setScale(3, RoundingMode.UNNECESSARY);
    }

    private BigDecimal money(BigDecimal value, boolean positive) {
        if (value == null || (positive ? value.signum() <= 0 : value.signum() < 0)
                || value.scale() > 2 || value.precision() - value.scale() > 10) {
            throw new InvalidRequestException("Amount must be nonnegative with at most two decimal places");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    private String requireManager(String registrationId) {
        Student actor = students.getByStudentId(registrationId);
        if (!managerAccess.hasCurrentAccess(actor.getId())) {
            throw new InvalidBusinessStateException("Only a currently assigned Meal Manager may record purchases");
        }
        return actor.getId();
    }

    private String normalize(String note) { return note == null || note.isBlank() ? null : note.trim(); }

    private PurchaseResponse response(Purchase purchase, String supplierName) {
        return new PurchaseResponse(purchase.getId(), purchase.getRequestId(), purchase.getSupplierId(),
                supplierName, purchase.getPurchaseDate(), purchase.getLines(), purchase.getPaymentMode(),
                purchase.getTotal(), purchase.getInitialPaid(), purchase.getTotal().subtract(purchase.getInitialPaid()),
                purchase.getStatus(), purchase.getNote(), purchase.getManagerStudentId(),
                purchase.getCreatedAt(), purchase.getUpdatedAt());
    }
}
