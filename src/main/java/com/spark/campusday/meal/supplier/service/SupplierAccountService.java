package com.spark.campusday.meal.supplier.service;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.meal.supplier.dto.SupplierDtos.AccountEntryResponse;
import com.spark.campusday.meal.supplier.dto.SupplierDtos.PaymentPage;
import com.spark.campusday.meal.supplier.model.SupplierAccountEntry;
import com.spark.campusday.meal.supplier.model.SupplierAccountEntryType;
import com.spark.campusday.meal.supplier.repository.SupplierAccountEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierAccountService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private final SupplierAccountEntryRepository entries;

    public BigDecimal due(String supplierId) {
        return entries.findFirstBySupplierIdOrderBySequenceDesc(supplierId)
                .map(SupplierAccountEntry::getBalanceAfter).orElse(ZERO);
    }

    public Map<String, BigDecimal> allDues() {
        return balances(entries.latestBalances());
    }

    public void creditPurchase(String supplierId, String purchaseId, BigDecimal amount,
                               LocalDate date, String managerDocumentId) {
        if (amount.signum() == 0) return;
        record(supplierId, SupplierAccountEntryType.PURCHASE_DUE, amount, date,
                managerDocumentId, null, "PURCHASE:" + purchaseId);
    }

    public AccountEntryResponse pay(String supplierId, String requestId, BigDecimal amount,
                                    LocalDate date, String note, String managerDocumentId) {
        if (requestId == null || requestId.isBlank() || requestId.length() > 100
                || amount == null || amount.signum() <= 0 || amount.scale() > 2
                || amount.precision() - amount.scale() > 10 || date == null
                || note != null && note.length() > 500) {
            throw new InvalidRequestException("Valid payment request, date and positive amount are required");
        }
        return response(record(supplierId, SupplierAccountEntryType.PAYMENT,
                amount.setScale(2), date, managerDocumentId, note, "PAYMENT:" + requestId));
    }

    public PaymentPage payments(String supplierId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new InvalidRequestException("Invalid payment page");
        Page<SupplierAccountEntry> result = entries.findBySupplierIdAndTypeOrderByBusinessDateDescSequenceDesc(
                supplierId, SupplierAccountEntryType.PAYMENT, PageRequest.of(page, size));
        return new PaymentPage(result.getContent().stream().map(this::response).toList(),
                page, size, result.getTotalElements(), result.getTotalPages());
    }

    private SupplierAccountEntry record(String supplierId, SupplierAccountEntryType type, BigDecimal amount,
                                        LocalDate date, String managerId, String note, String reference) {
        for (int attempt = 0; attempt < 5; attempt++) {
            var prior = entries.findBySourceReference(reference);
            if (prior.isPresent()) return matching(prior.get(), supplierId, type, amount);
            var latest = entries.findFirstBySupplierIdOrderBySequenceDesc(supplierId);
            BigDecimal before = latest.map(SupplierAccountEntry::getBalanceAfter).orElse(ZERO);
            if (type == SupplierAccountEntryType.PAYMENT && before.compareTo(amount) < 0) {
                throw new InvalidBusinessStateException("Payment exceeds current supplier due of " + before);
            }
            BigDecimal after = type == SupplierAccountEntryType.PAYMENT
                    ? before.subtract(amount) : before.add(amount);
            SupplierAccountEntry entry = new SupplierAccountEntry(supplierId,
                    latest.map(value -> value.getSequence() + 1).orElse(1L), type, amount,
                    after, date, managerId, note, reference);
            try {
                return entries.save(entry);
            } catch (DuplicateKeyException collision) {
                var existing = entries.findBySourceReference(reference);
                if (existing.isPresent()) return matching(existing.get(), supplierId, type, amount);
                if (attempt == 4) throw new ConflictException("Supplier account changed concurrently; retry", collision);
            }
        }
        throw new ConflictException("Supplier account changed concurrently; retry");
    }

    private SupplierAccountEntry matching(SupplierAccountEntry entry, String supplierId,
                                           SupplierAccountEntryType type, BigDecimal amount) {
        if (!entry.getSupplierId().equals(supplierId) || entry.getType() != type
                || entry.getAmount().compareTo(amount) != 0) {
            throw new ConflictException("Request reference conflicts with an existing account entry");
        }
        return entry;
    }

    private AccountEntryResponse response(SupplierAccountEntry entry) {
        return new AccountEntryResponse(entry.getId(), entry.getSupplierId(), entry.getType(),
                entry.getAmount(), entry.getBalanceAfter(), entry.getBusinessDate(),
                entry.getNote(), entry.getCreatedAt());
    }

    private Map<String, BigDecimal> balances(
            List<SupplierAccountEntryRepository.BalanceSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) return Map.of();
        return snapshots.stream()
                .filter(snapshot -> snapshot.id() != null && snapshot.latestBalance() != null)
                .collect(Collectors.toMap(SupplierAccountEntryRepository.BalanceSnapshot::id,
                        SupplierAccountEntryRepository.BalanceSnapshot::latestBalance,
                        (first, ignored) -> first));
    }
}
