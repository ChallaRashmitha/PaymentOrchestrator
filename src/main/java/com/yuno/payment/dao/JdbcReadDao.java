package com.yuno.payment.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcReadDao implements ReadDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcReadDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public <T> T queryForObject(String sql, RowMapper<T> mapper, Object... params) {
        return jdbcTemplate.queryForObject(sql, mapper, params);
    }
}