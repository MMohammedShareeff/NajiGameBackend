package com.naji.exception;

import com.naji.exception.exceptions.*;
import com.naji.response.ApiResponse;
import com.naji.validation.Validation;
import org.antlr.v4.runtime.Token;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ApiResponse<String> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return new ApiResponse<>(ex.getLocalizedMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InsufficientPlayersException.class)
    public ApiResponse<String> handleInsufficientPlayersException(InsufficientPlayersException ex) {
        return new ApiResponse<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ApiResponse<String> handleDataIntegrityViolationException(DataIntegrityViolationException ex){
        return new ApiResponse<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ApiResponse<String> handleUnauthorizedAccessException(UnauthorizedAccessException ex) {
        return new ApiResponse<>(ex.getLocalizedMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(RoomNotActiveException.class)
    public ApiResponse<String> handleRoomNotActiveException(RoomNotActiveException ex) {
        return new ApiResponse<>(ex.getLocalizedMessage(), HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = Validation.getValidationErrors(ex.getBindingResult());
        return new ApiResponse<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ValueViolationsException.class)
    public ApiResponse<String> handleDataIntegrityViolationException(ValueViolationsException ex) {
        return new ApiResponse<>(ex.getLocalizedMessage(), HttpStatus.CONFLICT);
    }


    @ExceptionHandler(MailException.class)
    public ApiResponse<String> handleMailException(MailException ex) {
        return new ApiResponse<>("failed to send email: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(TokenNotValidException.class)
    public ApiResponse<String> handleTokenValidationException(TokenNotValidException ex){
        return new ApiResponse<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FieldsMisMatchException.class)
    public ApiResponse<String> handleFieldsMisMatchException(FieldsMisMatchException ex){
        return new ApiResponse<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
