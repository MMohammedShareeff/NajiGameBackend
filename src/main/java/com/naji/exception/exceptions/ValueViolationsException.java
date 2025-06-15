package com.naji.exception.exceptions;

import org.springframework.dao.DataIntegrityViolationException;

public class ValueViolationsException extends DataIntegrityViolationException {
    public ValueViolationsException(String message) {
        super(message);
    }
}
