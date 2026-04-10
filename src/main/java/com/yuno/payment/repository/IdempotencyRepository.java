package com.yuno.payment.repository;

import java.util.UUID;

public interface IdempotencyRepository {

    boolean exists(String key);

    UUID getPaymentId(String key);

    void save(String key, UUID paymentId);
}