package com.yuno.payment.repository;

import com.yuno.payment.dao.ReadDao;
import com.yuno.payment.dao.WriteDao;
import com.yuno.payment.model.PaymentAttempt;
import com.yuno.payment.model.enums.PaymentStatus;
import com.yuno.payment.model.enums.Provider;
import com.yuno.payment.util.SqlLoader;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentAttemptRepositoryImplTest {

    private final SqlLoader sqlLoader = mock(SqlLoader.class);
    private final ReadDao readDao = mock(ReadDao.class);
    private final WriteDao writeDao = mock(WriteDao.class);
    private final PaymentAttemptRepositoryImpl repository = new PaymentAttemptRepositoryImpl(sqlLoader, writeDao, readDao);

    @Test
    void saveLoadsInsertSqlAndWritesPaymentAttemptFields() {
        PaymentAttempt attempt = paymentAttempt();
        when(sqlLoader.loadSql("sql/payment_attempts/insert_payments.sql")).thenReturn("insert-attempt");

        repository.save(attempt);

        verify(writeDao).update(
                "insert-attempt",
                attempt.getId(),
                attempt.getPaymentId(),
                attempt.getProvider().name(),
                attempt.getStatus().name(),
                attempt.getErrorMessage(),
                attempt.getAttemptNumber(),
                attempt.getCreatedAt()
        );
    }

    @Test
    void saveWritesProviderNameAsString() {
        PaymentAttempt attempt = paymentAttempt();
        attempt.setProvider(Provider.PROVIDER_B);
        when(sqlLoader.loadSql("sql/payment_attempts/insert_payments.sql")).thenReturn("insert-attempt");

        repository.save(attempt);

        verify(writeDao).update(
                "insert-attempt",
                attempt.getId(),
                attempt.getPaymentId(),
                "PROVIDER_B",
                attempt.getStatus().name(),
                attempt.getErrorMessage(),
                attempt.getAttemptNumber(),
                attempt.getCreatedAt()
        );
    }

    @Test
    void saveWritesStatusNameAsString() {
        PaymentAttempt attempt = paymentAttempt();
        attempt.setStatus(PaymentStatus.FAILED);
        when(sqlLoader.loadSql("sql/payment_attempts/insert_payments.sql")).thenReturn("insert-attempt");

        repository.save(attempt);

        verify(writeDao).update(
                "insert-attempt",
                attempt.getId(),
                attempt.getPaymentId(),
                attempt.getProvider().name(),
                "FAILED",
                attempt.getErrorMessage(),
                attempt.getAttemptNumber(),
                attempt.getCreatedAt()
        );
    }

    @Test
    void findByPaymentIdLoadsCorrectSqlFile() {
        UUID paymentId = UUID.randomUUID();
        List<PaymentAttempt> attempts = List.of(paymentAttempt());
        when(sqlLoader.loadSql("sql/payment_attempts/find_attempts_by_payment.sql")).thenReturn("select-attempts");
        when(readDao.query(eq("select-attempts"), any(), eq(paymentId))).thenReturn((List) attempts);

        List<PaymentAttempt> result = repository.findByPaymentId(paymentId);

        assertThat(result).isSameAs(attempts);
        verify(sqlLoader).loadSql("sql/payment_attempts/find_attempts_by_payment.sql");
    }

    @Test
    void findByPaymentIdReturnsMultipleAttempts() {
        UUID paymentId = UUID.randomUUID();
        PaymentAttempt attempt1 = paymentAttempt();
        PaymentAttempt attempt2 = paymentAttempt();
        List<PaymentAttempt> attempts = List.of(attempt1, attempt2);
        when(sqlLoader.loadSql("sql/payment_attempts/find_attempts_by_payment.sql")).thenReturn("select-attempts");
        when(readDao.query(eq("select-attempts"), any(), eq(paymentId))).thenReturn((List) attempts);

        List<PaymentAttempt> result = repository.findByPaymentId(paymentId);

        assertThat(result).hasSize(2).containsExactly(attempt1, attempt2);
    }

    @Test
    void findByPaymentIdReturnsEmptyListWhenNoAttemptsFound() {
        UUID paymentId = UUID.randomUUID();
        when(sqlLoader.loadSql("sql/payment_attempts/find_attempts_by_payment.sql")).thenReturn("select-attempts");
        when(readDao.query(eq("select-attempts"), any(), eq(paymentId))).thenReturn((List) List.of());

        List<PaymentAttempt> result = repository.findByPaymentId(paymentId);

        assertThat(result).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void rowMapperMapsPaymentAttemptColumnsCorrectly() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        Timestamp createdAt = new Timestamp(System.currentTimeMillis());
        ResultSet rs = mock(ResultSet.class);
        when(sqlLoader.loadSql("sql/payment_attempts/find_attempts_by_payment.sql")).thenReturn("select-attempts");
        when(rs.getString("id")).thenReturn(attemptId.toString());
        when(rs.getString("payment_id")).thenReturn(paymentId.toString());
        when(rs.getString("provider")).thenReturn("PROVIDER_A");
        when(rs.getString("status")).thenReturn("SUCCESS");
        when(rs.getString("error_message")).thenReturn("No errors");
        when(rs.getInt("attempt_number")).thenReturn(1);
        when(rs.getTimestamp("created_at")).thenReturn(createdAt);

        repository.findByPaymentId(paymentId);

        ArgumentCaptor<RowMapper<PaymentAttempt>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        verify(readDao).query(eq("select-attempts"), mapperCaptor.capture(), eq(paymentId));
        PaymentAttempt mapped = mapperCaptor.getValue().mapRow(rs, 0);

        assertThat(mapped.getId()).isEqualTo(attemptId);
        assertThat(mapped.getPaymentId()).isEqualTo(paymentId);
        assertThat(mapped.getProvider()).isEqualTo(Provider.PROVIDER_A);
        assertThat(mapped.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(mapped.getErrorMessage()).isEqualTo("No errors");
        assertThat(mapped.getAttemptNumber()).isEqualTo(1);
        assertThat(mapped.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @SuppressWarnings("unchecked")
    void rowMapperMapsProviderBCorrectly() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        ResultSet rs = mock(ResultSet.class);
        when(sqlLoader.loadSql("sql/payment_attempts/find_attempts_by_payment.sql")).thenReturn("select-attempts");
        when(rs.getString("id")).thenReturn(attemptId.toString());
        when(rs.getString("payment_id")).thenReturn(paymentId.toString());
        when(rs.getString("provider")).thenReturn("PROVIDER_B");
        when(rs.getString("status")).thenReturn("FAILED");
        when(rs.getString("error_message")).thenReturn("Insufficient balance");
        when(rs.getInt("attempt_number")).thenReturn(2);
        when(rs.getTimestamp("created_at")).thenReturn(new Timestamp(System.currentTimeMillis()));

        repository.findByPaymentId(paymentId);

        ArgumentCaptor<RowMapper<PaymentAttempt>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        verify(readDao).query(eq("select-attempts"), mapperCaptor.capture(), eq(paymentId));
        PaymentAttempt mapped = mapperCaptor.getValue().mapRow(rs, 0);

        assertThat(mapped.getProvider()).isEqualTo(Provider.PROVIDER_B);
        assertThat(mapped.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(mapped.getErrorMessage()).isEqualTo("Insufficient balance");
        assertThat(mapped.getAttemptNumber()).isEqualTo(2);
    }

    private PaymentAttempt paymentAttempt() {
        return PaymentAttempt.builder()
                .id(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .provider(Provider.PROVIDER_A)
                .status(PaymentStatus.SUCCESS)
                .errorMessage(null)
                .attemptNumber(1)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();
    }
}
