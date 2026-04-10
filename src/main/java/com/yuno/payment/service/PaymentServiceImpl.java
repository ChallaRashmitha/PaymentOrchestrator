package com.yuno.payment.service;

import com.yuno.payment.dto.CreatePaymentRequest;
import com.yuno.payment.dto.PaymentDetailsResponse;
import com.yuno.payment.dto.PaymentResponse;
import com.yuno.payment.executor.PaymentExecutor;
import com.yuno.payment.factory.ProviderFactory;
import com.yuno.payment.mapper.PaymentMapper;
import com.yuno.payment.model.Payment;
import com.yuno.payment.provider.PaymentProvider;
import com.yuno.payment.provider.PaymentResult;
import com.yuno.payment.repository.PaymentRepository;
import com.yuno.payment.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountService accountService;
    private final IdempotencyRepository idempotencyRepository;
    private final ProviderFactory providerFactory;
    private final PaymentExecutor executor;

    @Transactional
    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request) {

        if (idempotencyRepository.exists(request.getIdempotencyKey())) {
            UUID paymentId = idempotencyRepository.getPaymentId(request.getIdempotencyKey());
            Payment existing = paymentRepository.findById(paymentId);
            return PaymentMapper.toResponse(existing);
        }
        Payment payment = PaymentMapper.toEntity(request);
        accountService.debit(payment.getFromAccountId(), payment.getAmount());
        paymentRepository.save(payment);
        payment.markProcessing();

        try {
            List<PaymentProvider> providers =
                    providerFactory.getProviders(payment.getMethod());
            PaymentResult result = executor.execute(providers, payment);
            payment.markSuccess(result.getTransactionId());
            accountService.credit(payment.getToAccountId(), payment.getAmount());
        } catch (Exception e) {
            payment.markFailed();
            accountService.credit(payment.getFromAccountId(), payment.getAmount());
        }

        paymentRepository.update(payment);
        idempotencyRepository.save(request.getIdempotencyKey(), payment.getId());

        return PaymentMapper.toResponse(payment);
    }

    @Override
    public PaymentDetailsResponse getPayment(UUID paymentId) {

        Payment payment = paymentRepository.findById(paymentId);

        return PaymentDetailsResponse.builder()
                .paymentId(payment.getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .provider(payment.getProvider() != null ? payment.getProvider().name() : null)
                .transactionId(payment.getTransactionId())
                .build();
    }
}
