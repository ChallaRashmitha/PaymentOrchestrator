package com.yuno.payment.exception;

public class SqlLoadException extends RuntimeException {

    public SqlLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
