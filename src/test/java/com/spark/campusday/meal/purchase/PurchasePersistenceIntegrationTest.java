package com.spark.campusday.meal.purchase;

import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.inventory.model.Ingredient;
import com.spark.campusday.meal.inventory.model.IngredientUnit;
import com.spark.campusday.meal.inventory.model.StockMovementType;
import com.spark.campusday.meal.inventory.repository.IngredientRepository;
import com.spark.campusday.meal.inventory.repository.StockMovementRepository;
import com.spark.campusday.meal.inventory.service.InventoryService;
import com.spark.campusday.meal.purchase.dto.PurchaseDtos.CreateRequest;
import com.spark.campusday.meal.purchase.dto.PurchaseDtos.LineRequest;
import com.spark.campusday.meal.purchase.model.Purchase;
import com.spark.campusday.meal.purchase.model.PurchaseLine;
import com.spark.campusday.meal.purchase.model.PurchasePaymentMode;
import com.spark.campusday.meal.purchase.model.PurchaseStatus;
import com.spark.campusday.meal.purchase.repository.PurchaseRepository;
import com.spark.campusday.meal.purchase.service.PurchaseService;
import com.spark.campusday.meal.supplier.dto.SupplierDtos.PaymentRequest;
import com.spark.campusday.meal.supplier.dto.SupplierDtos.SupplierRequest;
import com.spark.campusday.meal.supplier.repository.SupplierAccountEntryRepository;
import com.spark.campusday.meal.supplier.repository.SupplierRepository;
import com.spark.campusday.meal.supplier.service.SupplierAccountService;
import com.spark.campusday.meal.supplier.service.SupplierService;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "CAMPUS_DAY_MONGO_TEST_URI", matches = ".+")
@SpringBootTest(properties = {"spring.mongodb.database=CampusDayPart10Test", "spring.data.mongodb.auto-index-creation=true"})
class PurchasePersistenceIntegrationTest {
    @DynamicPropertySource
    static void mongo(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> System.getenv("CAMPUS_DAY_MONGO_TEST_URI"));
    }

    @Autowired IngredientRepository ingredients;
    @Autowired StockMovementRepository movements;
    @Autowired SupplierRepository suppliers;
    @Autowired SupplierAccountEntryRepository accountEntries;
    @Autowired PurchaseRepository purchases;
    @Autowired MongoTemplate mongo;

    @Test
    void cashCreditStockDuesPaymentsAndRetries() {
        String actor = "MANAGER-" + UUID.randomUUID();
        StudentService students = mock(StudentService.class);
        Student manager = mock(Student.class);
        MealManagerAccess access = mock(MealManagerAccess.class);
        when(students.getByStudentId(actor)).thenReturn(manager);
        when(manager.getId()).thenReturn("manager-db");
        when(access.hasCurrentAccess("manager-db")).thenReturn(true);
        InventoryService inventory = new InventoryService(ingredients, movements, students, access,
                Clock.system(ZoneId.of("Asia/Dhaka")));
        SupplierAccountService accounts = new SupplierAccountService(accountEntries);
        SupplierService supplierService = new SupplierService(suppliers, accounts, students, access);
        PurchaseService service = new PurchaseService(purchases, ingredients, suppliers, supplierService,
                accounts, inventory, students, access);
        Ingredient ingredient = ingredients.save(new Ingredient("Purchase test " + UUID.randomUUID(), IngredientUnit.KG));
        String supplierId = supplierService.create(actor, new SupplierRequest("Vendor " + UUID.randomUUID(), null, null)).id();
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Dhaka"));
        try {
            CreateRequest cash = request(UUID.randomUUID().toString(), supplierId, ingredient.getId(),
                    date, IngredientUnit.KG, PurchasePaymentMode.CASH, null);
            var cashResult = service.create(actor, cash);
            assertEquals(PurchaseStatus.COMPLETED, cashResult.status());
            assertEquals(new BigDecimal("15.00"), cashResult.total());
            assertTrue(mongo.getCollection("meal_purchases").find(new org.bson.Document("_id", new org.bson.types.ObjectId(cashResult.id())))
                    .first().get("total") instanceof org.bson.types.Decimal128);
            assertEquals(new BigDecimal("15.00"), cashResult.initialPaid());
            assertEquals(0, accounts.due(supplierId).signum());
            assertEquals(cashResult.id(), service.create(actor, cash).id());
            assertEquals(new BigDecimal("2.000"), inventory.getIngredient(ingredient.getId()).currentStock());

            CreateRequest credit = request(UUID.randomUUID().toString(), supplierId, ingredient.getId(),
                    date, IngredientUnit.KG, PurchasePaymentMode.CREDIT, new BigDecimal("5"));
            var creditResult = service.create(actor, credit);
            assertEquals(new BigDecimal("10.00"), accounts.due(supplierId));
            assertEquals(creditResult.id(), service.create(actor, credit).id());
            assertEquals(new BigDecimal("10.00"), accounts.due(supplierId));
            assertEquals(new BigDecimal("4.000"), inventory.getIngredient(ingredient.getId()).currentStock());
            assertEquals(2, movements.findByIngredientIdOrderBySequenceDesc(ingredient.getId(),
                    org.springframework.data.domain.PageRequest.of(0, 20)).getTotalElements());
            assertTrue(movements.findBySourceReference("PURCHASE:" + creditResult.id() + ":0")
                    .filter(movement -> movement.getType() == StockMovementType.PURCHASE_IN).isPresent());

            PaymentRequest payment = new PaymentRequest(UUID.randomUUID().toString(), new BigDecimal("4"), date, null);
            var paid = supplierService.pay(actor, supplierId, payment);
            assertEquals(new BigDecimal("6.00"), accounts.due(supplierId));
            assertEquals(paid.id(), supplierService.pay(actor, supplierId, payment).id());
            assertEquals(1, supplierService.payments(supplierId, 0, 20).totalElements());
            assertThrows(InvalidBusinessStateException.class, () -> supplierService.pay(actor, supplierId,
                    new PaymentRequest(UUID.randomUUID().toString(), new BigDecimal("7"), date, null)));
            assertEquals(new BigDecimal("6.00"), accounts.due(supplierId));

            CreateRequest badUnit = request(UUID.randomUUID().toString(), supplierId, ingredient.getId(),
                    date, IngredientUnit.LITER, PurchasePaymentMode.CASH, null);
            assertThrows(InvalidBusinessStateException.class, () -> service.create(actor, badUnit));
            assertTrue(purchases.findByRequestId(badUnit.requestId()).isEmpty());
            assertEquals(new BigDecimal("4.000"), inventory.getIngredient(ingredient.getId()).currentStock());

            Purchase interrupted = purchases.save(new Purchase(UUID.randomUUID().toString(), supplierId,
                    date, List.of(new PurchaseLine(ingredient.getId(), ingredient.getName(), IngredientUnit.KG,
                    new BigDecimal("2.000"), new BigDecimal("7.50"), new BigDecimal("15.00"))),
                    PurchasePaymentMode.CREDIT, new BigDecimal("15.00"), BigDecimal.ZERO.setScale(2),
                    null, "manager-db"));
            inventory.creditPurchaseLine(ingredient, interrupted.getId(), 0, new BigDecimal("2.000"), date, "manager-db");
            assertEquals(PurchaseStatus.COMPLETED, service.resume(actor, interrupted.getId()).status());
            assertEquals(PurchaseStatus.COMPLETED, service.resume(actor, interrupted.getId()).status());
            assertEquals(new BigDecimal("6.000"), inventory.getIngredient(ingredient.getId()).currentStock());
            assertEquals(new BigDecimal("21.00"), accounts.due(supplierId));

            when(access.hasCurrentAccess("manager-db")).thenReturn(false);
            assertThrows(InvalidBusinessStateException.class, () -> service.create(actor,
                    request(UUID.randomUUID().toString(), supplierId, ingredient.getId(), date,
                            IngredientUnit.KG, PurchasePaymentMode.CASH, null)));
            assertThrows(InvalidBusinessStateException.class, () -> supplierService.pay(actor, supplierId,
                    new PaymentRequest(UUID.randomUUID().toString(), BigDecimal.ONE, date, null)));
        } finally {
            purchases.deleteAll(purchases.search(supplierId, null, null, null,
                    org.springframework.data.domain.PageRequest.of(0, 100)).getContent());
            accountEntries.deleteAll(accountEntries.findAll().stream()
                    .filter(entry -> supplierId.equals(entry.getSupplierId())).toList());
            movements.deleteAll(movements.findByIngredientIdOrderBySequenceDesc(ingredient.getId(),
                    org.springframework.data.domain.PageRequest.of(0, 100)).getContent());
            suppliers.deleteById(supplierId);
            ingredients.deleteById(ingredient.getId());
        }
    }

    private CreateRequest request(String requestId, String supplierId, String ingredientId, LocalDate date,
                                  IngredientUnit unit, PurchasePaymentMode mode, BigDecimal paid) {
        return new CreateRequest(requestId, supplierId, date,
                List.of(new LineRequest(ingredientId, unit, new BigDecimal("2"), new BigDecimal("7.50"))),
                mode, paid, null);
    }
}
