package com.yuno.payment.service;

import com.yuno.payment.dto.CreatePaymentRequest;
import com.yuno.payment.dto.PaymentDetailsResponse;
import com.yuno.payment.dto.PaymentResponse;
import com.yuno.payment.executor.PaymentExecutor;
import com.yuno.payment.exception.PaymentExecutionException;
import com.yuno.payment.exception.PaymentNotFoundException;
import com.yuno.payment.factory.ProviderFactory;
import com.yuno.payment.mapper.PaymentMapper;
import com.yuno.payment.model.Payment;
import com.yuno.payment.model.PaymentAttempt;
import com.yuno.payment.provider.PaymentProvider;
import com.yuno.payment.provider.PaymentResult;
import com.yuno.payment.repository.PaymentAttemptRepository;
import com.yuno.payment.repository.PaymentRepository;
import com.yuno.payment.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountService accountService;
    private final IdempotencyRepository idempotencyRepository;
    private final ProviderFactory providerFactory;
    private final PaymentExecutor executor;
    private final PaymentAttemptRepository paymentAttemptRepository;

    @Transactional
    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        log.info(
                "Starting payment creation idempotencyKey={} fromAccountId={} toAccountId={} amount={}",
                request.getIdempotencyKey(),
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount()
        );

        if (idempotencyRepository.exists(request.getIdempotencyKey())) {
            UUID paymentId = idempotencyRepository.getPaymentId(request.getIdempotencyKey());
            log.info("Idempotency hit key={} paymentId={}", request.getIdempotencyKey(), paymentId);
            Payment existing = paymentRepository.findById(paymentId);
            if (existing == null) {
                log.warn("Idempotency record points to missing paymentId={}", paymentId);
                throw new PaymentNotFoundException(paymentId);
            }
            return PaymentMapper.toResponse(existing);
        }
        Payment payment = PaymentMapper.toEntity(request);
        accountService.debit(payment.getFromAccountId(), payment.getAmount());
        paymentRepository.save(payment);
        payment.markProcessing();
        log.info("Payment saved and marked processing paymentId={}", payment.getId());

        try {
            List<PaymentProvider> providers =
                    providerFactory.getProviders(payment.getMethod());
            PaymentResult result = executor.execute(providers, payment);
            payment.markSuccess(result.getTransactionId());
            accountService.credit(payment.getToAccountId(), payment.getAmount());
            log.info(
                    "Payment succeeded paymentId={} provider={} transactionId={}",
                    payment.getId(),
                    payment.getProvider(),
                    payment.getTransactionId()
            );
        } catch (PaymentExecutionException e) {
            payment.markFailed();
            accountService.credit(payment.getFromAccountId(), payment.getAmount());
            log.warn("Payment failed and sender refunded paymentId={}", payment.getId(), e);
        }

        paymentRepository.update(payment);
        idempotencyRepository.save(request.getIdempotencyKey(), payment.getId());
        log.info(
                "Payment finalized paymentId={} status={} idempotencyKey={}",
                payment.getId(),
                payment.getStatus(),
                request.getIdempotencyKey()
        );

        return PaymentMapper.toResponse(payment);
    }

    @Override
    public PaymentDetailsResponse getPayment(UUID paymentId) {

        Payment payment = paymentRepository.findById(paymentId);
        if (payment == null) {
            log.warn("Payment not found paymentId={}", paymentId);
            throw new PaymentNotFoundException(paymentId);
        }

        List<PaymentAttempt> attempts =
                paymentAttemptRepository.findByPaymentId(paymentId);

        return PaymentMapper.toDetailsResponse(payment, attempts);
    }
}
