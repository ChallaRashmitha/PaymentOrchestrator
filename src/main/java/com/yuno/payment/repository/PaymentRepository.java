package com.yuno.payment.repository;

import com.yuno.payment.model.Payment;

import java.util.UUID;

public interface PaymentRepository {
    Payment findById(UUID id);
    void save(Payment payment);
    void update(Payment payment);
    Payment findByIdempotencyKey(String key);
}
