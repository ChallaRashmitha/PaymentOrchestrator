package com.yuno.payment;

import com.yuno.payment.dto.CreatePaymentRequest;
import com.yuno.payment.dto.PaymentResponse;
import com.yuno.payment.model.enums.PaymentMethod;
import com.yuno.payment.service.PaymentService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentOrchestrator {

    private final PaymentService paymentService;

    public PaymentOrchestrator(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void run() {

        System.out.println("Running Payment Flow...");

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .amount(1000L)
                .currency("INR")
                .method(PaymentMethod.CARD)
                .fromAccountId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .toAccountId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .idempotencyKey("yuno-test-123")
                .build();

        try {
            PaymentResponse response = paymentService.createPayment(request);

            System.out.println(" Payment Success!");
            System.out.println("Payment ID: " + response.getPaymentId());
            System.out.println("Status: " + response.getStatus());
            System.out.println("Txn ID: " + response.getTransactionId());

        } catch (Exception e) {
            System.out.println(" Payment Failed: " + e.getMessage());
        }
    }
}