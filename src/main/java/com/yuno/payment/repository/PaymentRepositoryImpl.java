package com.yuno.payment.repository;

import com.yuno.payment.dao.ReadDao;
import com.yuno.payment.dao.WriteDao;
import com.yuno.payment.model.Payment;
import com.yuno.payment.model.enums.PaymentMethod;
import com.yuno.payment.model.enums.PaymentStatus;
import com.yuno.payment.util.SqlLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final SqlLoader sqlLoader;
    private final ReadDao readDao;
    private final WriteDao writeDao;

    private final RowMapper<Payment> mapper = (rs, rowNum) ->
            Payment.builder()
                    .id(UUID.fromString(rs.getString("id")))
                    .idempotencyKey(rs.getString("idempotency_key"))
                    .amount(rs.getLong("amount"))
                    .currency(rs.getString("currency"))
                    .method(PaymentMethod.valueOf(rs.getString("method")))
                    .status(PaymentStatus.valueOf(rs.getString("status")))
                    .fromAccountId(UUID.fromString(rs.getString("from_account_id")))
                    .toAccountId(UUID.fromString(rs.getString("to_account_id")))
                    .transactionId(rs.getString("transaction_id"))
                    .build();

    @Override
    public void save(Payment payment) {
        String sql = sqlLoader.loadSql("sql/payments/insert_payment.sql");
        writeDao.update(sql,
                payment.getId(),
                payment.getIdempotencyKey(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMethod().name(),
                payment.getStatus().name(),
                payment.getFromAccountId(),
                payment.getToAccountId(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    @Override
    public Payment findByIdempotencyKey(String key) {
        String sql = sqlLoader.loadSql("sql/payments/find_by_idempotency.sql");
        return readDao.queryForObject(sql, mapper, key);
    }

    @Override
    public Payment findById(UUID id) {
        String sql = sqlLoader.loadSql("sql/payments/find_by_id.sql");
        return readDao.queryForObject(sql, mapper, id);
    }

    @Override
    public void update(Payment payment) {
        String sql = sqlLoader.loadSql("sql/payments/update_payment.sql");
        writeDao.update(sql,
                payment.getStatus().name(),
                payment.getProvider() != null ? payment.getProvider().name() : null,
                payment.getTransactionId(),
                payment.getUpdatedAt(),
                payment.getId()
        );
    }
}
