package com.yuno.payment.provider;

import com.yuno.payment.model.Payment;
import com.yuno.payment.model.enums.Provider;
import org.springframework.stereotype.Component;

@Component
public class ProviderB implements PaymentProvider {

    @Override
    public Provider getProvider() {
        return Provider.PROVIDER_B;
    }

    @Override
    public PaymentResult process(Payment payment) {
        if (Math.random() > 0.5) {
            return PaymentResult.failure("Provider B failed");
        }
        return PaymentResult.success("TXN_B_" + System.currentTimeMillis());
    }
}
