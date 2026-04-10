package com.yuno.payment.executor;

import com.yuno.payment.exception.PaymentExecutionException;
import com.yuno.payment.model.Payment;
import com.yuno.payment.provider.PaymentProvider;
import com.yuno.payment.provider.PaymentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class PaymentExecutor {

    public PaymentResult execute(List<PaymentProvider> providers, Payment payment) {
        if (providers == null || providers.isEmpty()) {
            log.error("No payment providers available for paymentId={}", payment.getId());
            throw new PaymentExecutionException("No payment providers available");
        }

        Exception lastProviderException = null;
        for (PaymentProvider provider : providers) {
            try {
                log.info("Trying provider={} for paymentId={}", provider.getProvider(), payment.getId());

                PaymentResult result = provider.process(payment);

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
                log.warn(
                        "Provider={} threw exception for paymentId={}. Trying next provider if available",
                        provider.getProvider(),
                        payment.getId(),
                        e
                );
            }
        }

        log.error("All providers failed for paymentId={}", payment.getId());
        throw new PaymentExecutionException("All providers failed", lastProviderException);
    }
}
