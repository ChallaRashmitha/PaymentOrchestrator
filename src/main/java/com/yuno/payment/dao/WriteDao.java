package com.yuno.payment.dao;

public interface WriteDao {
    int update(String sql, Object... params);
}