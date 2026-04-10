package com.yuno.payment.provider;

import com.yuno.payment.model.Payment;
import com.yuno.payment.model.enums.Provider;
import org.springframework.stereotype.Component;

@Component
public class ProviderA implements PaymentProvider {

    @Override
    public Provider getProvider() {
        return Provider.PROVIDER_A;
    }

    @Override
    public PaymentResult process(Payment payment) {
        return PaymentResult.success("TXN_A_" + System.currentTimeMillis());
    }
}