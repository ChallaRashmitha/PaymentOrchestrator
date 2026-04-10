package com.yuno.payment.service;

import com.yuno.payment.exception.InsufficientBalanceException;
import com.yuno.payment.model.Account;
import com.yuno.payment.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    public Account getAccount(UUID id) {
        return accountRepository.findById(id);
    }

    @Override
    public void credit(UUID accountId, long amount) {
        Account account = accountRepository.findById(accountId);
        account.credit(amount);
        accountRepository.update(account);
    }

    @Override
    public void debit(UUID accountId, long amount) {
        boolean success = accountRepository.debit(accountId, amount);

        if (!success) {
            throw new InsufficientBalanceException("Insufficient balance");
        }
    }
}
