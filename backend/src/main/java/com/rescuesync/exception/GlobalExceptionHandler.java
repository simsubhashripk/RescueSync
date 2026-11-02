package com.rescuesync.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.rescuesync.dto.MessageResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    String message = error.getDefaultMessage();
                    return message != null ? message : "Invalid field value.";
                })
                .collect(Collectors.joining("; "));

        if (errorMessage.isBlank()) {
            errorMessage = "Validation failed for the request fields.";
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(MessageResponse.error(errorMessage));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<MessageResponse> handleResourceNotFound(
            ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MessageResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<MessageResponse> handleDuplicateResource(
            DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(MessageResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidRegionDataException.class)
    public ResponseEntity<MessageResponse> handleInvalidRegionData(
            InvalidRegionDataException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(MessageResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<MessageResponse> handleConflict(
            ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(MessageResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<MessageResponse> handleAccessDenied(
            AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(MessageResponse.error(
                        "Access denied. Administrative privileges are required for this action."));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MessageResponse> handleBadCredentials(
            BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(MessageResponse.error("Invalid username or password."));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<MessageResponse> handleDisabled(
            DisabledException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(MessageResponse.error(
                        "This account is currently disabled or suspended."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponse> handleIllegalArgument(
            IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(MessageResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleGeneralException(
            Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(MessageResponse.error(
                        "An unexpected error occurred: " + ex.getMessage()));
    }
}