package com.yuno.payment.repository;

import com.yuno.payment.dao.ReadDao;
import com.yuno.payment.dao.WriteDao;
import com.yuno.payment.model.Account;
import com.yuno.payment.util.SqlLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {

    private final SqlLoader sqlLoader;
    private final ReadDao readDao;
    private final WriteDao writeDao;

    private final RowMapper<Account> mapper = (rs, rowNum) ->
            Account.builder()
                    .id(UUID.fromString(rs.getString("id")))
                    .userId(rs.getString("user_id"))
                    .balance(rs.getLong("balance"))
                    .currency(rs.getString("currency"))
                    .build();

    @Override
    public Account findById(UUID id) {
        String sql = sqlLoader.loadSql("sql/account/find_account.sql");
        return readDao.queryForObject(sql, mapper, id);
    }

    @Override
    public void update(Account account) {
        String sql = sqlLoader.loadSql("sql/account/update_balance.sql");
        writeDao.update(sql, account.getBalance(), account.getId());
    }

    @Override
    public boolean debit(UUID accountId, long amount) {
        String sql = sqlLoader.loadSql("sql/account/debit_balance.sql");

        int rows = writeDao.update(sql, amount, accountId, amount);

        return rows > 0;
    }
}
