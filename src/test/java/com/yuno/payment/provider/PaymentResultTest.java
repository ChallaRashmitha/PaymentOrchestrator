package com.yuno.payment.provider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentResultTest {

    @Test
    void successBuildsSuccessfulResult() {
        PaymentResult result = PaymentResult.success("txn-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransactionId()).isEqualTo("txn-1");
        assertThat(result.getError()).isNull();
    }

    @Test
    void failureBuildsFailedResult() {
        PaymentResult result = PaymentResult.failure("failed");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getTransactionId()).isNull();
        assertThat(result.getError()).isEqualTo("failed");
    }
}
