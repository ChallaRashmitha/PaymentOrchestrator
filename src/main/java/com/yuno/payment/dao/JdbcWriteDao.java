package com.yuno.payment.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcWriteDao implements WriteDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcWriteDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int update(String sql, Object... params) {
        return jdbcTemplate.update(sql, params);
    }
}