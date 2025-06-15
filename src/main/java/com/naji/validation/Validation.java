package com.naji.validation;

import org.springframework.validation.BindingResult;

import java.util.HashMap;
import java.util.Map;

public class Validation {
    public static Map<String, String> getValidationErrors(BindingResult result) {
        Map<String, String> errors = new HashMap<>();
        result.getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }
}
