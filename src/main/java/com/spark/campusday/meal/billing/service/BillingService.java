package com.spark.campusday.meal.billing.service;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.billing.model.FinalYearBill;
import com.spark.campusday.meal.billing.model.FixedChargeVersion;
import com.spark.campusday.meal.billing.model.MonthlyPayment;
import com.spark.campusday.meal.billing.repository.FinalYearBillRepository;
import com.spark.campusday.meal.billing.repository.FixedChargeVersionRepository;
import com.spark.campusday.meal.billing.repository.MonthlyPaymentRepository;
import com.spark.campusday.meal.menu.model.DailyMealMenu;
import com.spark.campusday.meal.menu.repository.DailyMealMenuRepository;
import com.spark.campusday.meal.order.model.FinalYearMealOrder;
import com.spark.campusday.meal.order.model.MealChoiceKind;
import com.spark.campusday.meal.order.model.OrderStatus;
import com.spark.campusday.meal.order.repository.FinalYearMealOrderRepository;
import com.spark.campusday.meal.price.model.MealPriceVersion;
import com.spark.campusday.meal.price.model.PriceTarget;
import com.spark.campusday.meal.price.repository.MealPriceVersionRepository;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.model.AcademicYear;
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
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BillingService {
    private static final ZoneId DHAKA = ZoneId.of("Asia/Dhaka");
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final StudentService students;
    private final MealManagerAccess managerAccess;
    private final FixedChargeVersionRepository charges;
    private final MonthlyPaymentRepository payments;
    private final FinalYearBillRepository finalBills;
    private final FinalYearMealOrderRepository orders;
    private final DailyMealMenuRepository menus;
    private final MealPriceVersionRepository prices;
    private final MongoOperations mongo;
    @Qualifier("mealClock")
    private final Clock mealClock;

    public record Line(LocalDate mealDate, String orderId, String menuId, PriceTarget target, String itemId, String label,
                       int quantity, String priceVersionId, BigDecimal unitPrice, BigDecimal lineTotal, boolean pricePending) {}
    public record Bill(String studentId, int year, int month, String model, String status, BigDecimal amount,
                       BigDecimal paid, BigDecimal outstanding, BigDecimal credit, List<Line> lines,
                       List<MonthlyPayment> paymentHistory) {}
    public record OfficeSummary(int year, int month, BigDecimal charge, BigDecimal collected,
                                List<MonthlyPayment> payments) {}
    public record CollectionRow(String studentId, String studentName, String status, BigDecimal total,
                                BigDecimal paid, BigDecimal outstanding, boolean pricePending) {}

    public FixedChargeVersion setCharge(String managerStudentId, int year, int month, BigDecimal amount) {
        manager(managerStudentId);
        period(year, month);
        BigDecimal value = money(amount, false);
        var current = charges.findFirstByYearAndMonthOrderByEffectiveAtDesc(year, month);
        if (current.isPresent() && current.get().getAmount().compareTo(value) == 0) return current.get();
        Instant now = Instant.now(mealClock).truncatedTo(ChronoUnit.MILLIS);
        if (current.isPresent() && !now.isAfter(current.get().getEffectiveAt()))
            throw new ConflictException("Charge changed at this instant; retry the save");
        try {
            return charges.save(new FixedChargeVersion(year, month, value,
                    students.getByStudentId(managerStudentId).getId(), now));
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("A charge revision already exists for this instant", exception);
        }
    }

    public List<FixedChargeVersion> chargeHistory(int year, int month) {
        period(year, month);
        return charges.findByYearAndMonthOrderByEffectiveAtDesc(year, month);
    }

    public Bill bill(String studentRegistrationId, int year, int month) {
        YearMonth billingMonth = period(year, month);
        Student student = students.getByStudentId(studentRegistrationId);
        if (!student.getAcademicYear().isFinalYear())
            throw new InvalidBusinessStateException("Meal billing is available only to Final Year students");
        return bill(student, billingMonth);
    }

    public List<CollectionRow> finalYearCollections(String managerStudentId, int year, int month) {
        manager(managerStudentId);
        YearMonth billingMonth = period(year, month);
        return students.listByAcademicYear(AcademicYear.FINAL_YEAR).stream().map(student -> {
            Bill bill = bill(student, billingMonth);
            return new CollectionRow(student.getStudentId(), student.getName(), bill.status(), bill.amount(),
                    bill.paid(), bill.outstanding(), bill.lines().stream().anyMatch(Line::pricePending));
        }).toList();
    }

    private Bill bill(Student student, YearMonth billingMonth) {
        int year = billingMonth.getYear();
        int month = billingMonth.getMonthValue();
        var finalized = finalBills.findByStudentIdAndYearAndMonth(student.getId(), year, month);
        if (finalized.isPresent()) {
            FinalYearBill snapshot = finalized.get();
            List<Line> lines = snapshot.getLines().stream().map(line -> new Line(line.mealDate(), line.orderId(), line.menuId(),
                    line.target(), line.itemId(), line.label(), line.quantity(), line.priceVersionId(),
                    line.unitPrice(), line.lineTotal(), false)).toList();
            return response(student.getStudentId(), year, month, "FINAL_YEAR", "FINALIZED", snapshot.getTotal(),
                    snapshot.getPaid(), lines, snapshot.getPaymentHistory());
        }
        List<Line> lines = preview(student.getId(), billingMonth, Instant.now(mealClock));
        BigDecimal runningTotal = lines.stream().anyMatch(Line::pricePending)
                ? null
                : lines.stream().map(Line::lineTotal).reduce(ZERO, BigDecimal::add);
        return response(student.getStudentId(), year, month, "FINAL_YEAR", "PENDING_FINALIZATION",
                runningTotal, ZERO, lines, List.of());
    }

    public Bill finalizeBill(String managerStudentId, String studentRegistrationId, int year, int month) {
        manager(managerStudentId);
        YearMonth billingMonth = period(year, month);
        Student student = students.getByStudentId(studentRegistrationId);
        if (!student.getAcademicYear().isFinalYear()) throw new InvalidBusinessStateException("Only Final Year bills can be finalized");
        if (finalBills.findByStudentIdAndYearAndMonth(student.getId(), year, month).isPresent())
            return bill(studentRegistrationId, year, month);
        if (!billingMonth.isBefore(YearMonth.now(mealClock.withZone(DHAKA))))
            throw new InvalidBusinessStateException("Final Year bill can be finalized after the billing month ends");
        Instant finalizedAt = Instant.now(mealClock);
        List<Line> lines = preview(student.getId(), billingMonth, finalizedAt);
        if (lines.stream().anyMatch(Line::pricePending))
            throw new InvalidBusinessStateException("Required prices are pending");
        BigDecimal total = lines.stream().map(Line::lineTotal).reduce(ZERO, BigDecimal::add);
        List<FinalYearBill.Line> snapshot = lines.stream().map(line -> new FinalYearBill.Line(line.mealDate(),
                line.orderId(), line.menuId(), line.target(), line.itemId(),
                line.label(), line.quantity(), line.priceVersionId(), line.unitPrice(), line.lineTotal())).toList();
        try {
            finalBills.save(new FinalYearBill(student.getId(), year, month, snapshot, total, finalizedAt));
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("Bill was finalized concurrently", exception);
        }
        return bill(studentRegistrationId, year, month);
    }

    /** College Office development boundary: authorization is deferred to Security. */
    public MonthlyPayment recordOfficePayment(String studentRegistrationId, int year, int month,
                                              BigDecimal amount, LocalDate paymentDate, String reference) {
        Student student = students.getByStudentId(studentRegistrationId);
        if (!student.getAcademicYear().isFixedChargeYear()) throw new InvalidBusinessStateException("College Office payments are for Years 1–4");
        return record(student, year, month, amount, paymentDate, "COLLEGE_OFFICE", null, reference);
    }

    public MonthlyPayment recordFinalYearCollection(String managerStudentId, String studentRegistrationId,
                                                                   int year, int month, BigDecimal amount,
                                                                   LocalDate paymentDate, String reference) {
        Student manager = manager(managerStudentId);
        Student student = students.getByStudentId(studentRegistrationId);
        if (!student.getAcademicYear().isFinalYear()) throw new InvalidBusinessStateException("Final Year collection requires a Final Year student");
        BigDecimal value = money(amount, true);
        period(year, month);
        FinalYearBill current = finalBills.findByStudentIdAndYearAndMonth(student.getId(), year, month)
                .orElseThrow(() -> new InvalidBusinessStateException("Bill is pending finalization"));
        MonthlyPayment entry = paymentEntry(student, year, month, value, paymentDate, "MEAL_MANAGER", manager.getId(), reference);
        Query query = Query.query(Criteria.where("_id").is(current.getId())
                .and("paid").lte(current.getTotal().subtract(value)));
        Update update = new Update().inc("paid", value).push("paymentHistory", entry);
        if (mongo.updateFirst(query, update, FinalYearBill.class).getModifiedCount() != 1)
            throw new InvalidBusinessStateException("Payment exceeds finalized outstanding balance");
        return entry;
    }

    public OfficeSummary officeSummary(int year, int month) {
        period(year, month);
        List<MonthlyPayment> entries = payments.findByYearAndMonthAndChannelOrderByPaymentDateAsc(year, month, "COLLEGE_OFFICE");
        return new OfficeSummary(year, month, charges.findFirstByYearAndMonthOrderByEffectiveAtDesc(year, month)
                .map(FixedChargeVersion::getAmount).orElse(null),
                entries.stream().map(MonthlyPayment::getAmount).reduce(ZERO, BigDecimal::add), entries);
    }

    private MonthlyPayment record(Student student, int year, int month, BigDecimal amount, LocalDate date,
                                  String channel, String actor, String reference) {
        MonthlyPayment entry = paymentEntry(student, year, month, amount, date, channel, actor, reference);
        return payments.save(entry);
    }

    private MonthlyPayment paymentEntry(Student student, int year, int month, BigDecimal amount, LocalDate date,
                                        String channel, String actor, String reference) {
        YearMonth billingMonth = period(year, month);
        BigDecimal value = money(amount, true);
        if (date == null || date.isAfter(LocalDate.now(mealClock.withZone(DHAKA))))
            throw new InvalidRequestException("Payment date must exist and cannot be in the future");
        if (date.isBefore(billingMonth.atDay(1))) throw new InvalidRequestException("Payment date precedes billing month");
        return new MonthlyPayment(student.getId(), year, month, value, date, channel, actor,
                reference == null || reference.isBlank() ? null : reference.trim(), Instant.now(mealClock));
    }

    private Bill response(String studentId, int year, int month, String model, String status, BigDecimal amount,
                          BigDecimal paid, List<Line> lines, List<MonthlyPayment> history) {
        BigDecimal difference = amount == null ? null : amount.subtract(paid);
        return new Bill(studentId, year, month, model, status, amount, paid,
                difference == null ? null : difference.max(ZERO),
                difference == null ? null : difference.negate().max(ZERO), lines, history);
    }

    private List<Line> preview(String studentId, YearMonth month, Instant asOf) {
        List<FinalYearMealOrder> active = orders.findHistory(studentId, month.atDay(1), month.atEndOfMonth())
                .stream().filter(order -> order.getStatus() == OrderStatus.SUBMITTED).toList();
        if (active.isEmpty()) return List.of();
        Set<String> menuIds = new HashSet<>();
        active.forEach(order -> menuIds.add(order.getMenuId()));
        Map<String, DailyMealMenu> menuMap = new HashMap<>();
        menus.findAllById(menuIds).forEach(menu -> menuMap.put(menu.getId(), menu));
        Map<String, MealPriceVersion> latest = new HashMap<>();
        prices.findForMenusNewestFirst(menuIds).stream()
                .filter(price -> !price.getEffectiveAt().isAfter(asOf))
                .forEach(price -> latest.putIfAbsent(key(price.getMenuId(), price.getTarget(), price.getItemId()), price));
        List<Line> lines = new ArrayList<>();
        for (FinalYearMealOrder order : active) {
            DailyMealMenu menu = menuMap.get(order.getMenuId());
            if (menu == null) throw new InvalidBusinessStateException("Ordered menu is missing");
            if (order.isBreakfast()) addLine(lines, order, latest, PriceTarget.BREAKFAST_PACKAGE, null,
                    menu.getBreakfast().packageDescription(), 1);
            addChoice(lines, order, latest, order.getLunch(), menu.getLunch(), PriceTarget.LUNCH_FULL_MEAL, PriceTarget.LUNCH_MENU_ITEM);
            addChoice(lines, order, latest, order.getDinner(), menu.getDinner(), PriceTarget.DINNER_FULL_MEAL, PriceTarget.DINNER_MENU_ITEM);
        }
        return List.copyOf(lines);
    }

    private void addChoice(List<Line> lines, FinalYearMealOrder order, Map<String, MealPriceVersion> latest,
                           FinalYearMealOrder.MealChoice choice, DailyMealMenu.MainMeal menu,
                           PriceTarget full, PriceTarget item) {
        if (choice.kind() == MealChoiceKind.FULL_MEAL) {
            addLine(lines, order, latest, full, null, menu.fullMealDescription(), 1);
        } else if (choice.kind() == MealChoiceKind.ITEMS) {
            choice.items().forEach(selected -> addLine(lines, order, latest, item, selected.itemId(), selected.name(), selected.quantity()));
        }
    }

    private void addLine(List<Line> lines, FinalYearMealOrder order, Map<String, MealPriceVersion> latest,
                         PriceTarget target, String itemId, String label, int quantity) {
        MealPriceVersion price = latest.get(key(order.getMenuId(), target, itemId));
        lines.add(new Line(order.getMealDate(), order.getId(), order.getMenuId(), target, itemId, label, quantity,
                price == null ? null : price.getId(), price == null ? null : price.getAmount(),
                price == null ? null : price.getAmount().multiply(BigDecimal.valueOf(quantity)), price == null));
    }

    private String key(String menuId, PriceTarget target, String itemId) {
        return menuId + "|" + target + "|" + (itemId == null ? "" : itemId);
    }

    private Student manager(String registrationId) {
        Student actor = students.getByStudentId(registrationId);
        if (!managerAccess.hasCurrentAccess(actor.getId()))
            throw new InvalidBusinessStateException("Only a currently assigned Meal Manager may perform this action");
        return actor;
    }

    private YearMonth period(int year, int month) {
        if (year < 2000 || year > 9999 || month < 1 || month > 12) throw new InvalidRequestException("Invalid billing year or month");
        return YearMonth.of(year, month);
    }

    private BigDecimal money(BigDecimal value, boolean positive) {
        if (value == null || value.signum() < (positive ? 1 : 0) || value.scale() > 2 || value.precision() > 12)
            throw new InvalidRequestException("Amount must be a valid BDT amount with at most two decimal places");
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }
}
