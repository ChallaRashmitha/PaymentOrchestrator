package com.yuno.payment.model;

import com.yuno.payment.model.enums.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAttempt {
    private UUID id;
    private UUID paymentId;
    private Provider provider;
    private PaymentStatus status;
    private String errorMessage;
    private int attemptNumber;
    private Timestamp createdAt;
}