package com.yuno.payment.dao;

import org.springframework.jdbc.core.RowMapper;

public interface ReadDao {
    <T> T queryForObject(String sql, RowMapper<T> mapper, Object... params);
}