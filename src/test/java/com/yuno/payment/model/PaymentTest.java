package com.yuno.payment.model;

import com.yuno.payment.model.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void markProcessingMovesCreatedPaymentToProcessing() {
        Payment payment = Payment.builder()
                .status(PaymentStatus.CREATED)
                .build();

        payment.markProcessing();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(payment.getUpdatedAt()).isNotNull();
    }

    @Test
    void markSuccessMovesProcessingPaymentToSuccessAndStoresTransactionId() {
        Payment payment = Payment.builder()
                .status(PaymentStatus.PROCESSING)
                .build();

        payment.markSuccess("txn-1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getTransactionId()).isEqualTo("txn-1");
        assertThat(payment.getUpdatedAt()).isNotNull();
    }

    @Test
    void markFailedMovesProcessingPaymentToFailed() {
        Payment payment = Payment.builder()
                .status(PaymentStatus.PROCESSING)
                .build();

        payment.markFailed();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getUpdatedAt()).isNotNull();
    }

    @Test
    void rejectsInvalidTransition() {
        Payment payment = Payment.builder()
                .status(PaymentStatus.CREATED)
                .build();

        assertThatThrownBy(() -> payment.markSuccess("txn-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid state transition: CREATED -> SUCCESS");
    }
}
