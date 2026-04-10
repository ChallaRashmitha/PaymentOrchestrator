package com.yuno.payment.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    void debitSubtractsAmountWhenBalanceIsSufficient() {
        Account account = Account.builder().balance(1_000L).build();

        account.debit(250L);

        assertThat(account.getBalance()).isEqualTo(750L);
    }

    @Test
    void debitRejectsNonPositiveAmount() {
        Account account = Account.builder().balance(1_000L).build();

        assertThatThrownBy(() -> account.debit(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be positive");
    }

    @Test
    void debitRejectsInsufficientBalance() {
        Account account = Account.builder().balance(100L).build();

        assertThatThrownBy(() -> account.debit(101L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient balance");
    }

    @Test
    void creditAddsAmountWhenAmountIsPositive() {
        Account account = Account.builder().balance(100L).build();

        account.credit(50L);

        assertThat(account.getBalance()).isEqualTo(150L);
    }

    @Test
    void creditRejectsNonPositiveAmount() {
        Account account = Account.builder().balance(100L).build();

        assertThatThrownBy(() -> account.credit(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be positive");
    }
}
