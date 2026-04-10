package com.yuno.payment.executor;

import com.yuno.payment.exception.PaymentExecutionException;
import com.yuno.payment.model.Payment;
import com.yuno.payment.model.enums.Provider;
import com.yuno.payment.provider.PaymentProvider;
import com.yuno.payment.provider.PaymentResult;
import com.yuno.payment.repository.PaymentAttemptRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentExecutorTest {

    private final PaymentAttemptRepository attemptRepository = mock(PaymentAttemptRepository.class);
    private final PaymentExecutor executor = new PaymentExecutor(attemptRepository);

    @Test
    void returnsFirstSuccessfulProviderResultAndStoresProviderOnPayment() {

        PaymentProvider failingProvider = mock(PaymentProvider.class);
        PaymentProvider successProvider = mock(PaymentProvider.class);

        Payment payment = Payment.builder().build();

        when(failingProvider.process(payment)).thenReturn(PaymentResult.failure("failed"));
        when(failingProvider.getProvider()).thenReturn(Provider.PROVIDER_A);

        when(successProvider.process(payment)).thenReturn(PaymentResult.success("txn-1"));
        when(successProvider.getProvider()).thenReturn(Provider.PROVIDER_B);

        PaymentResult result = executor.execute(List.of(failingProvider, successProvider), payment);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransactionId()).isEqualTo("txn-1");
        assertThat(payment.getProvider()).isEqualTo(Provider.PROVIDER_B);

        verify(attemptRepository, times(2)).save(any()); // 2 attempts
    }

    @Test
    void skipsProviderExceptionAndTriesNextProvider() {

        PaymentProvider throwingProvider = mock(PaymentProvider.class);
        PaymentProvider successProvider = mock(PaymentProvider.class);

        Payment payment = Payment.builder().build();

        when(throwingProvider.process(payment)).thenThrow(new RuntimeException("down"));
        when(throwingProvider.getProvider()).thenReturn(Provider.PROVIDER_A);

        when(successProvider.process(payment)).thenReturn(PaymentResult.success("txn-1"));
        when(successProvider.getProvider()).thenReturn(Provider.PROVIDER_B);

        PaymentResult result = executor.execute(List.of(throwingProvider, successProvider), payment);

        assertThat(result.getTransactionId()).isEqualTo("txn-1");

        verify(throwingProvider).process(payment);
        verify(successProvider).process(payment);
        verify(attemptRepository, times(2)).save(any());
    }

    @Test
    void throwsWhenNoProviderSucceeds() {

        PaymentProvider provider = mock(PaymentProvider.class);
        Payment payment = Payment.builder().build();

        when(provider.process(payment)).thenReturn(PaymentResult.failure("failed"));
        when(provider.getProvider()).thenReturn(Provider.PROVIDER_A);

        assertThatThrownBy(() -> executor.execute(List.of(provider), payment))
                .isInstanceOf(PaymentExecutionException.class)
                .hasMessage("All providers failed");

        verify(attemptRepository).save(any());
    }

    @Test
    void throwsWhenProviderListIsNull() {

        Payment payment = Payment.builder().build();

        assertThatThrownBy(() -> executor.execute(null, payment))
                .isInstanceOf(PaymentExecutionException.class)
                .hasMessage("No payment providers available");
    }

    @Test
    void throwsWhenProviderListIsEmpty() {

        Payment payment = Payment.builder().build();

        assertThatThrownBy(() -> executor.execute(List.of(), payment))
                .isInstanceOf(PaymentExecutionException.class)
                .hasMessage("No payment providers available");
    }

    @Test
    void allProvidersFailedExceptionCarriesLastProviderExceptionAsCause() {

        PaymentProvider provider = mock(PaymentProvider.class);
        Payment payment = Payment.builder().build();

        RuntimeException providerException = new RuntimeException("provider down");

        when(provider.process(payment)).thenThrow(providerException);
        when(provider.getProvider()).thenReturn(Provider.PROVIDER_A);

        assertThatThrownBy(() -> executor.execute(List.of(provider), payment))
                .isInstanceOf(PaymentExecutionException.class)
                .hasMessage("All providers failed")
                .hasCause(providerException);

        verify(attemptRepository).save(any());
    }
}