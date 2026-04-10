package com.yuno.payment.provider;

import com.yuno.payment.model.Payment;
import com.yuno.payment.model.enums.Provider;

public interface PaymentProvider {

    Provider getProvider();

    PaymentResult process(Payment payment);
}