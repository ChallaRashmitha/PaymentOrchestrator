package com.yuno.payment.repository;

import com.yuno.payment.model.Account;

import java.util.UUID;

public interface AccountRepository {

    Account findById(UUID id);

    void update(Account account);

    boolean debit(UUID accountId, long amount);
}