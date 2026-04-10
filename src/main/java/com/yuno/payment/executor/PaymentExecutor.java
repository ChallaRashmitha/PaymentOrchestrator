package com.yuno.payment.executor;

import com.yuno.payment.exception.PaymentExecutionException;
import com.yuno.payment.model.Payment;
import com.yuno.payment.model.PaymentAttempt;
import com.yuno.payment.model.enums.PaymentStatus;
import com.yuno.payment.provider.PaymentProvider;
import com.yuno.payment.provider.PaymentResult;
import com.yuno.payment.repository.PaymentAttemptRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class PaymentExecutor {

    private final PaymentAttemptRepository attemptRepository;

    public PaymentExecutor(PaymentAttemptRepository attemptRepository) {
        this.attemptRepository = attemptRepository;
    }

    public PaymentResult execute(List<PaymentProvider> providers, Payment payment) {
        if (providers == null || providers.isEmpty()) {
            log.error("No payment providers available for paymentId={}", payment.getId());
            throw new PaymentExecutionException("No payment providers available");
        }
        int attemptNumber = 1;
        Exception lastProviderException = null;
        for (PaymentProvider provider : providers) {
            try {
                log.info("Trying provider={} for paymentId={}", provider.getProvider(), payment.getId());

                PaymentResult result = provider.process(payment);

                PaymentAttempt attempt = PaymentAttempt.builder()
                        .id(UUID.randomUUID())
                        .paymentId(payment.getId())
                        .provider(provider.getProvider())
                        .status(result.isSuccess() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                        .errorMessage(result.getError())
                        .attemptNumber(attemptNumber)
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build();

                attemptRepository.save(attempt);

                if (result.isSuccess()) {
                    payment.setProvider(provider.getProvider());
                    log.info(
                            "Provider={} succeeded for paymentId={} transactionId={}",
                            provider.getProvider(),
                            payment.getId(),
                            result.getTransactionId()
                    );
                    return result;
                }

                log.warn(
                        "Provider={} returned failure for paymentId={} error={}",
                        provider.getProvider(),
                        payment.getId(),
                        result.getError()
                );
            } catch (Exception e) {
                lastProviderException = e;
                PaymentAttempt attempt = PaymentAttempt.builder()
                        .id(UUID.randomUUID())
                        .paymentId(payment.getId())
                        .provider(provider.getProvider())
                        .status(PaymentStatus.FAILED)
                        .errorMessage(e.getMessage())
                        .attemptNumber(attemptNumber)
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .build();

                attemptRepository.save(attempt);
                log.warn(
                        "Provider={} threw exception for paymentId={}. Trying next provider if available",
                        provider.getProvider(),
                        payment.getId(),
                        e
                );
            }
            attemptNumber++;
        }

        log.error("All providers failed for paymentId={}", payment.getId());
        throw new PaymentExecutionException("All providers failed", lastProviderException);
    }
}
