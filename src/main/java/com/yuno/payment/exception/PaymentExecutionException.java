package com.yuno.payment.exception;

public class PaymentExecutionException extends RuntimeException {

    public PaymentExecutionException(String message) {
        super(message);
    }

    public PaymentExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
