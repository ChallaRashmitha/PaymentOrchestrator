package com.yuno.payment.model;

import com.yuno.payment.dto.CreatePaymentRequest;
import com.yuno.payment.dto.PaymentDetailsResponse;
import com.yuno.payment.dto.PaymentResponse;
import com.yuno.payment.model.enums.PaymentMethod;
import com.yuno.payment.model.enums.PaymentStatus;
import com.yuno.payment.model.enums.Provider;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DtoAndModelTest {

    @Test
    void createPaymentRequestStoresRequestFields() {
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .amount(500L)
                .currency("INR")
                .method(PaymentMethod.CARD)
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .idempotencyKey("key-1")
                .build();

        assertThat(request.getAmount()).isEqualTo(500L);
        assertThat(request.getCurrency()).isEqualTo("INR");
        assertThat(request.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(request.getFromAccountId()).isEqualTo(fromAccountId);
        assertThat(request.getToAccountId()).isEqualTo(toAccountId);
        assertThat(request.getIdempotencyKey()).isEqualTo("key-1");
    }

    @Test
    void paymentResponseStoresResponseFields() {
        UUID paymentId = UUID.randomUUID();

        PaymentResponse response = PaymentResponse.builder()
                .paymentId(paymentId)
                .status(PaymentStatus.SUCCESS)
                .transactionId("txn-1")
                .build();

        assertThat(response.getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getTransactionId()).isEqualTo("txn-1");
    }

    @Test
    void paymentDetailsResponseStoresDetailsFields() {
        UUID paymentId = UUID.randomUUID();

        PaymentDetailsResponse response = PaymentDetailsResponse.builder()
                .paymentId(paymentId)
                .amount(250L)
                .currency("INR")
                .method(PaymentMethod.UPI)
                .status(PaymentStatus.PROCESSING)
                .provider("PROVIDER_B")
                .transactionId("txn-2")
                .build();

        assertThat(response.getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getAmount()).isEqualTo(250L);
        assertThat(response.getCurrency()).isEqualTo("INR");
        assertThat(response.getMethod()).isEqualTo(PaymentMethod.UPI);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(response.getProvider()).isEqualTo("PROVIDER_B");
        assertThat(response.getTransactionId()).isEqualTo("txn-2");
    }

    @Test
    void idempotencyRecordStoresFields() {
        UUID paymentId = UUID.randomUUID();
        Timestamp createdAt = new Timestamp(System.currentTimeMillis());

        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey("key-1")
                .paymentId(paymentId)
                .createdAt(createdAt)
                .build();

        assertThat(record.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(record.getPaymentId()).isEqualTo(paymentId);
        assertThat(record.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void paymentAttemptStoresFields() {
        UUID id = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        PaymentAttempt attempt = PaymentAttempt.builder()
                .id(id)
                .paymentId(paymentId)
                .provider(Provider.PROVIDER_A)
                .status(PaymentStatus.FAILED)
                .errorMessage("failed")
                .attemptNumber(2)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();

        assertThat(attempt.getId()).isEqualTo(id);
        assertThat(attempt.getPaymentId()).isEqualTo(paymentId);
        assertThat(attempt.getProvider()).isEqualTo(Provider.PROVIDER_A);
        assertThat(attempt.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(attempt.getErrorMessage()).isEqualTo("failed");
        assertThat(attempt.getAttemptNumber()).isEqualTo(2);
        assertThat(attempt.getCreatedAt()).isNotNull();
    }

    @Test
    void enumsExposeExpectedValues() {
        assertThat(PaymentMethod.values()).containsExactly(PaymentMethod.CARD, PaymentMethod.UPI);
        assertThat(PaymentStatus.values()).containsExactly(
                PaymentStatus.CREATED,
                PaymentStatus.PROCESSING,
                PaymentStatus.SUCCESS,
                PaymentStatus.FAILED
        );
        assertThat(Provider.values()).containsExactly(Provider.PROVIDER_A, Provider.PROVIDER_B);
    }
}
