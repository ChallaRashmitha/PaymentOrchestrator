package com.yuno.payment.service;

import com.yuno.payment.model.Account;

import java.util.UUID;

public interface AccountService {

    Account getAccount(UUID id);

    void credit(UUID accountId, long amount);

    void debit(UUID accountId, long amount);
}