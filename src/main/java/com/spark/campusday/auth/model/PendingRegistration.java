package com.spark.campusday.auth.model;

import com.spark.campusday.student.model.AcademicYear;
import com.spark.campusday.student.model.StudentGender;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Locale;

@Getter
@Document(collection = "auth_pending_registrations")
public class PendingRegistration {
    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;
    private String fullName;
    private AcademicYear academicYear;
    private String phoneNumber;
    private StudentGender gender;

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    protected PendingRegistration() {
        // Spring Data MongoDB
    }

    public PendingRegistration(String email, String passwordHash, String fullName,
                               AcademicYear academicYear, String phoneNumber,
                               StudentGender gender, Instant expiresAt) {
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.passwordHash = passwordHash;
        this.fullName = fullName.trim();
        this.academicYear = academicYear;
        this.phoneNumber = phoneNumber.trim();
        this.gender = gender;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
