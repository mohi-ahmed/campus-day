package com.spark.campusday.student.service;

import com.spark.campusday.auth.repository.AuthAccountRepository;
import com.spark.campusday.auth.security.CampusUserPrincipal;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.student.model.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentStudentResolver {
    private final StudentService students;
    private final AuthAccountRepository accounts;

    public Student require(CampusUserPrincipal principal) {
        if (principal == null) {
            throw new InvalidBusinessStateException("Complete your student profile first");
        }
        String studentDocumentId = accounts.findById(principal.accountId())
                .map(account -> account.getStudentId())
                .orElse(principal.studentId());
        if (studentDocumentId == null || studentDocumentId.isBlank()) {
            throw new InvalidBusinessStateException("Complete your student profile first");
        }
        return students.getById(studentDocumentId);
    }
}
