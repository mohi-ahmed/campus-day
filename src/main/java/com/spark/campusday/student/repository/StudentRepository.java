package com.spark.campusday.student.repository;

import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.model.AcademicYear;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface StudentRepository extends MongoRepository<Student, String> {
    Optional<Student> findByStudentId(String studentId);

    Optional<Student> findByEmail(String email);

    Optional<Student> findByPhoneNumber(String phoneNumber);

    List<Student> findByAcademicYearOrderByNameAsc(AcademicYear academicYear);

    List<Student> findByAcademicYearIn(Collection<AcademicYear> academicYears);
}
