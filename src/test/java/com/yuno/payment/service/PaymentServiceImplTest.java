package com.yuno.payment.service;

import com.yuno.payment.dto.CreatePaymentRequest;
import com.yuno.payment.dto.PaymentDetailsResponse;
import com.yuno.payment.dto.PaymentResponse;
import com.yuno.payment.executor.PaymentExecutor;
import com.yuno.payment.exception.PaymentExecutionException;
import com.yuno.payment.exception.PaymentNotFoundException;
import com.yuno.payment.factory.ProviderFactory;
import com.yuno.payment.model.Payment;
import com.yuno.payment.model.PaymentAttempt;
import com.yuno.payment.model.enums.PaymentMethod;
import com.yuno.payment.model.enums.PaymentStatus;
import com.yuno.payment.model.enums.Provider;
import com.yuno.payment.provider.PaymentProvider;
import com.yuno.payment.provider.PaymentResult;
import com.yuno.payment.repository.IdempotencyRepository;
import com.yuno.payment.repository.PaymentAttemptRepository;
import com.yuno.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentServiceImplTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final AccountService accountService = mock(AccountService.class);
    private final IdempotencyRepository idempotencyRepository = mock(IdempotencyRepository.class);
    private final ProviderFactory providerFactory = mock(ProviderFactory.class);
    private final PaymentExecutor executor = mock(PaymentExecutor.class);
    private final PaymentAttemptRepository paymentAttemptRepository = mock(PaymentAttemptRepository.class);

    private final PaymentServiceImpl service = new PaymentServiceImpl(
            paymentRepository,
            accountService,
            idempotencyRepository,
            providerFactory,
            executor,
            paymentAttemptRepository
    );

    @Test
    void createPaymentReturnsExistingPaymentWhenIdempotencyKeyExists() {
        UUID paymentId = UUID.randomUUID();
        CreatePaymentRequest request = request();

        Payment existing = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.SUCCESS)
                .transactionId("txn-existing")
                .provider(Provider.PROVIDER_A)
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
    void createPaymentThrowsWhenIdempotencyRecordPointsToMissingPayment() {
        UUID paymentId = UUID.randomUUID();
        CreatePaymentRequest request = request();

        when(idempotencyRepository.exists("key-1")).thenReturn(true);
        when(idempotencyRepository.getPaymentId("key-1")).thenReturn(paymentId);
        when(paymentRepository.findById(paymentId)).thenReturn(null);

        assertThatThrownBy(() -> service.createPayment(request))
                .isInstanceOf(PaymentNotFoundException.class);

        verify(accountService, never()).debit(any(), anyLong());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createPaymentDebitsExecutesProviderCreditsReceiverAndSavesIdempotencyRecord() {
        CreatePaymentRequest request = request();
        PaymentProvider provider = mock(PaymentProvider.class);

        when(idempotencyRepository.exists("key-1")).thenReturn(false);
        when(providerFactory.getProviders(PaymentMethod.CARD)).thenReturn(List.of(provider));

        when(executor.execute(any(), any()))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(1);
                    payment.setProvider(Provider.PROVIDER_A);
                    return PaymentResult.success("txn-1");
                });

        PaymentResponse response = service.createPayment(request);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());

        Payment savedPayment = paymentCaptor.getValue();

        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(savedPayment.getTransactionId()).isEqualTo("txn-1");

        verify(accountService).debit(request.getFromAccountId(), request.getAmount());
        verify(accountService).credit(request.getToAccountId(), request.getAmount());
        verify(paymentRepository).update(savedPayment);
        verify(idempotencyRepository).save("key-1", savedPayment.getId());

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void createPaymentMarksFailedAndRefundsSenderWhenExecutionFails() {
        CreatePaymentRequest request = request();

        when(idempotencyRepository.exists("key-1")).thenReturn(false);
        when(providerFactory.getProviders(PaymentMethod.CARD))
                .thenReturn(List.of(mock(PaymentProvider.class)));

        when(executor.execute(any(), any()))
                .thenThrow(new PaymentExecutionException("all failed"));

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
        when(paymentAttemptRepository.findByPaymentId(paymentId)).thenReturn(List.of());

        PaymentDetailsResponse response = service.getPayment(paymentId);

        assertThat(response.getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getAmount()).isEqualTo(500L);
        assertThat(response.getCurrency()).isEqualTo("INR");
        assertThat(response.getMethod()).isEqualTo(PaymentMethod.UPI);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getProvider()).isEqualTo("PROVIDER_B");
        assertThat(response.getTransactionId()).isEqualTo("txn-2");

        verify(paymentAttemptRepository).findByPaymentId(paymentId);
    }

    @Test
    void getPaymentBuildsDetailsResponseWithNullProvider() {
        UUID paymentId = UUID.randomUUID();

        Payment payment = Payment.builder()
                .id(paymentId)
                .amount(500L)
                .currency("INR")
                .method(PaymentMethod.UPI)
                .status(PaymentStatus.PROCESSING)
                .provider(null)
                .transactionId(null)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(payment);
        when(paymentAttemptRepository.findByPaymentId(paymentId)).thenReturn(List.of());

        PaymentDetailsResponse response = service.getPayment(paymentId);

        assertThat(response.getProvider()).isNull();
        assertThat(response.getTransactionId()).isNull();
    }

    @Test
    void getPaymentThrowsWhenPaymentDoesNotExist() {
        UUID paymentId = UUID.randomUUID();

        when(paymentRepository.findById(paymentId)).thenReturn(null);

        assertThatThrownBy(() -> service.getPayment(paymentId))
                .isInstanceOf(PaymentNotFoundException.class);
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