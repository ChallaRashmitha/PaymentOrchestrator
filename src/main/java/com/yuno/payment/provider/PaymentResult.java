package com.yuno.payment.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResult {

    private boolean success;
    private String transactionId;
    private String error;

    public static PaymentResult success(String txnId) {
        return new PaymentResult(true, txnId, null);
    }

    public static PaymentResult failure(String error) {
        return new PaymentResult(false, null, error);
    }
}