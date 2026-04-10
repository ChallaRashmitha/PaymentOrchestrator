package com.yuno.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAttemptResponse {

    private String provider;
    private String status;
    private String errorMessage;
    private int attemptNumber;
}