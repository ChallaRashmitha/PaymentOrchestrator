package com.yuno.payment.repository;

import com.yuno.payment.dao.ReadDao;
import com.yuno.payment.dao.WriteDao;
import com.yuno.payment.model.PaymentAttempt;
import com.yuno.payment.model.enums.PaymentStatus;
import com.yuno.payment.model.enums.Provider;
import com.yuno.payment.util.SqlLoader;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class PaymentAttemptRepositoryImpl implements PaymentAttemptRepository {

    private final SqlLoader sqlLoader;
    private final WriteDao writeDao;
    private final ReadDao readDao;

    public PaymentAttemptRepositoryImpl(SqlLoader sqlLoader, WriteDao writeDao, ReadDao readDao) {
        this.sqlLoader = sqlLoader;
        this.writeDao = writeDao;
        this.readDao = readDao;
    }

    @Override
    public void save(PaymentAttempt attempt) {

        String sql = sqlLoader.loadSql("sql/payment_attempts/insert_payments.sql");

        writeDao.update(sql,
                attempt.getId(),
                attempt.getPaymentId(),
                attempt.getProvider().name(),
                attempt.getStatus().name(),
                attempt.getErrorMessage(),
                attempt.getAttemptNumber(),
                attempt.getCreatedAt()
        );
    }

    @Override
    public List<PaymentAttempt> findByPaymentId(UUID paymentId) {

        String sql = sqlLoader.loadSql("sql/payment_attempts/find_attempts_by_payment.sql");

        return readDao.query(sql, (rs, rowNum) ->
                        PaymentAttempt.builder()
                                .id(UUID.fromString(rs.getString("id")))
                                .paymentId(UUID.fromString(rs.getString("payment_id")))
                                .provider(Provider.valueOf(rs.getString("provider")))
                                .status(PaymentStatus.valueOf(rs.getString("status")))
                                .errorMessage(rs.getString("error_message"))
                                .attemptNumber(rs.getInt("attempt_number"))
                                .createdAt(rs.getTimestamp("created_at"))
                                .build(),
                paymentId
        );
    }
}
