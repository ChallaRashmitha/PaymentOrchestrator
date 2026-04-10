package com.yuno.payment.dao;

import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReadDaoTest {

    private final ReadDao readDao = createReadDaoWithMockedQuery();

    private ReadDao createReadDaoWithMockedQuery() {
        return new ReadDao() {
            private ReadDao delegate = mock(ReadDao.class);

            @Override
            public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
                return delegate.query(sql, mapper, params);
            }

            public void setupQuery(String sql, RowMapper<?> mapper, Object[] params, List<?> result) {
                when(delegate.query(sql, mapper, params)).thenReturn((List) result);
            }

            public void setupQueryToThrow(String sql, RowMapper<?> mapper, Object[] params, RuntimeException exception) {
                when(delegate.query(sql, mapper, params)).thenThrow(exception);
            }
        };
    }

    @Test
    void queryForObjectReturnsFirstResultWhenListIsNotEmpty() {
        String sql = "select name from users";
        RowMapper<String> mapper = (rs, rowNum) -> rs.getString("name");
        List<String> results = List.of("John", "Jane");
        Object[] params = {"id-1"};

        ReadDao readDaoImpl = new ReadDao() {
            @Override
            public <T> List<T> query(String s, RowMapper<T> rowMapper, Object... objects) {
                return (List<T>) results;
            }
        };

        String result = readDaoImpl.queryForObject(sql, mapper, "id-1");

        assertThat(result).isEqualTo("John");
    }

    @Test
    void queryForObjectThrowsEmptyResultDataAccessExceptionWhenListIsEmpty() {
        String sql = "select name from users";
        RowMapper<String> mapper = (rs, rowNum) -> rs.getString("name");

        ReadDao readDaoImpl = new ReadDao() {
            @Override
            public <T> List<T> query(String s, RowMapper<T> rowMapper, Object... objects) {
                return List.of();
            }
        };

        assertThatThrownBy(() -> readDaoImpl.queryForObject(sql, mapper, "id-999"))
                .isInstanceOf(EmptyResultDataAccessException.class);
    }

    @Test
    void queryForObjectThrowsEmptyResultDataAccessExceptionWhenListHasNoElements() {
        String sql = "select id from accounts where id = ?";
        RowMapper<Long> mapper = (rs, rowNum) -> rs.getLong("id");

        ReadDao readDaoImpl = new ReadDao() {
            @Override
            public <T> List<T> query(String s, RowMapper<T> rowMapper, Object... objects) {
                return List.of();
            }
        };

        assertThatThrownBy(() -> readDaoImpl.queryForObject(sql, mapper, "non-existent"))
                .isInstanceOf(EmptyResultDataAccessException.class)
                .hasMessage("Incorrect result size: expected 1, actual 0");
    }

    @Test
    void queryForObjectPropagatesExceptionFromQuery() {
        String sql = "select * from payments";
        RowMapper<String> mapper = (rs, rowNum) -> rs.getString("id");

        ReadDao readDaoImpl = new ReadDao() {
            @Override
            public <T> List<T> query(String s, RowMapper<T> rowMapper, Object... objects) {
                throw new RuntimeException("DB Error");
            }
        };

        assertThatThrownBy(() -> readDaoImpl.queryForObject(sql, mapper, "param"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB Error");
    }
}
