package com.spark.campusday.study.calendar.integration.service;

import com.spark.campusday.study.calendar.dto.CalendarEventResponse;
import com.spark.campusday.study.calendar.dto.CalendarEventType;
import com.spark.campusday.study.calendar.service.StudyCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CalendarExportEventService {
    private static final Set<CalendarEventType> EXPORTABLE_TYPES = EnumSet.of(
            CalendarEventType.TASK,
            CalendarEventType.ROUTINE,
            CalendarEventType.SYLLABUS_TOPIC);

    private final StudyCalendarService calendar;

    public List<CalendarEventResponse> events(
            String studentId, LocalDate from, LocalDate to) {
        return calendar.getRange(studentId, from, to).days().stream()
                .flatMap(day -> day.events().stream())
                .filter(event -> EXPORTABLE_TYPES.contains(event.type()))
                .toList();
    }

    public String eventKey(CalendarEventResponse event) {
        return event.type().name() + ":" + event.id() + ":" + event.date();
    }
}
