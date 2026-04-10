package com.yuno.payment.model;

import lombok.*;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {
    private String idempotencyKey;
    private UUID paymentId;
    private Timestamp createdAt;
}