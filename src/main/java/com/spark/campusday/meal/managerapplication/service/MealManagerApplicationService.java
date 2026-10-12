package com.spark.campusday.meal.managerapplication.service;

import com.spark.campusday.auth.security.CampusUserPrincipal;
import com.spark.campusday.auth.service.SecretTokenService;
import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.meal.managementperiod.model.ManagementPeriodStatus;
import com.spark.campusday.meal.managementperiod.model.MealManagementPeriod;
import com.spark.campusday.meal.managementperiod.repository.MealManagementPeriodRepository;
import com.spark.campusday.meal.managerapplication.config.MealManagerApplicationProperties;
import com.spark.campusday.meal.managerapplication.dto.MealManagerApplicationOverview;
import com.spark.campusday.meal.managerapplication.dto.MealManagerApplicationResponse;
import com.spark.campusday.meal.managerapplication.mail.MealManagerApplicationEmailService;
import com.spark.campusday.meal.managerapplication.model.MealManagerApplication;
import com.spark.campusday.meal.managerapplication.model.MealManagerApplicationStatus;
import com.spark.campusday.meal.managerapplication.model.MealManagerDecision;
import com.spark.campusday.meal.managerapplication.repository.MealManagerApplicationRepository;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.CurrentStudentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static com.spark.campusday.meal.managerapplication.model.MealManagerApplication.ROLE_ACCESS_SCOPE;

@Service
@RequiredArgsConstructor
public class MealManagerApplicationService {
    private final MealManagerApplicationRepository applications;
    private final MealManagementPeriodRepository periods;
    private final CurrentStudentResolver currentStudent;
    private final SecretTokenService tokens;
    private final MealManagerApplicationEmailService emailService;
    private final MealManagerApplicationProperties properties;
    @Qualifier("mealClock")
    private final Clock mealClock;

    public MealManagerApplicationOverview currentOverview(CampusUserPrincipal principal) {
        Student student = currentStudent.require(principal);
        Optional<MealManagementPeriod> period = currentPeriod();
        boolean assigned = period
                .map(current -> current.getManagerStudentIds().contains(student.getId()))
                .orElse(false);
        MealManagerApplication application = applications
                .findByStudentDocumentIdAndManagementPeriodId(student.getId(), ROLE_ACCESS_SCOPE)
                .map(this::expireIfDue)
                .orElse(null);
        boolean approved = application != null
                && application.getStatus() == MealManagerApplicationStatus.APPROVED;
        return new MealManagerApplicationOverview(
                student.getAcademicYear().isFourthYear(),
                assigned || approved,
                period.map(MealManagementPeriod::getId).orElse(null),
                period.map(MealManagementPeriod::getStartDate).orElse(null),
                period.map(MealManagementPeriod::getEndDate).orElse(null),
                application == null ? null : MealManagerApplicationResponse.from(application));
    }

    public MealManagerApplicationResponse apply(CampusUserPrincipal principal) {
        Student student = currentStudent.require(principal);
        if (!student.getAcademicYear().isFourthYear()) {
            throw new InvalidBusinessStateException("Only Fourth Year students may apply to become a Meal Manager");
        }
        if (currentPeriod().map(period -> period.getManagerStudentIds().contains(student.getId())).orElse(false)) {
            throw new ConflictException("You already have Meal Manager access for the current period");
        }
        var existing = applications.findByStudentDocumentIdAndManagementPeriodId(
                student.getId(), ROLE_ACCESS_SCOPE);
        if (existing.isPresent()) {
            return MealManagerApplicationResponse.from(expireIfDue(existing.get()));
        }
        if (student.getEmail() == null || student.getEmail().isBlank()) {
            throw new InvalidBusinessStateException("A verified student email is required before applying");
        }

        Instant now = mealClock.instant();
        if (properties.getDecisionTtl() == null
                || properties.getDecisionTtl().isZero()
                || properties.getDecisionTtl().isNegative()) {
            throw new InvalidBusinessStateException("Meal Manager decision TTL must be positive");
        }
        String rawToken = tokens.newOpaqueToken();
        MealManagerApplication application = new MealManagerApplication(
                student.getId(),
                student.getStudentId(),
                student.getName(),
                student.getEmail(),
                ROLE_ACCESS_SCOPE,
                null,
                null,
                tokens.hashToken(rawToken),
                now.plus(properties.getDecisionTtl()));
        try {
            application = applications.save(application);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("A Meal Manager application already exists for this student", exception);
        }
        try {
            emailService.sendApplicationSubmitted(application, rawToken);
        } catch (RuntimeException exception) {
            applications.deleteById(application.getId());
            throw exception;
        }
        return MealManagerApplicationResponse.from(application);
    }

    public MealManagerApplicationResponse review(String rawToken) {
        return MealManagerApplicationResponse.from(findByToken(rawToken));
    }

    public MealManagerApplicationResponse decide(String rawToken, MealManagerDecision decision) {
        if (decision == null) {
            throw new InvalidRequestException("An approval decision is required");
        }
        MealManagerApplication application = findByToken(rawToken);
        if (application.getStatus() == MealManagerApplicationStatus.EXPIRED) {
            throw new InvalidBusinessStateException("This application expired after 72 hours and was automatically rejected");
        }
        Instant now = mealClock.instant();
        String decisionActor = "EMAIL_LINK:" + properties.getAdminEmail();
        if (decision == MealManagerDecision.APPROVE) {
            application.approve(now, decisionActor);
        } else {
            application.reject(now, decisionActor);
        }
        try {
            return MealManagerApplicationResponse.from(applications.save(application));
        } catch (OptimisticLockingFailureException exception) {
            throw new ConflictException("This application was decided in another session", exception);
        }
    }

    public boolean hasApprovedAccess(String studentDocumentId) {
        return applications.existsByStudentDocumentIdAndManagementPeriodIdAndStatus(
                studentDocumentId, ROLE_ACCESS_SCOPE, MealManagerApplicationStatus.APPROVED);
    }

    public int expirePendingApplications() {
        Instant now = mealClock.instant();
        var due = applications.findAllByStatusAndExpiresAtLessThanEqual(
                MealManagerApplicationStatus.PENDING, now);
        int expired = 0;
        for (MealManagerApplication application : due) {
            if (!application.expireIfDue(now)) {
                continue;
            }
            try {
                applications.save(application);
                expired++;
            } catch (OptimisticLockingFailureException ignored) {
                // An admin decision won the race; its final state must remain authoritative.
            }
        }
        return expired;
    }

    private MealManagerApplication findByToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRequestException("The decision token is required");
        }
        return applications.findByTokenHash(tokens.hashToken(rawToken.trim()))
                .map(this::expireIfDue)
                .orElseThrow(() -> new ResourceNotFoundException("Meal Manager application not found"));
    }

    private MealManagerApplication expireIfDue(MealManagerApplication application) {
        if (application.expireIfDue(mealClock.instant())) {
            return applications.save(application);
        }
        return application;
    }

    private Optional<MealManagementPeriod> currentPeriod() {
        LocalDate today = LocalDate.now(mealClock);
        return periods.findFirstByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                ManagementPeriodStatus.ACTIVE, today, today);
    }
}
