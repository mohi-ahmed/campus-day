package com.spark.campusday.meal.supplier.service;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.meal.access.MealManagerAccess;
import com.spark.campusday.meal.supplier.dto.SupplierDtos.*;
import com.spark.campusday.meal.supplier.model.Supplier;
import com.spark.campusday.meal.supplier.repository.SupplierRepository;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SupplierService {
    private final SupplierRepository suppliers;
    private final SupplierAccountService accounts;
    private final StudentService students;
    private final MealManagerAccess managerAccess;

    public SupplierResponse create(String managerStudentId, SupplierRequest request) {
        requireManager(managerStudentId);
        validate(request);
        Supplier supplier = new Supplier(request.name(), request.contact(), request.note());
        if (suppliers.existsByNameKey(supplier.getNameKey())) throw new ConflictException("Supplier already exists");
        try {
            return response(suppliers.save(supplier), BigDecimal.ZERO.setScale(2));
        } catch (DuplicateKeyException duplicate) {
            throw new ConflictException("Supplier already exists", duplicate);
        }
    }

    public SupplierResponse update(String managerStudentId, String id, SupplierRequest request) {
        requireManager(managerStudentId);
        validate(request);
        Supplier supplier = requireSupplier(id);
        String key = request.name().trim().toLowerCase(Locale.ROOT);
        if (!key.equals(supplier.getNameKey()) && suppliers.existsByNameKeyAndIdNot(key, id)) {
            throw new ConflictException("Supplier already exists");
        }
        supplier.revise(request.name(), request.contact(), request.note());
        try {
            return response(suppliers.save(supplier), accounts.due(id));
        } catch (DuplicateKeyException duplicate) {
            throw new ConflictException("Supplier already exists", duplicate);
        }
    }

    public SupplierPage list(int page, int size) {
        validatePage(page, size);
        Page<Supplier> result = suppliers.findAll(PageRequest.of(page, size, Sort.by("name")));
        return new SupplierPage(result.getContent().stream()
                .map(supplier -> response(supplier, accounts.due(supplier.getId())))
                .toList(), page, size, result.getTotalElements(), result.getTotalPages());
    }

    public SupplierResponse get(String id) {
        return response(requireSupplier(id), accounts.due(id));
    }

    public DueResponse due(String id) {
        requireSupplier(id);
        return new DueResponse(id, accounts.due(id));
    }

    public PaymentPage payments(String id, int page, int size) {
        requireSupplier(id);
        return accounts.payments(id, page, size);
    }

    public AccountEntryResponse pay(String managerStudentId, String supplierId, PaymentRequest request) {
        String managerId = requireManager(managerStudentId);
        requireSupplier(supplierId);
        if (request == null) throw new InvalidRequestException("Supplier payment is required");
        return accounts.pay(supplierId, request.requestId(), request.amount(), request.paymentDate(),
                request.note(), managerId);
    }

    public Supplier requireSupplier(String id) {
        return suppliers.findById(id).orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));
    }

    private String requireManager(String registrationId) {
        Student student = students.getByStudentId(registrationId);
        if (!managerAccess.hasCurrentAccess(student.getId())) {
            throw new InvalidBusinessStateException("Only a currently assigned Meal Manager may change supplier records");
        }
        return student.getId();
    }

    private void validate(SupplierRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()
                || request.name().length() > 100 || request.contact() != null && request.contact().length() > 50
                || request.note() != null && request.note().length() > 500) {
            throw new InvalidRequestException("Valid supplier name and contact details are required");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new InvalidRequestException("Invalid supplier page");
    }

    private SupplierResponse response(Supplier supplier, BigDecimal due) {
        return new SupplierResponse(supplier.getId(), supplier.getName(), supplier.getContact(),
                supplier.getNote(), due, supplier.getCreatedAt(), supplier.getUpdatedAt());
    }
}
