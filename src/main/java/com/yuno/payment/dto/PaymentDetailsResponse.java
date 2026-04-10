package com.yuno.payment.dto;

import com.yuno.payment.model.enums.*;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetailsResponse {

    private UUID paymentId;
    private Long amount;
    private String currency;
    private PaymentMethod method;
    private PaymentStatus status;
    private String provider;
    private String transactionId;
}