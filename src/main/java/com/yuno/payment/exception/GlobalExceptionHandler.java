package com.yuno.payment.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        List<String> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .toList();

        log.warn("Validation failed path={} details={}", request.getRequestURI(), details);
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request.getRequestURI(),
                details
        );
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            PaymentNotFoundException exception,
            HttpServletRequest request) {

        log.warn("Resource not found path={} message={}", request.getRequestURI(), exception.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler({
            InsufficientBalanceException.class,
            InvalidPaymentStateException.class
    })
    public ResponseEntity<ApiErrorResponse> handleConflict(
            RuntimeException exception,
            HttpServletRequest request) {

        log.warn("Conflict path={} message={}", request.getRequestURI(), exception.getMessage());
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(InvalidPaymentAmountException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            InvalidPaymentAmountException exception,
            HttpServletRequest request) {

        log.warn("Bad request path={} message={}", request.getRequestURI(), exception.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(PaymentExecutionException.class)
    public ResponseEntity<ApiErrorResponse> handleProviderFailure(
            PaymentExecutionException exception,
            HttpServletRequest request) {

        log.warn("Provider failure path={} message={}", request.getRequestURI(), exception.getMessage(), exception);
        return buildResponse(HttpStatus.BAD_GATEWAY, exception.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler({
            SqlLoadException.class,
            DataAccessException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInfrastructureFailure(
            RuntimeException exception,
            HttpServletRequest request) {

        log.error("Infrastructure failure path={}", request.getRequestURI(), exception);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal persistence error",
                request.getRequestURI(),
                List.of(exception.getMessage())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {

        log.error("Unexpected failure path={}", request.getRequestURI(), exception);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected server error",
                request.getRequestURI(),
                List.of(exception.getMessage())
        );
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            String path,
            List<String> details) {

        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .details(details)
                .build();

        return ResponseEntity.status(status).body(response);
    }
}
