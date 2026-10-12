package com.spark.campusday.meal.absence.service;

import com.spark.campusday.common.exception.ConflictException;
import com.spark.campusday.common.exception.InvalidBusinessStateException;
import com.spark.campusday.common.exception.InvalidRequestException;
import com.spark.campusday.common.exception.ResourceNotFoundException;
import com.spark.campusday.meal.absence.dto.AbsenceNoticeRequest;
import com.spark.campusday.meal.absence.dto.AbsenceNoticeResponse;
import com.spark.campusday.meal.absence.model.MealAbsenceNotice;
import com.spark.campusday.meal.absence.repository.MealAbsenceNoticeRepository;
import com.spark.campusday.student.model.Student;
import com.spark.campusday.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AbsenceNoticeService {
    private final MealAbsenceNoticeRepository notices;
    private final StudentService students;

    /** studentId is the Student registration ID until authenticated identity is available. */
    public AbsenceNoticeResponse create(String studentId, AbsenceNoticeRequest request) {
        Student student = requireFixedChargeStudent(studentId);
        if (request == null) throw new InvalidRequestException("Absence notice is required");
        MealAbsenceNotice notice = new MealAbsenceNotice(student.getId(), request.startDate(),
                request.endDate(), request.meals(), request.note());
        if (notices.existsOverlapping(student.getId(), notice.getStartDate(),
                notice.getEndDate(), notice.getMeals())) {
            throw new ConflictException("An absence notice already covers one of these meals and dates");
        }
        return response(notices.save(notice));
    }

    public List<AbsenceNoticeResponse> list(String studentId, LocalDate from, LocalDate to) {
        Student student = requireFixedChargeStudent(studentId);
        if (from == null || to == null || from.isAfter(to)) {
            throw new InvalidRequestException("A valid from/to date range is required");
        }
        return notices.findInRange(student.getId(), from, to).stream().map(this::response).toList();
    }

    public AbsenceNoticeResponse getById(String studentId, String id) {
        Student student = requireFixedChargeStudent(studentId);
        MealAbsenceNotice notice = notices.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Absence notice not found: " + id));
        if (!notice.getStudentId().equals(student.getId())) {
            throw new ResourceNotFoundException("Absence notice not found: " + id);
        }
        return response(notice);
    }

    private Student requireFixedChargeStudent(String studentId) {
        Student student = students.getByStudentId(studentId);
        if (!student.getAcademicYear().isFixedChargeYear()) {
            throw new InvalidBusinessStateException("Absence notices are only for Years 1–4 students");
        }
        return student;
    }

    private AbsenceNoticeResponse response(MealAbsenceNotice notice) {
        return new AbsenceNoticeResponse(notice.getId(), notice.getStartDate(), notice.getEndDate(),
                notice.getMeals(), notice.getNote(), notice.getCreatedAt(), notice.getUpdatedAt());
    }
}
