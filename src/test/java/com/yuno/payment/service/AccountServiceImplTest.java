package com.yuno.payment.service;

import com.yuno.payment.model.Account;
import com.yuno.payment.repository.AccountRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountServiceImplTest {

    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final AccountServiceImpl service = new AccountServiceImpl(accountRepository);

    @Test
    void getAccountDelegatesToRepository() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.builder().id(accountId).build();
        when(accountRepository.findById(accountId)).thenReturn(account);

        Account result = service.getAccount(accountId);

        assertThat(result).isSameAs(account);
    }

    @Test
    void creditLoadsAccountCreditsAndPersistsUpdatedAccount() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.builder().id(accountId).balance(100L).build();
        when(accountRepository.findById(accountId)).thenReturn(account);

        service.credit(accountId, 40L);

        assertThat(account.getBalance()).isEqualTo(140L);
        verify(accountRepository).update(account);
    }

    @Test
    void debitDelegatesAtomicDebitToRepository() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.debit(accountId, 50L)).thenReturn(true);

        service.debit(accountId, 50L);

        verify(accountRepository).debit(accountId, 50L);
    }

    @Test
    void debitThrowsWhenRepositoryCannotDebit() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.debit(accountId, 50L)).thenReturn(false);

        assertThatThrownBy(() -> service.debit(accountId, 50L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient balance");
    }
}
