package com.yuno.payment.repository;

import com.yuno.payment.util.SqlLoader;
import com.yuno.payment.dao.ReadDao;
import com.yuno.payment.dao.WriteDao;

import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.UUID;

@Repository
public class IdempotencyRepositoryImpl implements IdempotencyRepository {

    private final SqlLoader sqlLoader;
    private final ReadDao readDao;
    private final WriteDao writeDao;

    public IdempotencyRepositoryImpl(SqlLoader sqlLoader,
                                     ReadDao readDao,
                                     WriteDao writeDao) {
        this.sqlLoader = sqlLoader;
        this.readDao = readDao;
        this.writeDao = writeDao;
    }

    @Override
    public boolean exists(String key) {
        String sql = sqlLoader.loadSql("sql/idempotency/find_idempotency.sql");

        try {
            readDao.queryForObject(sql, (rs, rowNum) -> rs.getObject("payment_id"), key);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public UUID getPaymentId(String key) {
        String sql = sqlLoader.loadSql("sql/idempotency/find_idempotency.sql");

        return readDao.queryForObject(sql,
                (rs, rowNum) -> UUID.fromString(rs.getString("payment_id")),
                key
        );
    }

    @Override
    public void save(String key, UUID paymentId) {
        String sql = sqlLoader.loadSql("sql/idempotency/insert_idempotency.sql");

        writeDao.update(sql,
                key,
                paymentId,
                new Timestamp(System.currentTimeMillis())
        );
    }
}