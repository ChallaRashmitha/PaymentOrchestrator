package com.yuno.payment.repository;

import com.yuno.payment.model.PaymentAttempt;

import java.util.List;
import java.util.UUID;

public interface PaymentAttemptRepository {
    void save(PaymentAttempt attempt);
    List<PaymentAttempt> findByPaymentId(UUID paymentId);
}