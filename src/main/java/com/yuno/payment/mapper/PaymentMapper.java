package com.yuno.payment.mapper;

import com.yuno.payment.dto.CreatePaymentRequest;
import com.yuno.payment.dto.PaymentAttemptResponse;
import com.yuno.payment.dto.PaymentDetailsResponse;
import com.yuno.payment.dto.PaymentResponse;
import com.yuno.payment.model.Payment;
import com.yuno.payment.model.PaymentAttempt;
import com.yuno.payment.model.enums.PaymentStatus;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public class PaymentMapper {

    // DTO → Entity
    public static Payment toEntity(CreatePaymentRequest request) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .idempotencyKey(request.getIdempotencyKey())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .method(request.getMethod())
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .status(PaymentStatus.CREATED)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .updatedAt(new Timestamp(System.currentTimeMillis()))
                .build();
    }

    // Entity → Response DTO
    public static PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .build();
    }

    public static PaymentDetailsResponse toDetailsResponse(
                Payment payment,
                List<PaymentAttempt> attempts
        ) {

            List<PaymentAttemptResponse> attemptResponses = attempts.stream()
                    .map(a -> PaymentAttemptResponse.builder()
                            .provider(a.getProvider().name())
                            .status(a.getStatus().name())
                            .errorMessage(a.getErrorMessage())
                            .attemptNumber(a.getAttemptNumber())
                            .build())
                    .toList();

            return PaymentDetailsResponse.builder()
                    .paymentId(payment.getId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .method(payment.getMethod())
                    .status(payment.getStatus())
                    .provider(payment.getProvider() != null ? payment.getProvider().name() : null)
                    .transactionId(payment.getTransactionId())
                    .attempts(attemptResponses)
                    .build();
        }
}