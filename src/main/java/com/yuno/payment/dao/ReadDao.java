package com.yuno.payment.dao;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;

public interface ReadDao {
    <T> List<T> query(String sql, RowMapper<T> mapper, Object... params);

    default <T> T queryForObject(String sql, RowMapper<T> mapper, Object... params) {
        List<T> results = query(sql, mapper, params);
        if (results.isEmpty()) {
            throw new EmptyResultDataAccessException(1);
        }
        return results.get(0);
    }
}
