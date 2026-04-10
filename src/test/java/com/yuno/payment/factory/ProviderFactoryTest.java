package com.yuno.payment.factory;

import com.yuno.payment.model.Payment;
import com.yuno.payment.model.enums.PaymentMethod;
import com.yuno.payment.model.enums.Provider;
import com.yuno.payment.provider.PaymentProvider;
import com.yuno.payment.provider.PaymentResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderFactoryTest {

    @Test
    void mapsProviderAToCardAndProviderBToUpi() {
        PaymentProvider providerA = provider(Provider.PROVIDER_A);
        PaymentProvider providerB = provider(Provider.PROVIDER_B);
        ProviderFactory factory = new ProviderFactory(List.of(providerA, providerB));

        assertThat(factory.getProviders(PaymentMethod.CARD)).containsExactly(providerA, providerB);
        assertThat(factory.getProviders(PaymentMethod.UPI)).containsExactly(providerB, providerA);
    }

    @Test
    void skipsMissingFallbackProvider() {
        PaymentProvider providerA = provider(Provider.PROVIDER_A);
        ProviderFactory factory = new ProviderFactory(List.of(providerA));

        assertThat(factory.getProviders(PaymentMethod.CARD)).containsExactly(providerA);
        assertThat(factory.getProviders(PaymentMethod.UPI)).containsExactly(providerA);
    }

    @Test
    void returnsEmptyListForNullMethod() {
        PaymentProvider providerA = provider(Provider.PROVIDER_A);
        ProviderFactory factory = new ProviderFactory(List.of(providerA));

        assertThat(factory.getProviders(null)).isEmpty();
    }

    private PaymentProvider provider(Provider provider) {
        return new PaymentProvider() {
            @Override
            public Provider getProvider() {
                return provider;
            }

            @Override
            public PaymentResult process(Payment payment) {
                return PaymentResult.success("txn");
            }
        };
    }
}
