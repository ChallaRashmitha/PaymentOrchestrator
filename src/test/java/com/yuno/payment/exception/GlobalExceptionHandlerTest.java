package com.yuno.payment.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void paymentNotFoundMapsToNotFound() {
        UUID paymentId = UUID.randomUUID();
        when(request.getRequestURI()).thenReturn("/payments/" + paymentId);

        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(
                new PaymentNotFoundException(paymentId),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("Payment not found: " + paymentId);
        assertThat(response.getBody().getPath()).isEqualTo("/payments/" + paymentId);
    }

    @Test
    void insufficientBalanceMapsToConflict() {
        when(request.getRequestURI()).thenReturn("/payments");

        ResponseEntity<ApiErrorResponse> response = handler.handleConflict(
                new InsufficientBalanceException("Insufficient balance"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Insufficient balance");
    }

    @Test
    void invalidAmountMapsToBadRequest() {
        when(request.getRequestURI()).thenReturn("/payments");

        ResponseEntity<ApiErrorResponse> response = handler.handleBadRequest(
                new InvalidPaymentAmountException("Amount must be positive"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Amount must be positive");
    }

    @Test
    void paymentExecutionFailureMapsToBadGateway() {
        when(request.getRequestURI()).thenReturn("/payments");

        ResponseEntity<ApiErrorResponse> response = handler.handleProviderFailure(
                new PaymentExecutionException("All providers failed"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("All providers failed");
    }

    @Test
    void validationFailureMapsFieldErrorsToBadRequestDetails() {
        when(request.getRequestURI()).thenReturn("/payments");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "amount", "Amount is required"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getDetails()).containsExactly("amount: Amount is required");
    }

    @Test
    void infrastructureFailureMapsToInternalServerError() {
        when(request.getRequestURI()).thenReturn("/payments");

        ResponseEntity<ApiErrorResponse> response = handler.handleInfrastructureFailure(
                new DataAccessResourceFailureException("database down"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Internal persistence error");
        assertThat(response.getBody().getDetails()).containsExactly("database down");
    }

    @Test
    void unexpectedExceptionMapsToInternalServerError() {
        when(request.getRequestURI()).thenReturn("/payments");

        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(
                new IllegalStateException("boom"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Unexpected server error");
        assertThat(response.getBody().getDetails()).containsExactly("boom");
    }
}
