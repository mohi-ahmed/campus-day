package com.spark.campusday.study.syllabus.exception;

import com.spark.campusday.common.exception.ConflictException;

public class DuplicateSyllabusTopicException extends ConflictException {
    public DuplicateSyllabusTopicException(String title) {
        super("A syllabus topic already exists with the title: " + title);
    }

    public DuplicateSyllabusTopicException(String title, Throwable cause) {
        super("A syllabus topic already exists with the title: " + title, cause);
    }
}
