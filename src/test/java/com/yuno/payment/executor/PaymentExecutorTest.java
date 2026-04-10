package com.yuno.payment.executor;

import com.yuno.payment.model.Payment;
import com.yuno.payment.model.enums.Provider;
import com.yuno.payment.provider.PaymentProvider;
import com.yuno.payment.provider.PaymentResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentExecutorTest {

    @Test
    void returnsFirstSuccessfulProviderResultAndStoresProviderOnPayment() {
        PaymentProvider failingProvider = mock(PaymentProvider.class);
        PaymentProvider successProvider = mock(PaymentProvider.class);
        Payment payment = Payment.builder().build();
        PaymentExecutor executor = new PaymentExecutor();

        when(failingProvider.process(payment)).thenReturn(PaymentResult.failure("failed"));
        when(successProvider.process(payment)).thenReturn(PaymentResult.success("txn-1"));
        when(successProvider.getProvider()).thenReturn(Provider.PROVIDER_B);

        PaymentResult result = executor.execute(List.of(failingProvider, successProvider), payment);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransactionId()).isEqualTo("txn-1");
        assertThat(payment.getProvider()).isEqualTo(Provider.PROVIDER_B);
    }

    @Test
    void skipsProviderExceptionAndTriesNextProvider() {
        PaymentProvider throwingProvider = mock(PaymentProvider.class);
        PaymentProvider successProvider = mock(PaymentProvider.class);
        Payment payment = Payment.builder().build();
        PaymentExecutor executor = new PaymentExecutor();

        when(throwingProvider.process(payment)).thenThrow(new RuntimeException("down"));
        when(successProvider.process(payment)).thenReturn(PaymentResult.success("txn-1"));
        when(successProvider.getProvider()).thenReturn(Provider.PROVIDER_A);

        PaymentResult result = executor.execute(List.of(throwingProvider, successProvider), payment);

        assertThat(result.getTransactionId()).isEqualTo("txn-1");
        verify(throwingProvider).process(payment);
        verify(successProvider).process(payment);
    }

    @Test
    void throwsWhenNoProviderSucceeds() {
        PaymentProvider provider = mock(PaymentProvider.class);
        Payment payment = Payment.builder().build();
        PaymentExecutor executor = new PaymentExecutor();

        when(provider.process(payment)).thenReturn(PaymentResult.failure("failed"));

        assertThatThrownBy(() -> executor.execute(List.of(provider), payment))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("All providers failed");
    }

    @Test
    void throwsWhenProviderListIsNull() {
        Payment payment = Payment.builder().id(java.util.UUID.randomUUID()).build();
        PaymentExecutor executor = new PaymentExecutor();

        assertThatThrownBy(() -> executor.execute(null, payment))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No payment providers available");
    }

    @Test
    void throwsWhenProviderListIsEmpty() {
        Payment payment = Payment.builder().id(java.util.UUID.randomUUID()).build();
        PaymentExecutor executor = new PaymentExecutor();

        assertThatThrownBy(() -> executor.execute(List.of(), payment))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No payment providers available");
    }

    @Test
    void allProvidersFailedExceptionCarriesLastProviderExceptionAsCause() {
        PaymentProvider provider = mock(PaymentProvider.class);
        Payment payment = Payment.builder().build();
        PaymentExecutor executor = new PaymentExecutor();
        RuntimeException providerException = new RuntimeException("provider down");

        when(provider.process(payment)).thenThrow(providerException);

        assertThatThrownBy(() -> executor.execute(List.of(provider), payment))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("All providers failed")
                .hasCause(providerException);
    }
}
