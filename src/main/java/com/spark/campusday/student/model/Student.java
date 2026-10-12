package com.spark.campusday.student.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Document(collection = "students")
public class
Student {
    @Id
    private String id;

    @NotBlank
    @Indexed(unique = true)
    private String studentId;

    @NotBlank
    private String name;

    @NotBlank
    @Email
    @Indexed(unique = true, sparse = true)
    private String email;

    @NotNull
    private AcademicYear academicYear;

    @Indexed(unique = true, sparse = true)
    private String phoneNumber;

    private StudentGender gender;

    private String profileImageUrl;
    private String profileImagePublicId;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    protected Student() {
        // Spring Data MongoDB
    }

    public Student(String studentId, String name, String email, AcademicYear academicYear) {
        this(studentId, name, email, academicYear, null, null);
    }

    public Student(String studentId, String name, String email, AcademicYear academicYear,
                   String phoneNumber, StudentGender gender) {
        this.studentId = studentId;
        this.name = name;
        this.email = email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
        this.academicYear = academicYear;
        this.phoneNumber = phoneNumber == null ? null : phoneNumber.trim();
        this.gender = gender;
    }

    public void updateProfile(String name, AcademicYear academicYear,
                              String phoneNumber, StudentGender gender) {
        this.name = name;
        this.academicYear = academicYear;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
    }

    public void updateProfileImage(String imageUrl, String publicId) {
        this.profileImageUrl = imageUrl;
        this.profileImagePublicId = publicId;
    }

    public void clearProfileImage() {
        this.profileImageUrl = null;
        this.profileImagePublicId = null;
    }
}
