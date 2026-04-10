package com.yuno.payment.mapper;

import com.yuno.payment.dto.CreatePaymentRequest;
import com.yuno.payment.dto.PaymentResponse;
import com.yuno.payment.model.Payment;
import com.yuno.payment.model.enums.PaymentMethod;
import com.yuno.payment.model.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMapperTest {

    @Test
    void toEntityCreatesPaymentFromRequest() {
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .amount(100L)
                .currency("INR")
                .method(PaymentMethod.CARD)
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .idempotencyKey("key-1")
                .build();

        Payment payment = PaymentMapper.toEntity(request);

        assertThat(payment.getId()).isNotNull();
        assertThat(payment.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(payment.getAmount()).isEqualTo(100L);
        assertThat(payment.getCurrency()).isEqualTo("INR");
        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payment.getFromAccountId()).isEqualTo(fromAccountId);
        assertThat(payment.getToAccountId()).isEqualTo(toAccountId);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(payment.getCreatedAt()).isNotNull();
        assertThat(payment.getUpdatedAt()).isNotNull();
    }

    @Test
    void toResponseCreatesPaymentResponseFromPayment() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.SUCCESS)
                .transactionId("txn-1")
                .build();

        PaymentResponse response = PaymentMapper.toResponse(payment);

        assertThat(response.getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getTransactionId()).isEqualTo("txn-1");
    }
}
