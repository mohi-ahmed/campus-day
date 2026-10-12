package com.spark.campusday.meal.handover.service;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.handover.model.StockHandover;
import com.spark.campusday.meal.handover.repository.StockHandoverRepository;
import com.spark.campusday.meal.inventory.dto.InventoryResponses.IngredientResponse;
import com.spark.campusday.meal.inventory.model.IngredientUnit;
import com.spark.campusday.meal.inventory.service.InventoryService;
import com.spark.campusday.meal.managementperiod.model.MealManagementPeriod;
import com.spark.campusday.meal.managementperiod.repository.MealManagementPeriodRepository;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StockHandoverService {
    private final StockHandoverRepository handovers;
    private final MealManagementPeriodRepository periods;
    private final InventoryService inventory;
    private final StudentService students;
    private final MealManagerAccess access;
    private final MongoOperations mongo;
    @Qualifier("mealClock")
    private final Clock mealClock;

    public record LineRequest(String ingredientId, IngredientUnit unit, BigDecimal quantity) {}
    public record CreateRequest(String requestId, String outgoingPeriodId, String incomingPeriodId,
                                LocalDate handoverDate, List<LineRequest> lines, BigDecimal recordedValue, String note) {}
    public record PaymentRequest(String requestId, BigDecimal amount, LocalDate paymentDate) {}

    public StockHandover create(String managerStudentId, CreateRequest request) {
        Student actor = manager(managerStudentId);
        if (request == null || blank(request.requestId()) || blank(request.outgoingPeriodId())
                || blank(request.incomingPeriodId()) || request.handoverDate() == null
                || request.lines() == null || request.lines().isEmpty() || request.note() != null && request.note().length() > 500)
            throw new InvalidRequestException("Handover reference, periods, date and stock lines are required");
        if (request.requestId().length() > 100) throw new InvalidRequestException("Handover reference is too long");
        MealManagementPeriod outgoing = period(request.outgoingPeriodId());
        MealManagementPeriod incoming = period(request.incomingPeriodId());
        if (!YearMonth.of(outgoing.getYear(), outgoing.getMonth()).plusMonths(1)
                .equals(YearMonth.of(incoming.getYear(), incoming.getMonth()))
                || !outgoing.getEndDate().isBefore(incoming.getStartDate()))
            throw new InvalidBusinessStateException("Incoming team must manage the following month");
        if (!outgoing.getManagerStudentIds().contains(actor.getId()) && !incoming.getManagerStudentIds().contains(actor.getId()))
            throw new InvalidBusinessStateException("Acting manager must belong to a referenced team");
        if (request.handoverDate().isBefore(outgoing.getStartDate()) || request.handoverDate().isAfter(incoming.getEndDate())
                || request.handoverDate().isAfter(LocalDate.now(mealClock)))
            throw new InvalidRequestException("Handover date must fall within the team transition and not be in the future");
        BigDecimal value = money(request.recordedValue(), true);
        var existing = handovers.findByRequestId(request.requestId());
        if (existing.isPresent()) return matching(existing.get(), request, value);
        Map<String, IngredientResponse> stock = new HashMap<>();
        inventory.listIngredients().forEach(ingredient -> stock.put(ingredient.id(), ingredient));
        Set<String> selected = new HashSet<>();
        List<StockHandover.Line> lines = request.lines().stream().map(line -> {
            if (line == null || blank(line.ingredientId()) || !selected.add(line.ingredientId()))
                throw new InvalidRequestException("Handover ingredients must be unique and identified");
            IngredientResponse ingredient = stock.get(line.ingredientId());
            if (ingredient == null) throw new ResourceNotFoundException("Ingredient not found: " + line.ingredientId());
            if (ingredient.unit() != line.unit()) throw new InvalidBusinessStateException("Stock unit must match the ingredient unit");
            BigDecimal quantity = quantity(line.quantity());
            if (quantity.compareTo(ingredient.currentStock()) > 0)
                throw new InvalidBusinessStateException("Transferred quantity exceeds available stock for " + ingredient.name());
            return new StockHandover.Line(ingredient.id(), ingredient.name(), ingredient.unit(), quantity);
        }).toList();
        if (handovers.findByOutgoingPeriodId(outgoing.getId()).isPresent())
            throw new ConflictException("Outgoing period already has a stock handover");
        try {
            return handovers.save(new StockHandover(request.requestId(), outgoing.getId(), incoming.getId(),
                    request.handoverDate(), lines, value, actor.getId(), request.note(), Instant.now(mealClock)));
        } catch (DuplicateKeyException exception) {
            return handovers.findByRequestId(request.requestId())
                    .map(found -> matching(found, request, value))
                    .orElseThrow(() -> new ConflictException("Outgoing period already has a handover", exception));
        }
    }

    public StockHandover pay(String managerStudentId, String id, PaymentRequest request) {
        Student actor = manager(managerStudentId);
        if (request == null || blank(request.requestId()) || request.requestId().length() > 100
                || request.paymentDate() == null || request.paymentDate().isAfter(LocalDate.now(mealClock)))
            throw new InvalidRequestException("Payment reference and valid date are required");
        StockHandover handover = get(id);
        MealManagementPeriod incoming = period(handover.getIncomingPeriodId());
        if (!incoming.getManagerStudentIds().contains(actor.getId()))
            throw new InvalidBusinessStateException("Payment must be recorded by the incoming management team");
        BigDecimal amount = money(request.amount(), true);
        if (handover.getPaymentRequestId() != null) return matchingPayment(handover, request, amount);
        if (amount.compareTo(handover.getRecordedValue()) != 0)
            throw new InvalidBusinessStateException("Handover payment must equal the full recorded value");
        if (request.paymentDate().isBefore(handover.getHandoverDate()))
            throw new InvalidRequestException("Payment date cannot precede handover");
        Query query = Query.query(Criteria.where("_id").is(id).and("paymentRequestId").is(null));
        Update update = new Update().set("paidAmount", amount).set("paymentDate", request.paymentDate())
                .set("paymentRequestId", request.requestId()).set("paidByStudentId", actor.getId())
                .set("paidAt", Instant.now(mealClock));
        if (mongo.updateFirst(query, update, StockHandover.class).getModifiedCount() != 1)
            return matchingPayment(get(id), request, amount);
        return get(id);
    }

    public StockHandover get(String id) {
        return handovers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Stock handover not found: " + id));
    }
    public List<StockHandover> history() { return handovers.findAllByOrderByHandoverDateDesc(); }
    public List<StockHandover> forPeriod(String periodId) {
        return handovers.findByOutgoingPeriodIdOrIncomingPeriodIdOrderByHandoverDateDesc(periodId, periodId);
    }

    private StockHandover matching(StockHandover found, CreateRequest request, BigDecimal value) {
        boolean sameLines = found.getLines().size() == request.lines().size();
        if (sameLines) for (int index = 0; index < found.getLines().size(); index++) {
            StockHandover.Line saved = found.getLines().get(index);
            LineRequest sent = request.lines().get(index);
            if (sent == null || sent.quantity() == null || !saved.ingredientId().equals(sent.ingredientId())
                    || saved.unit() != sent.unit() || saved.quantity().compareTo(sent.quantity()) != 0) {
                sameLines = false; break;
            }
        }
        if (!found.getOutgoingPeriodId().equals(request.outgoingPeriodId())
                || !found.getIncomingPeriodId().equals(request.incomingPeriodId())
                || !found.getHandoverDate().equals(request.handoverDate()) || !sameLines
                || found.getRecordedValue().compareTo(value) != 0 || !Objects.equals(found.getNote(), request.note()))
            throw new ConflictException("Handover reference was already used for different details");
        return found;
    }
    private StockHandover matchingPayment(StockHandover found, PaymentRequest request, BigDecimal amount) {
        if (!Objects.equals(found.getPaymentRequestId(), request.requestId())
                || found.getPaidAmount().compareTo(amount) != 0 || !Objects.equals(found.getPaymentDate(), request.paymentDate()))
            throw new ConflictException("Handover has already been paid with different details");
        return found;
    }
    private MealManagementPeriod period(String id) {
        return periods.findById(id).orElseThrow(() -> new ResourceNotFoundException("Management period not found: " + id));
    }
    private Student manager(String registrationId) {
        Student actor = students.getByStudentId(registrationId);
        if (!access.hasCurrentAccess(actor.getId())) throw new InvalidBusinessStateException("Only a currently assigned Meal Manager may record handover");
        return actor;
    }
    private BigDecimal money(BigDecimal value, boolean positive) {
        if (value == null || value.signum() < (positive ? 1 : 0) || value.scale() > 2 || value.precision() - value.scale() > 10)
            throw new InvalidRequestException("Invalid BDT amount");
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }
    private BigDecimal quantity(BigDecimal value) {
        if (value == null || value.signum() <= 0 || value.scale() > 3 || value.precision() - value.scale() > 9)
            throw new InvalidRequestException("Invalid transferred quantity");
        return value.setScale(3, RoundingMode.UNNECESSARY);
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
