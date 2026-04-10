package com.yuno.payment.service;

import com.yuno.payment.dto.CreatePaymentRequest;
import com.yuno.payment.dto.PaymentDetailsResponse;
import com.yuno.payment.dto.PaymentResponse;
import com.yuno.payment.executor.PaymentExecutor;
import com.yuno.payment.factory.ProviderFactory;
import com.yuno.payment.model.Payment;
import com.yuno.payment.model.enums.PaymentMethod;
import com.yuno.payment.model.enums.PaymentStatus;
import com.yuno.payment.model.enums.Provider;
import com.yuno.payment.provider.PaymentProvider;
import com.yuno.payment.provider.PaymentResult;
import com.yuno.payment.repository.IdempotencyRepository;
import com.yuno.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceImplTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final AccountService accountService = mock(AccountService.class);
    private final IdempotencyRepository idempotencyRepository = mock(IdempotencyRepository.class);
    private final ProviderFactory providerFactory = mock(ProviderFactory.class);
    private final PaymentExecutor executor = mock(PaymentExecutor.class);
    private final PaymentServiceImpl service = new PaymentServiceImpl(
            paymentRepository,
            accountService,
            idempotencyRepository,
            providerFactory,
            executor
    );

    @Test
    void createPaymentReturnsExistingPaymentWhenIdempotencyKeyExists() {
        UUID paymentId = UUID.randomUUID();
        CreatePaymentRequest request = request();
        Payment existing = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.SUCCESS)
                .transactionId("txn-existing")
                .build();

        when(idempotencyRepository.exists("key-1")).thenReturn(true);
        when(idempotencyRepository.getPaymentId("key-1")).thenReturn(paymentId);
        when(paymentRepository.findById(paymentId)).thenReturn(existing);

        PaymentResponse response = service.createPayment(request);

        assertThat(response.getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getTransactionId()).isEqualTo("txn-existing");
        verify(accountService, never()).debit(any(), anyLong());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createPaymentDebitsExecutesProviderCreditsReceiverAndSavesIdempotencyRecord() {
        CreatePaymentRequest request = request();
        PaymentProvider provider = mock(PaymentProvider.class);

        when(idempotencyRepository.exists("key-1")).thenReturn(false);
        when(providerFactory.getProviders(PaymentMethod.CARD)).thenReturn(List.of(provider));
        when(executor.execute(any(), any())).thenReturn(PaymentResult.success("txn-1"));

        PaymentResponse response = service.createPayment(request);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(savedPayment.getTransactionId()).isEqualTo("txn-1");
        assertThat(response.getPaymentId()).isEqualTo(savedPayment.getId());
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(accountService).debit(request.getFromAccountId(), request.getAmount());
        verify(accountService).credit(request.getToAccountId(), request.getAmount());
        verify(paymentRepository).update(savedPayment);
        verify(idempotencyRepository).save("key-1", savedPayment.getId());
    }

    @Test
    void createPaymentMarksFailedAndRefundsSenderWhenExecutionFails() {
        CreatePaymentRequest request = request();

        when(idempotencyRepository.exists("key-1")).thenReturn(false);
        when(providerFactory.getProviders(PaymentMethod.CARD)).thenReturn(List.of());
        when(executor.execute(any(), any())).thenThrow(new RuntimeException("all failed"));

        PaymentResponse response = service.createPayment(request);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(accountService).credit(request.getFromAccountId(), request.getAmount());
        verify(accountService, never()).credit(request.getToAccountId(), request.getAmount());
        verify(paymentRepository).update(savedPayment);
        verify(idempotencyRepository).save("key-1", savedPayment.getId());
    }

    @Test
    void getPaymentBuildsDetailsResponse() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId)
                .amount(500L)
                .currency("INR")
                .method(PaymentMethod.UPI)
                .status(PaymentStatus.SUCCESS)
                .provider(Provider.PROVIDER_B)
                .transactionId("txn-2")
                .build();
        when(paymentRepository.findById(paymentId)).thenReturn(payment);

        PaymentDetailsResponse response = service.getPayment(paymentId);

        assertThat(response.getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getAmount()).isEqualTo(500L);
        assertThat(response.getCurrency()).isEqualTo("INR");
        assertThat(response.getMethod()).isEqualTo(PaymentMethod.UPI);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getProvider()).isEqualTo("PROVIDER_B");
        assertThat(response.getTransactionId()).isEqualTo("txn-2");
    }

    private CreatePaymentRequest request() {
        return CreatePaymentRequest.builder()
                .amount(100L)
                .currency("INR")
                .method(PaymentMethod.CARD)
                .fromAccountId(UUID.randomUUID())
                .toAccountId(UUID.randomUUID())
                .idempotencyKey("key-1")
                .build();
    }
}
