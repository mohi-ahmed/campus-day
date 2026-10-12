package com.spark.campusday.study.syllabus.exception;

import com.spark.campusday.common.exception.InvalidBusinessStateException;

public class InvalidSyllabusStateException extends InvalidBusinessStateException {
    public InvalidSyllabusStateException(String message) {
        super(message);
    }
}
