package com.yuno.payment.dao;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcDaoTest {

    @Test
    void jdbcReadDaoDelegatesQueryForObjectToJdbcTemplate() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        JdbcReadDao dao = new JdbcReadDao(jdbcTemplate);
        RowMapper<String> mapper = (rs, rowNum) -> rs.getString("name");

        when(jdbcTemplate.queryForObject("select", mapper, "id-1")).thenReturn("value");

        String result = dao.queryForObject("select", mapper, "id-1");

        assertThat(result).isEqualTo("value");
        verify(jdbcTemplate).queryForObject("select", mapper, "id-1");
    }

    @Test
    void jdbcWriteDaoDelegatesUpdateToJdbcTemplate() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        JdbcWriteDao dao = new JdbcWriteDao(jdbcTemplate);

        when(jdbcTemplate.update("update", "param")).thenReturn(1);

        int rows = dao.update("update", "param");

        assertThat(rows).isEqualTo(1);
        verify(jdbcTemplate).update("update", "param");
    }
}
