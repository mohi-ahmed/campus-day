package com.spark.campusday.meal.inventory.service;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.inventory.dto.IngredientRequest;
import com.spark.campusday.meal.inventory.dto.InventoryResponses.IngredientResponse;
import com.spark.campusday.meal.inventory.dto.InventoryResponses.IssueHistoryResponse;
import com.spark.campusday.meal.inventory.dto.InventoryResponses.IssueLineResponse;
import com.spark.campusday.meal.inventory.dto.InventoryResponses.MovementHistoryResponse;
import com.spark.campusday.meal.inventory.dto.InventoryResponses.MovementResponse;
import com.spark.campusday.meal.inventory.dto.KitchenIssueRequest;
import com.spark.campusday.meal.inventory.dto.OpeningStockRequest;
import com.spark.campusday.meal.inventory.dto.StockAdjustmentRequest;
import com.spark.campusday.meal.inventory.model.Ingredient;
import com.spark.campusday.meal.inventory.model.IngredientUnit;
import com.spark.campusday.meal.inventory.model.StockMovement;
import com.spark.campusday.meal.inventory.model.StockMovementType;
import com.spark.campusday.meal.inventory.repository.IngredientRepository;
import com.spark.campusday.meal.inventory.repository.StockMovementRepository;
import com.spark.campusday.meal.model.MealType;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(3);

    private final IngredientRepository ingredients;
    private final StockMovementRepository movements;
    private final StudentService students;
    private final MealManagerAccess managerAccess;
    @Qualifier("mealClock")
    private final Clock mealClock;

    public IngredientResponse createIngredient(String managerStudentId, IngredientRequest request) {
        requireManager(managerStudentId);
        validateIngredient(request);
        Ingredient ingredient = new Ingredient(request.name(), request.unit());
        if (ingredients.existsByNameKey(ingredient.getNameKey())) {
            throw new ConflictException("Ingredient already exists: " + ingredient.getName());
        }
        try {
            return ingredientResponse(ingredients.save(ingredient), ZERO, true);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("Ingredient already exists: " + ingredient.getName(), exception);
        }
    }

    public IngredientResponse updateIngredient(String managerStudentId, String id, IngredientRequest request) {
        requireManager(managerStudentId);
        validateIngredient(request);
        Ingredient ingredient = requireIngredient(id);
        var latest = movements.findFirstByIngredientIdOrderBySequenceDesc(id);
        if (latest.isPresent() && ingredient.getUnit() != request.unit()) {
            throw new InvalidBusinessStateException("Unit cannot change after stock movements exist");
        }
        String key = request.name().trim().toLowerCase(Locale.ROOT);
        if (!key.equals(ingredient.getNameKey()) && ingredients.existsByNameKeyAndIdNot(key, id)) {
            throw new ConflictException("Ingredient already exists: " + request.name());
        }
        ingredient.revise(request.name(), request.unit());
        try {
            ingredients.save(ingredient);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("Ingredient already exists: " + request.name(), exception);
        }
        return ingredientResponse(ingredient, latest.map(StockMovement::getBalanceAfter).orElse(ZERO), latest.isEmpty());
    }

    public List<IngredientResponse> listIngredients() {
        List<Ingredient> found = ingredients.findAll(Sort.by("name"));
        if (found.isEmpty()) return List.of();
        Map<String, BigDecimal> balances = movements.latestBalances().stream()
                .collect(Collectors.toMap(StockMovementRepository.BalanceSnapshot::id,
                        StockMovementRepository.BalanceSnapshot::balanceAfter));
        return found.stream().map(ingredient -> ingredientResponse(ingredient,
                balances.getOrDefault(ingredient.getId(), ZERO), !balances.containsKey(ingredient.getId()))).toList();
    }

    public IngredientResponse getIngredient(String id) {
        Ingredient ingredient = requireIngredient(id);
        var latest = movements.findFirstByIngredientIdOrderBySequenceDesc(id);
        return ingredientResponse(ingredient, latest.map(StockMovement::getBalanceAfter).orElse(ZERO), latest.isEmpty());
    }

    public MovementResponse open(String managerStudentId, OpeningStockRequest request) {
        String managerId = requireManager(managerStudentId);
        if (request == null) throw new InvalidRequestException("Opening stock is required");
        Ingredient ingredient = requireIngredient(request.ingredientId());
        requireUnit(ingredient, request.unit());
        return record(ingredient, StockMovementType.OPENING, request.quantity(),
                LocalDate.now(mealClock), null, null, managerId);
    }

    public MovementResponse adjust(String managerStudentId, StockAdjustmentRequest request) {
        String managerId = requireManager(managerStudentId);
        if (request == null || (request.type() != StockMovementType.ADJUSTMENT_IN
                && request.type() != StockMovementType.ADJUSTMENT_OUT)
                || request.note() == null || request.note().isBlank() || request.note().length() > 500) {
            throw new InvalidRequestException("Adjustment direction and reason are required");
        }
        Ingredient ingredient = requireIngredient(request.ingredientId());
        requireUnit(ingredient, request.unit());
        return record(ingredient, request.type(), request.quantity(), LocalDate.now(mealClock),
                null, request.note().trim(), managerId);
    }

    public MovementResponse issue(String managerStudentId, KitchenIssueRequest request) {
        String managerId = requireManager(managerStudentId);
        if (request == null || request.date() == null || request.mealType() == null
                || request.note() != null && request.note().length() > 500) {
            throw new InvalidRequestException("Kitchen issue date and meal are required");
        }
        Ingredient ingredient = requireIngredient(request.ingredientId());
        requireUnit(ingredient, request.unit());
        return record(ingredient, StockMovementType.KITCHEN_ISSUE, request.quantity(),
                request.date(), request.mealType(), request.note(), managerId);
    }

    /** Controlled, retry-safe stock inflow for a validated purchase line. */
    public MovementResponse creditPurchaseLine(Ingredient ingredient, String purchaseId, int lineIndex,
                                               BigDecimal quantity, LocalDate date, String managerDocumentId) {
        if (ingredient == null || ingredient.getId() == null || purchaseId == null
                || purchaseId.isBlank() || lineIndex < 0 || date == null || managerDocumentId == null) {
            throw new InvalidRequestException("Valid purchase stock reference is required");
        }
        return record(ingredient, StockMovementType.PURCHASE_IN, quantity, date, null,
                null, managerDocumentId, "PURCHASE:" + purchaseId + ":" + lineIndex);
    }

    public MovementHistoryResponse history(String ingredientId, int page, int size) {
        requireIngredient(ingredientId);
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidRequestException("Movement page must be nonnegative and size 1–100");
        }
        Page<StockMovement> found = movements.findByIngredientIdOrderBySequenceDesc(
                ingredientId, PageRequest.of(page, size));
        return new MovementHistoryResponse(ingredientId, found.getContent().stream().map(this::movementResponse).toList(),
                page, size, found.getTotalElements(), found.getTotalPages());
    }

    public IssueHistoryResponse issuesForDate(LocalDate date, int page, int size) {
        if (date == null) throw new InvalidRequestException("Issue date is required");
        validatePage(page, size);
        Page<StockMovement> found = movements.findIssuesForDate(date, PageRequest.of(page, size));
        List<String> ids = found.getContent().stream().map(StockMovement::getIngredientId).distinct().toList();
        Map<String, String> names = ingredients.findAllById(ids).stream()
                .collect(Collectors.toMap(Ingredient::getId, Ingredient::getName));
        List<IssueLineResponse> rows = found.getContent().stream().map(movement ->
                new IssueLineResponse(movement.getId(), movement.getIngredientId(),
                        names.getOrDefault(movement.getIngredientId(), "Ingredient unavailable"),
                        movement.getUnit(), movement.getQuantity(), movement.getMealType(),
                        movement.getBusinessDate(), movement.getNote(), movement.getOccurredAt())).toList();
        return new IssueHistoryResponse(date, rows, page, size, found.getTotalElements(), found.getTotalPages());
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidRequestException("Movement page must be nonnegative and size 1–100");
        }
    }

    private MovementResponse record(Ingredient ingredient, StockMovementType type, BigDecimal requested,
                                    LocalDate date, MealType mealType, String note, String managerId) {
        return record(ingredient, type, requested, date, mealType, note, managerId, null);
    }

    private MovementResponse record(Ingredient ingredient, StockMovementType type, BigDecimal requested,
                                    LocalDate date, MealType mealType, String note, String managerId,
                                    String sourceReference) {
        BigDecimal quantity = requireQuantity(requested);
        if (sourceReference != null) {
            var existing = movements.findBySourceReference(sourceReference);
            if (existing.isPresent()) return matchingCredit(existing.get(), ingredient, quantity);
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            var latest = movements.findFirstByIngredientIdOrderBySequenceDesc(ingredient.getId());
            if (type == StockMovementType.OPENING && latest.isPresent()) {
                throw new ConflictException("Opening stock has already been established");
            }
            BigDecimal before = latest.map(StockMovement::getBalanceAfter).orElse(ZERO);
            boolean incoming = type == StockMovementType.OPENING || type == StockMovementType.ADJUSTMENT_IN
                    || type == StockMovementType.PURCHASE_IN;
            if (!incoming && before.compareTo(quantity) < 0) {
                throw new InvalidBusinessStateException("Insufficient stock; available " + before + " " + ingredient.getUnit());
            }
            BigDecimal after = incoming ? before.add(quantity) : before.subtract(quantity);
            StockMovement movement = new StockMovement(ingredient.getId(),
                    latest.map(value -> value.getSequence() + 1).orElse(1L), type, quantity, after,
                    ingredient.getUnit(), date, Instant.now(mealClock), mealType, note, managerId, sourceReference);
            try {
                return movementResponse(movements.save(movement));
            } catch (DuplicateKeyException collision) {
                if (sourceReference != null) {
                    var existing = movements.findBySourceReference(sourceReference);
                    if (existing.isPresent()) return matchingCredit(existing.get(), ingredient, quantity);
                }
                if (attempt == 2) throw new ConflictException("Stock changed concurrently; retry the operation", collision);
            }
        }
        throw new ConflictException("Stock changed concurrently; retry the operation");
    }

    private MovementResponse matchingCredit(StockMovement existing, Ingredient ingredient, BigDecimal quantity) {
        if (existing.getType() != StockMovementType.PURCHASE_IN
                || !existing.getIngredientId().equals(ingredient.getId())
                || existing.getQuantity().compareTo(quantity) != 0) {
            throw new ConflictException("Purchase stock reference conflicts with an existing movement");
        }
        return movementResponse(existing);
    }

    private String requireManager(String registrationId) {
        Student actor = students.getByStudentId(registrationId);
        if (!managerAccess.hasCurrentAccess(actor.getId())) {
            throw new InvalidBusinessStateException("Only a currently assigned Meal Manager may change stock");
        }
        return actor.getId();
    }

    private Ingredient requireIngredient(String id) {
        return ingredients.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found: " + id));
    }

    private void validateIngredient(IngredientRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()
                || request.name().length() > 100 || request.unit() == null) {
            throw new InvalidRequestException("Ingredient name and stock unit are required");
        }
    }

    private void requireUnit(Ingredient ingredient, IngredientUnit requested) {
        if (requested == null || ingredient.getUnit() != requested) {
            throw new InvalidBusinessStateException("Stock unit must match the ingredient unit");
        }
    }

    private BigDecimal requireQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0 || quantity.scale() > 3
                || quantity.precision() - quantity.scale() > 9) {
            throw new InvalidBusinessStateException("Stock quantity must be positive with at most three decimal places");
        }
        return quantity.setScale(3, RoundingMode.UNNECESSARY);
    }

    private IngredientResponse ingredientResponse(Ingredient ingredient, BigDecimal balance, boolean openingAllowed) {
        return new IngredientResponse(ingredient.getId(), ingredient.getName(), ingredient.getUnit(),
                balance, openingAllowed, ingredient.getCreatedAt(), ingredient.getUpdatedAt());
    }

    private MovementResponse movementResponse(StockMovement movement) {
        return new MovementResponse(movement.getId(), movement.getIngredientId(), movement.getSequence(),
                movement.getType(), movement.getQuantity(), movement.getBalanceAfter(), movement.getUnit(),
                movement.getBusinessDate(), movement.getOccurredAt(), movement.getMealType(),
                movement.getNote(), movement.getManagerStudentId(), movement.getSourceReference());
    }
}
