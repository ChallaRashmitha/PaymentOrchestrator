package com.yuno.payment.dto;

import com.yuno.payment.model.enums.PaymentStatus;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID paymentId;
    private PaymentStatus status;
    private String transactionId;
}