package com.spark.campusday.meal.menu.service;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.meal.config.MealReadCacheConfiguration;
import com.spark.campusday.meal.menu.dto.MenuRequest;
import com.spark.campusday.meal.menu.dto.MenuResponse;
import com.spark.campusday.meal.menu.model.DailyMealMenu;
import com.spark.campusday.meal.menu.model.MenuStatus;
import com.spark.campusday.meal.menu.repository.DailyMealMenuRepository;
import com.spark.campusday.meal.order.model.OrderStatus;
import com.spark.campusday.meal.order.repository.FinalYearMealOrderRepository;
import com.spark.campusday.meal.price.repository.MealPriceVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuService {
    private final DailyMealMenuRepository menus;
    private final FinalYearMealOrderRepository orders;
    private final MealPriceVersionRepository prices;

    public MenuResponse create(MenuRequest request) {
        if (menus.existsByMealDate(request.mealDate())) {
            throw new ConflictException("A menu already exists for this date");
        }
        DailyMealMenu menu = new DailyMealMenu(request.mealDate(), breakfast(request.breakfast()),
                mainMeal(request.lunch()), mainMeal(request.dinner()));
        return response(save(menu));
    }

    @CacheEvict(cacheNames = MealReadCacheConfiguration.PUBLISHED_MENUS, allEntries = true)
    public MenuResponse update(String id, MenuRequest request) {
        DailyMealMenu menu = requireMenu(id);
        if (menu.getStatus() == MenuStatus.PUBLISHED && !menu.getMealDate().equals(request.mealDate())) {
            throw new InvalidBusinessStateException("The date of a published menu cannot be changed");
        }
        if (menu.getStatus() == MenuStatus.PUBLISHED
                && orders.existsByMenuIdAndStatus(id, OrderStatus.SUBMITTED)) {
            throw new InvalidBusinessStateException("A published menu with submitted orders cannot be edited");
        }
        if (!menu.getMealDate().equals(request.mealDate())
                && menus.existsByMealDateAndIdNot(request.mealDate(), id)) {
            throw new ConflictException("A menu already exists for this date");
        }
        menu.revise(request.mealDate(), breakfast(request.breakfast()),
                mainMeal(request.lunch()), mainMeal(request.dinner()));
        return response(save(menu));
    }

    @CacheEvict(cacheNames = MealReadCacheConfiguration.PUBLISHED_MENUS, allEntries = true)
    public void delete(String id) {
        DailyMealMenu menu = requireMenu(id);
        if (orders.existsByMenuId(id)) {
            throw new InvalidBusinessStateException("A menu with order history cannot be deleted");
        }
        prices.deleteAllByMenuId(id);
        menus.delete(menu);
    }

    @CacheEvict(cacheNames = MealReadCacheConfiguration.PUBLISHED_MENUS, key = "#result.mealDate()")
    public MenuResponse publish(String id) {
        DailyMealMenu menu = requireMenu(id);
        menu.publish();
        return response(menus.save(menu));
    }

    @Cacheable(cacheNames = MealReadCacheConfiguration.PUBLISHED_MENUS, key = "#p0")
    public MenuResponse getPublished(LocalDate date) {
        return response(menus.findByMealDateAndStatus(date, MenuStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Published menu not found for " + date)));
    }

    public MenuResponse getManaged(String id) {
        return response(requireMenu(id));
    }

    public List<MenuResponse> publishedBetween(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return menus.findPublishedInRange(MenuStatus.PUBLISHED, from, to)
                .stream().map(this::response).toList();
    }

    public List<MenuResponse> managedBetween(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return menus.findManagedInRange(from, to)
                .stream().map(this::response).toList();
    }

    private DailyMealMenu requireMenu(String id) {
        return menus.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu not found: " + id));
    }

    private DailyMealMenu.Breakfast breakfast(MenuRequest.BreakfastRequest request) {
        if (request == null || request.packageDescription() == null || request.packageDescription().isBlank()) {
            throw new InvalidBusinessStateException("Breakfast package description is required");
        }
        if (!request.available() && request.orderingEnabled()) {
            throw new InvalidBusinessStateException("Ordering cannot be enabled for unavailable breakfast");
        }
        return new DailyMealMenu.Breakfast(request.packageDescription().trim(),
                request.available(), request.orderingEnabled());
    }

    private DailyMealMenu.MainMeal mainMeal(MenuRequest.MainMealRequest request) {
        if (request == null || request.description() == null || request.description().isBlank()) {
            throw new InvalidBusinessStateException("Lunch and dinner descriptions are required");
        }
        if (!request.available() && request.orderingEnabled()) {
            throw new InvalidBusinessStateException("Ordering cannot be enabled for an unavailable meal");
        }
        if (request.fullMealAvailable() && (request.fullMealDescription() == null
                || request.fullMealDescription().isBlank())) {
            throw new InvalidBusinessStateException("Full meal description is required when available");
        }
        List<MenuRequest.ItemRequest> requested = request.items() == null ? List.of() : request.items();
        Set<String> names = new HashSet<>();
        Set<String> ids = new HashSet<>();
        List<DailyMealMenu.Item> items = requested.stream().map(item -> {
            if (item == null || item.name() == null || item.name().isBlank()) {
                throw new InvalidBusinessStateException("Menu item name is required");
            }
            String name = item.name().trim();
            String id = item.id() == null || item.id().isBlank() ? UUID.randomUUID().toString() : item.id().trim();
            if (!names.add(name.toLowerCase(Locale.ROOT)) || !ids.add(id)) {
                throw new InvalidBusinessStateException("Menu items must have distinct names and IDs within a meal");
            }
            if (!request.available() && item.available()) {
                throw new InvalidBusinessStateException("Items cannot be available in an unavailable meal");
            }
            return new DailyMealMenu.Item(id, name, item.nameBn(), item.quantity(), item.available());
        }).toList();
        if (!request.available() && request.fullMealAvailable()) {
            throw new InvalidBusinessStateException("Full meal cannot be available in an unavailable meal");
        }
        if (request.orderingEnabled() && !request.fullMealAvailable()
                && items.stream().noneMatch(DailyMealMenu.Item::available)) {
            throw new InvalidBusinessStateException("An orderable meal needs a full meal or an available item");
        }
        return new DailyMealMenu.MainMeal(request.description().trim(), request.available(),
                request.orderingEnabled(), request.fullMealAvailable(),
                request.fullMealDescription() == null ? null : request.fullMealDescription().trim(), items);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > 90) {
            throw new InvalidRequestException("A date range of at most 91 days is required");
        }
    }

    private DailyMealMenu save(DailyMealMenu menu) {
        try {
            return menus.save(menu);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("A menu already exists for this date", exception);
        }
    }

    private MenuResponse response(DailyMealMenu menu) {
        return new MenuResponse(menu.getId(), menu.getMealDate(), menu.getStatus(), menu.getBreakfast(),
                menu.getLunch(), menu.getDinner(), menu.getCreatedAt(), menu.getUpdatedAt());
    }
}
