package com.yuno.payment.repository;

import com.yuno.payment.dao.ReadDao;
import com.yuno.payment.dao.WriteDao;
import com.yuno.payment.model.Payment;
import com.yuno.payment.model.enums.PaymentMethod;
import com.yuno.payment.model.enums.PaymentStatus;
import com.yuno.payment.model.enums.Provider;
import com.yuno.payment.util.SqlLoader;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentRepositoryImplTest {

    private final SqlLoader sqlLoader = mock(SqlLoader.class);
    private final ReadDao readDao = mock(ReadDao.class);
    private final WriteDao writeDao = mock(WriteDao.class);
    private final PaymentRepositoryImpl repository = new PaymentRepositoryImpl(sqlLoader, readDao, writeDao);

    @Test
    void saveLoadsInsertSqlAndWritesPaymentFields() {
        Payment payment = payment();
        when(sqlLoader.loadSql("sql/payments/insert_payment.sql")).thenReturn("insert-payment");

        repository.save(payment);

        verify(writeDao).update(
                "insert-payment",
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

    @Test
    void findByIdempotencyKeyUsesExpectedSqlPath() {
        Payment payment = payment();
        when(sqlLoader.loadSql("sql/payments/find_by_idempotency.sql")).thenReturn("select-by-key");
        when(readDao.queryForObject(eq("select-by-key"), any(), eq("key-1"))).thenReturn(payment);

        Payment result = repository.findByIdempotencyKey("key-1");

        assertThat(result).isSameAs(payment);
    }

    @Test
    void findByIdUsesExpectedSqlPath() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder().id(paymentId).build();
        when(sqlLoader.loadSql("sql/payments/find_by_id.sql")).thenReturn("select-by-id");
        when(readDao.queryForObject(eq("select-by-id"), any(), eq(paymentId))).thenReturn(payment);

        Payment result = repository.findById(paymentId);

        assertThat(result).isSameAs(payment);
    }

    @Test
    void updateLoadsUpdateSqlAndWritesMutableFields() {
        Payment payment = payment();
        payment.setProvider(Provider.PROVIDER_A);
        payment.setTransactionId("txn-1");
        when(sqlLoader.loadSql("sql/payments/update_payment.sql")).thenReturn("update-payment");

        repository.update(payment);

        verify(writeDao).update(
                "update-payment",
                payment.getStatus().name(),
                "PROVIDER_A",
                payment.getTransactionId(),
                payment.getUpdatedAt(),
                payment.getId()
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void rowMapperMapsPaymentColumns() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID fromAccountId = UUID.randomUUID();
        UUID toAccountId = UUID.randomUUID();
        ResultSet rs = mock(ResultSet.class);
        when(sqlLoader.loadSql("sql/payments/find_by_id.sql")).thenReturn("select-by-id");
        when(rs.getString("id")).thenReturn(paymentId.toString());
        when(rs.getString("idempotency_key")).thenReturn("key-1");
        when(rs.getLong("amount")).thenReturn(100L);
        when(rs.getString("currency")).thenReturn("INR");
        when(rs.getString("method")).thenReturn("CARD");
        when(rs.getString("status")).thenReturn("SUCCESS");
        when(rs.getString("from_account_id")).thenReturn(fromAccountId.toString());
        when(rs.getString("to_account_id")).thenReturn(toAccountId.toString());
        when(rs.getString("transaction_id")).thenReturn("txn-1");

        repository.findById(paymentId);

        ArgumentCaptor<RowMapper<Payment>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        verify(readDao).queryForObject(eq("select-by-id"), mapperCaptor.capture(), eq(paymentId));
        Payment mapped = mapperCaptor.getValue().mapRow(rs, 0);

        assertThat(mapped.getId()).isEqualTo(paymentId);
        assertThat(mapped.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(mapped.getAmount()).isEqualTo(100L);
        assertThat(mapped.getCurrency()).isEqualTo("INR");
        assertThat(mapped.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(mapped.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(mapped.getFromAccountId()).isEqualTo(fromAccountId);
        assertThat(mapped.getToAccountId()).isEqualTo(toAccountId);
        assertThat(mapped.getTransactionId()).isEqualTo("txn-1");
    }

    private Payment payment() {
        return Payment.builder()
                .id(UUID.randomUUID())
                .idempotencyKey("key-1")
                .amount(100L)
                .currency("INR")
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.PROCESSING)
                .fromAccountId(UUID.randomUUID())
                .toAccountId(UUID.randomUUID())
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .updatedAt(new Timestamp(System.currentTimeMillis()))
                .build();
    }
}
