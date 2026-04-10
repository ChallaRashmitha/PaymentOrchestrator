package com.yuno.payment.provider;

import com.yuno.payment.model.Payment;
import com.yuno.payment.model.enums.Provider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderTest {

    @Test
    void providerAAlwaysReturnsSuccess() {
        ProviderA provider = new ProviderA();

        PaymentResult result = provider.process(Payment.builder().build());

        assertThat(provider.getProvider()).isEqualTo(Provider.PROVIDER_A);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransactionId()).startsWith("TXN_A_");
    }

    @Test
    void providerBReturnsProviderBResultShape() {
        ProviderB provider = new ProviderB();

        PaymentResult result = provider.process(Payment.builder().build());

        assertThat(provider.getProvider()).isEqualTo(Provider.PROVIDER_B);
        if (result.isSuccess()) {
            assertThat(result.getTransactionId()).startsWith("TXN_B_");
            assertThat(result.getError()).isNull();
        } else {
            assertThat(result.getTransactionId()).isNull();
            assertThat(result.getError()).isEqualTo("Provider B failed");
        }
    }
}
