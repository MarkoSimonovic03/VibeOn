package com.metropolitan.VibeOn.controller;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            MethodValidationException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<String> handleInvalidRequest(Exception exception) {
        return ResponseEntity.badRequest().body("Invalid request.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("Uploaded file is too large.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> handleAuthentication(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleMissingResource(NoResourceFoundException exception) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpectedError(Exception exception) {
        log.error("Unhandled request error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
    }
}
