package com.yuno.payment.model;

import com.yuno.payment.exception.InsufficientBalanceException;
import com.yuno.payment.exception.InvalidPaymentAmountException;
import lombok.*;
import java.sql.Timestamp;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    private UUID id;
    private String userId;
    private long balance;
    private String currency;
    private Timestamp createdAt;

    public void debit(long amount) {
        if (amount <= 0) {
            throw new InvalidPaymentAmountException("Amount must be positive");
        }
        if (this.balance < amount) {
            throw new InsufficientBalanceException("Insufficient balance");
        }
        this.balance -= amount;
    }

    public void credit(long amount) {
        if (amount <= 0) {
            throw new InvalidPaymentAmountException("Amount must be positive");
        }
        this.balance += amount;
    }
}
