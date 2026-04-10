package com.yuno.payment.repository;

import com.yuno.payment.dao.ReadDao;
import com.yuno.payment.dao.WriteDao;
import com.yuno.payment.util.SqlLoader;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.EmptyResultDataAccessException;
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

class IdempotencyRepositoryImplTest {

    private final SqlLoader sqlLoader = mock(SqlLoader.class);
    private final ReadDao readDao = mock(ReadDao.class);
    private final WriteDao writeDao = mock(WriteDao.class);
    private final IdempotencyRepositoryImpl repository = new IdempotencyRepositoryImpl(sqlLoader, readDao, writeDao);

    @Test
    void existsReturnsTrueWhenRecordIsFound() {
        when(sqlLoader.loadSql("sql/idempotency/find_idempotency.sql")).thenReturn("select-idempotency");
        when(readDao.queryForObject(eq("select-idempotency"), any(), eq("key-1"))).thenReturn(UUID.randomUUID());

        boolean result = repository.exists("key-1");

        assertThat(result).isTrue();
    }

    @Test
    void existsReturnsFalseWhenLookupFails() {
        when(sqlLoader.loadSql("sql/idempotency/find_idempotency.sql")).thenReturn("select-idempotency");
        when(readDao.queryForObject(eq("select-idempotency"), any(), eq("key-1")))
                .thenThrow(new EmptyResultDataAccessException(1));

        boolean result = repository.exists("key-1");

        assertThat(result).isFalse();
    }

    @Test
    void getPaymentIdMapsPaymentId() {
        UUID paymentId = UUID.randomUUID();
        when(sqlLoader.loadSql("sql/idempotency/find_idempotency.sql")).thenReturn("select-idempotency");
        when(readDao.queryForObject(eq("select-idempotency"), any(), eq("key-1"))).thenReturn(paymentId);

        UUID result = repository.getPaymentId("key-1");

        assertThat(result).isEqualTo(paymentId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPaymentIdRowMapperReadsPaymentIdColumn() throws Exception {
        UUID paymentId = UUID.randomUUID();
        ResultSet resultSet = mock(ResultSet.class);
        when(sqlLoader.loadSql("sql/idempotency/find_idempotency.sql")).thenReturn("select-idempotency");
        when(resultSet.getString("payment_id")).thenReturn(paymentId.toString());

        repository.getPaymentId("key-1");

        ArgumentCaptor<RowMapper<UUID>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
        verify(readDao).queryForObject(eq("select-idempotency"), mapperCaptor.capture(), eq("key-1"));
        UUID mappedPaymentId = mapperCaptor.getValue().mapRow(resultSet, 0);

        assertThat(mappedPaymentId).isEqualTo(paymentId);
    }

    @Test
    void saveLoadsInsertSqlAndWritesTimestampedRecord() {
        UUID paymentId = UUID.randomUUID();
        when(sqlLoader.loadSql("sql/idempotency/insert_idempotency.sql")).thenReturn("insert-idempotency");

        repository.save("key-1", paymentId);

        verify(writeDao).update(eq("insert-idempotency"), eq("key-1"), eq(paymentId), any(Timestamp.class));
    }
}
