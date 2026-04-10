package com.yuno.payment.executor;

import com.yuno.payment.model.Payment;
import com.yuno.payment.provider.PaymentProvider;
import com.yuno.payment.provider.PaymentResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentExecutor {

    public PaymentResult execute(List<PaymentProvider> providers, Payment payment) {

        int attempt = 1;

        for (PaymentProvider provider : providers) {
            try {

                PaymentResult result = provider.process(payment);

                if (result.isSuccess()) {
                    payment.setProvider(provider.getProvider());
                    return result;
                }

            } catch (Exception e) {
                // log error
            }

            attempt++;
        }

        throw new RuntimeException("All providers failed");
    }
}