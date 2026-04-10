package com.yuno.payment.model;

import com.yuno.payment.exception.InvalidPaymentStateException;
import com.yuno.payment.model.enums.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private UUID id;
    private String idempotencyKey;
    private long amount;
    private String currency;
    private PaymentMethod method;
    private PaymentStatus status;
    private Provider provider;
    private UUID fromAccountId;
    private UUID toAccountId;
    private String transactionId;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public void markProcessing() {
        validateTransition(this.status, PaymentStatus.PROCESSING);
        this.status = PaymentStatus.PROCESSING;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    public void markSuccess(String transactionId) {
        validateTransition(this.status, PaymentStatus.SUCCESS);
        this.status = PaymentStatus.SUCCESS;
        this.transactionId = transactionId;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    public void markFailed() {
        validateTransition(this.status, PaymentStatus.FAILED);
        this.status = PaymentStatus.FAILED;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    private void validateTransition(PaymentStatus current, PaymentStatus next) {
        if (current == PaymentStatus.CREATED && next == PaymentStatus.PROCESSING) return;
        if (current == PaymentStatus.PROCESSING &&
                (next == PaymentStatus.SUCCESS || next == PaymentStatus.FAILED)) return;

        throw new InvalidPaymentStateException("Invalid state transition: " + current + " -> " + next);
    }
}
