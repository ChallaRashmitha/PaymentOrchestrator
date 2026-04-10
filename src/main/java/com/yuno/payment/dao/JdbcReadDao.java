package com.yuno.payment.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcReadDao implements ReadDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcReadDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        return jdbcTemplate.query(sql, mapper, params);
    }
}