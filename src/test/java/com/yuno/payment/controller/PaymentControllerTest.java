package com.yuno.payment.controller;

import com.yuno.payment.dto.CreatePaymentRequest;
import com.yuno.payment.dto.PaymentDetailsResponse;
import com.yuno.payment.dto.PaymentResponse;
import com.yuno.payment.model.enums.PaymentMethod;
import com.yuno.payment.model.enums.PaymentStatus;
import com.yuno.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentControllerTest {

    private final PaymentService paymentService = mock(PaymentService.class);
    private final PaymentController controller = new PaymentController(paymentService);

    @Test
    void createPaymentReturnsServiceResponse() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .amount(100L)
                .currency("INR")
                .method(PaymentMethod.CARD)
                .fromAccountId(UUID.randomUUID())
                .toAccountId(UUID.randomUUID())
                .idempotencyKey("key-1")
                .build();
        PaymentResponse serviceResponse = PaymentResponse.builder()
                .paymentId(UUID.randomUUID())
                .status(PaymentStatus.SUCCESS)
                .transactionId("txn-1")
                .build();
        when(paymentService.createPayment(request)).thenReturn(serviceResponse);

        ResponseEntity<PaymentResponse> response = controller.createPayment(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(serviceResponse);
        verify(paymentService).createPayment(request);
    }

    @Test
    void getPaymentReturnsServiceResponse() {
        UUID paymentId = UUID.randomUUID();
        PaymentDetailsResponse serviceResponse = PaymentDetailsResponse.builder()
                .paymentId(paymentId)
                .status(PaymentStatus.SUCCESS)
                .build();
        when(paymentService.getPayment(paymentId)).thenReturn(serviceResponse);

        ResponseEntity<PaymentDetailsResponse> response = controller.getPayment(paymentId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isSameAs(serviceResponse);
        verify(paymentService).getPayment(paymentId);
    }
}
