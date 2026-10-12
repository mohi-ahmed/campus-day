package com.spark.campusday.meal.price.service;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.config.MealReadCacheConfiguration;
import com.spark.campusday.meal.menu.dto.MenuResponse;
import com.spark.campusday.meal.menu.model.DailyMealMenu;
import com.spark.campusday.meal.menu.service.MenuService;
import com.spark.campusday.meal.price.dto.ApplicablePriceResponse;
import com.spark.campusday.meal.price.dto.PriceVersionResponse;
import com.spark.campusday.meal.price.dto.SetPriceRequest;
import com.spark.campusday.meal.price.model.MealPriceVersion;
import com.spark.campusday.meal.price.model.PriceTarget;
import com.spark.campusday.meal.price.repository.MealPriceVersionRepository;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MealPriceService {
    private final MealPriceVersionRepository prices;
    private final MenuService menus;
    private final StudentService students;
    private final MealManagerAccess managerAccess;
    @Qualifier("mealClock")
    private final Clock mealClock;

    /** managerStudentId is a registration ID until Security supplies the acting Student. */
    @CacheEvict(cacheNames = MealReadCacheConfiguration.APPLICABLE_MEAL_PRICES, key = "#p1.mealDate()")
    public PriceVersionResponse setPrice(String managerStudentId, SetPriceRequest request) {
        Student actor = students.getByStudentId(managerStudentId);
        if (!managerAccess.hasCurrentAccess(actor.getId())) {
            throw new InvalidBusinessStateException("Only a currently assigned Meal Manager may set prices");
        }
        if (request == null || request.mealDate() == null || request.target() == null) {
            throw new InvalidRequestException("Meal date and price target are required");
        }
        BigDecimal amount = validatedAmount(request.amount());
        MenuResponse menu = menus.getPublished(request.mealDate());
        String itemId = validatedItemId(menu, request.target(), request.itemId());
        Optional<MealPriceVersion> latest = prices.findLatest(menu.id(), request.target(), itemId);
        if (latest.isPresent() && latest.get().getAmount().compareTo(amount) == 0) {
            return response(latest.get());
        }
        Instant effectiveAt = Instant.now(mealClock).truncatedTo(ChronoUnit.MILLIS);
        if (latest.isPresent() && !effectiveAt.isAfter(latest.get().getEffectiveAt())) {
            throw new ConflictException("Price changed at this instant; retry the save");
        }
        MealPriceVersion version = new MealPriceVersion(menu.id(), menu.mealDate(), request.target(),
                itemId, amount, actor.getId(), effectiveAt);
        try {
            return response(prices.save(version));
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("A price version already exists for this target and instant", exception);
        }
    }

    @Cacheable(cacheNames = MealReadCacheConfiguration.APPLICABLE_MEAL_PRICES, key = "#p0")
    public ApplicablePriceResponse applicable(LocalDate mealDate) {
        MenuResponse menu = menus.getPublished(mealDate);
        Map<PriceKey, MealPriceVersion> latest = new HashMap<>();
        prices.findForMenuNewestFirst(menu.id()).forEach(version ->
                latest.putIfAbsent(new PriceKey(version.getTarget(), version.getItemId()), version));
        List<ApplicablePriceResponse.Entry> entries = new ArrayList<>();
        entries.add(entry(PriceTarget.BREAKFAST_PACKAGE, null, "Breakfast package", latest));
        addMainMeal(entries, PriceTarget.LUNCH_FULL_MEAL, PriceTarget.LUNCH_MENU_ITEM, menu.lunch(), latest);
        addMainMeal(entries, PriceTarget.DINNER_FULL_MEAL, PriceTarget.DINNER_MENU_ITEM, menu.dinner(), latest);
        return new ApplicablePriceResponse(menu.id(), menu.mealDate(), List.copyOf(entries));
    }

    public List<PriceVersionResponse> history(LocalDate mealDate, PriceTarget target, String itemId) {
        if (mealDate == null || target == null) {
            throw new InvalidRequestException("Meal date and price target are required");
        }
        MenuResponse menu = menus.getPublished(mealDate);
        String validatedId = validatedItemId(menu, target, itemId);
        return prices.findHistory(menu.id(), target, validatedId).stream().map(this::response).toList();
    }

    /** Future billing can request the revision applicable at a specific instant. */
    public Optional<PriceVersionResponse> applicableAsOf(LocalDate mealDate, PriceTarget target,
                                                          String itemId, Instant asOf) {
        if (asOf == null) throw new InvalidRequestException("As-of instant is required");
        MenuResponse menu = menus.getPublished(mealDate);
        String validatedId = validatedItemId(menu, target, itemId);
        return prices.findAsOf(menu.id(), target, validatedId, asOf).map(this::response);
    }

    private BigDecimal validatedAmount(BigDecimal amount) {
        if (amount == null || amount.signum() < 0 || amount.scale() > 2 || amount.precision() > 12) {
            throw new InvalidBusinessStateException("Price must be a nonnegative amount with at most two decimal places");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private String validatedItemId(MenuResponse menu, PriceTarget target, String itemId) {
        if (!target.isIndividualItem()) {
            if (itemId != null && !itemId.isBlank()) {
                throw new InvalidBusinessStateException("Package and full-meal prices cannot reference an item");
            }
            if (target == PriceTarget.LUNCH_FULL_MEAL && !menu.lunch().fullMealAvailable()
                    || target == PriceTarget.DINNER_FULL_MEAL && !menu.dinner().fullMealAvailable()) {
                throw new InvalidBusinessStateException("This full meal is not offered by the published menu");
            }
            return null;
        }
        if (itemId == null || itemId.isBlank()) {
            throw new InvalidBusinessStateException("A menu item ID is required for individual pricing");
        }
        List<DailyMealMenu.Item> items = target == PriceTarget.LUNCH_MENU_ITEM
                ? menu.lunch().items() : menu.dinner().items();
        String normalized = itemId.trim();
        if (items.stream().noneMatch(item -> item.id().equals(normalized))) {
            throw new InvalidBusinessStateException("Item does not belong to the selected menu meal");
        }
        return normalized;
    }

    private void addMainMeal(List<ApplicablePriceResponse.Entry> entries, PriceTarget fullTarget,
                             PriceTarget itemTarget, DailyMealMenu.MainMeal meal,
                             Map<PriceKey, MealPriceVersion> latest) {
        if (meal.fullMealAvailable()) {
            entries.add(entry(fullTarget, null, meal.fullMealDescription(), latest));
        }
        meal.items().forEach(item -> entries.add(entry(itemTarget, item.id(), item.name(), latest)));
    }

    private ApplicablePriceResponse.Entry entry(PriceTarget target, String itemId, String label,
                                                Map<PriceKey, MealPriceVersion> latest) {
        MealPriceVersion version = latest.get(new PriceKey(target, itemId));
        return new ApplicablePriceResponse.Entry(target, itemId, label,
                version == null ? null : version.getAmount(),
                version == null ? null : version.getEffectiveAt());
    }

    private PriceVersionResponse response(MealPriceVersion version) {
        return new PriceVersionResponse(version.getId(), version.getMenuId(), version.getMealDate(),
                version.getTarget(), version.getItemId(), version.getAmount(),
                version.getSetByStudentId(), version.getEffectiveAt());
    }

    private record PriceKey(PriceTarget target, String itemId) {}
}
