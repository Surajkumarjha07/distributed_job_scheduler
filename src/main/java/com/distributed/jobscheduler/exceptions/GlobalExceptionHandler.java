package com.distributed.jobscheduler.exceptions;

import com.distributed.jobscheduler.utils.Utils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return build(status, message, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, List<String> details) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), message, details));
    }

    // 404 — resource not found
    @ExceptionHandler(CustomExceptions.ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(CustomExceptions.ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Not Found");
    }

    // 409 — duplicate resource (e.g. email already exists)
    @ExceptionHandler(CustomExceptions.DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(CustomExceptions.DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    // 409 — catches raw DB unique constraint violations you didn't explicitly check for
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String sqlState = Utils.extractSQLState(ex);
        System.err.println("Data integrity violation. " + "SQLState::::: " +  sqlState + " CAUSE:::: " + ex.getMostSpecificCause().getMessage());

        if ("23505".equals(sqlState)) {
            return build(HttpStatus.CONFLICT, "A record with these details already exists.");
        }
        if ("23502".equals(sqlState)) {
            return build(HttpStatus.BAD_REQUEST, "A required field is missing.");
        }
        if ("23503".equals(sqlState)) {
            return build(HttpStatus.BAD_REQUEST, "Referenced resource does not exist.");
        }
        if ("23514".equals(sqlState)) {
            return build(HttpStatus.BAD_REQUEST, "Data violates a business rule constraint.");
        }

        return build(HttpStatus.BAD_REQUEST, "Invalid or missing data.");
    }

    // 400 — @Valid validation failures (e.g. @NotBlank, @Email, @Size)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();

        return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    // 401 — bad login credentials
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    // 401 — any other authentication failure (e.g. missing/invalid JWT)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication failed");
    }

    // 403 — authenticated but not allowed (e.g. wrong role)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
    }

    // 400 — bad request data that doesn't fit validation (e.g. malformed JSON)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 500 — catch-all fallback for anything unhandled
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        System.err.println("ERROR IN APP:::: " + ex.getMessage());
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again later.");
    }
}