package com.spark.campusday.meal.billing.controller;

import com.spark.campusday.meal.billing.model.FixedChargeVersion;
import com.spark.campusday.meal.billing.model.MonthlyPayment;
import com.spark.campusday.meal.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Development API. College Office identity and authorization arrive with Security. */
@RestController
@RequestMapping("/api/meal/billing")
@RequiredArgsConstructor
public class BillingController {
    private final BillingService service;
    public record ChargeRequest(BigDecimal amount) {}
    public record PaymentRequest(String studentId, int year, int month, BigDecimal amount,
                                 LocalDate paymentDate, String reference) {}

    @PutMapping("/fixed-charge")
    public FixedChargeVersion setCharge(@RequestParam String managerStudentId, @RequestParam int year,
                                        @RequestParam int month, @RequestBody ChargeRequest request) {
        return service.setCharge(managerStudentId, year, month, request == null ? null : request.amount());
    }

    @GetMapping("/fixed-charge/history")
    public List<FixedChargeVersion> chargeHistory(@RequestParam int year, @RequestParam int month) {
        return service.chargeHistory(year, month);
    }

    @GetMapping("/bills/{studentId}")
    public BillingService.Bill bill(@PathVariable String studentId, @RequestParam int year, @RequestParam int month) {
        return service.bill(studentId, year, month);
    }

    @PostMapping("/bills/{studentId}/finalize")
    public BillingService.Bill finalizeBill(@PathVariable String studentId, @RequestParam String managerStudentId,
                                             @RequestParam int year, @RequestParam int month) {
        return service.finalizeBill(managerStudentId, studentId, year, month);
    }

    @PostMapping("/office-payments")
    public MonthlyPayment officePayment(@RequestBody PaymentRequest request) {
        return service.recordOfficePayment(request.studentId(), request.year(), request.month(),
                request.amount(), request.paymentDate(), request.reference());
    }

    @GetMapping("/office-payments")
    public BillingService.OfficeSummary officeSummary(@RequestParam int year, @RequestParam int month) {
        return service.officeSummary(year, month);
    }

    @PostMapping("/final-year-collections")
    public MonthlyPayment finalYearCollection(@RequestParam String managerStudentId, @RequestBody PaymentRequest request) {
        return service.recordFinalYearCollection(managerStudentId, request.studentId(), request.year(), request.month(),
                request.amount(), request.paymentDate(), request.reference());
    }

    @GetMapping("/final-year-collections/summary")
    public List<BillingService.CollectionRow> finalYearCollections(@RequestParam String managerStudentId,
                                                                   @RequestParam int year,
                                                                   @RequestParam int month) {
        return service.finalYearCollections(managerStudentId, year, month);
    }
}
