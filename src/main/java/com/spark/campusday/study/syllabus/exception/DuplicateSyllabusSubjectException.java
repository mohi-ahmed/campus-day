package com.spark.campusday.study.syllabus.exception;

import com.spark.campusday.common.exception.ConflictException;

public class DuplicateSyllabusSubjectException extends ConflictException {
    public DuplicateSyllabusSubjectException(String name) {
        super("A syllabus subject already exists with the name: " + name);
    }

    public DuplicateSyllabusSubjectException(String name, Throwable cause) {
        super("A syllabus subject already exists with the name: " + name, cause);
    }
}
