package com.spark.campusday.student.service;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.student.model.AcademicYear;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.model.StudentGender;
import com.spark.campusday.student.repository.StudentRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class

StudentService {
    private final StudentRepository students;
    private final Validator validator;

    public Student getById(String id) {
        return students.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));
    }

    public Student getByStudentId(String studentId) {
        return students.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
    }

    public List<Student> listByAcademicYear(AcademicYear academicYear) {
        return students.findByAcademicYearOrderByNameAsc(academicYear);
    }

    public Optional<Student> findByEmail(String email) {
        return students.findByEmail(email.trim().toLowerCase(Locale.ROOT));
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    public boolean existsByPhoneNumber(String phoneNumber) {
        return students.findByPhoneNumber(phoneNumber).isPresent();
    }

    public Optional<Student> findByPhoneNumber(String phoneNumber) {
        return students.findByPhoneNumber(phoneNumber);
    }

    public Student save(Student student) {
        var violations = validator.validate(student);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        try {
            return students.save(student);
        } catch (DuplicateKeyException exception) {
            throw new ConflictException("Student ID, email or phone number already exists", exception);
        }
    }

    // Internal boundary for a future central student onboarding flow; no public registration API.
    public Student createStudent(String studentId, String name, String email, AcademicYear academicYear) {
        return createStudent(studentId, name, email, academicYear, null, null);
    }

    public Student createStudent(String studentId, String name, String email, AcademicYear academicYear,
                                 String phoneNumber, StudentGender gender) {
        Student student = new Student(studentId, name, email, academicYear, phoneNumber, gender);
        var violations = validator.validate(student);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return save(student);
    }

    public Student createAccountProfile(String name, String email, AcademicYear academicYear,
                                        String phoneNumber, StudentGender gender) {
        String generatedStudentId = "CD-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase(Locale.ROOT);
        return createStudent(generatedStudentId, name, email, academicYear, phoneNumber, gender);
    }

    public void rollbackFailedAccountProfile(String studentDocumentId) {
        students.deleteById(studentDocumentId);
    }
}
