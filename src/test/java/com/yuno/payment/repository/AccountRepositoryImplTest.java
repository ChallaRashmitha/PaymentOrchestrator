package com.yuno.payment.repository;

import com.yuno.payment.dao.ReadDao;
import com.yuno.payment.dao.WriteDao;
import com.yuno.payment.model.Account;
import com.yuno.payment.util.SqlLoader;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountRepositoryImplTest {

    private final SqlLoader sqlLoader = mock(SqlLoader.class);
    private final ReadDao readDao = mock(ReadDao.class);
    private final WriteDao writeDao = mock(WriteDao.class);
    private final AccountRepositoryImpl repository = new AccountRepositoryImpl(sqlLoader, readDao, writeDao);

    @Test
    void findByIdLoadsSqlAndQueriesAccount() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.builder().id(accountId).build();
        when(sqlLoader.loadSql("sql/account/find_account.sql")).thenReturn("select-account");
        when(readDao.queryForObject(eq("select-account"), any(), eq(accountId))).thenReturn(account);

        Account result = repository.findById(accountId);

        assertThat(result).isSameAs(account);
    }

    @Test
    void updateLoadsSqlAndUpdatesBalance() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.builder().id(accountId).balance(500L).build();
        when(sqlLoader.loadSql("sql/account/update_balance.sql")).thenReturn("update-account");

        repository.update(account);

        verify(writeDao).update("update-account", 500L, accountId);
    }

    @Test
    void debitReturnsTrueWhenRowsWereUpdated() {
        UUID accountId = UUID.randomUUID();
        when(sqlLoader.loadSql("sql/account/debit_balance.sql")).thenReturn("debit-account");
        when(writeDao.update("debit-account", 100L, accountId, 100L)).thenReturn(1);

        boolean result = repository.debit(accountId, 100L);

        assertThat(result).isTrue();
    }

    @Test
    void debitReturnsFalseWhenNoRowsWereUpdated() {
        UUID accountId = UUID.randomUUID();
        when(sqlLoader.loadSql("sql/account/debit_balance.sql")).thenReturn("debit-account");
        when(writeDao.update("debit-account", 100L, accountId, 100L)).thenReturn(0);

        boolean result = repository.debit(accountId, 100L);

        assertThat(result).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void rowMapperMapsAccountColumns() throws Exception {
        UUID accountId = UUID.randomUUID();
        ResultSet rs = mock(ResultSet.class);
        when(sqlLoader.loadSql("sql/account/find_account.sql")).thenReturn("select-account");
        when(rs.getString("id")).thenReturn(accountId.toString());
        when(rs.getString("user_id")).thenReturn("user-1");
        when(rs.getLong("balance")).thenReturn(1_000L);
        when(rs.getString("currency")).thenReturn("INR");

        repository.findById(accountId);

        ArgumentCaptor<RowMapper<Account>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        verify(readDao).queryForObject(eq("select-account"), mapperCaptor.capture(), eq(accountId));
        Account mapped = mapperCaptor.getValue().mapRow(rs, 0);

        assertThat(mapped.getId()).isEqualTo(accountId);
        assertThat(mapped.getUserId()).isEqualTo("user-1");
        assertThat(mapped.getBalance()).isEqualTo(1_000L);
        assertThat(mapped.getCurrency()).isEqualTo("INR");
    }
}
