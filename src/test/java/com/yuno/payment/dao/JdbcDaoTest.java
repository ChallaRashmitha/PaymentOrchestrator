package com.yuno.payment.dao;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcDaoTest {

    @Test
    void jdbcReadDaoDelegatesQueryToJdbcTemplate() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        JdbcReadDao dao = new JdbcReadDao(jdbcTemplate);
        RowMapper<String> mapper = (rs, rowNum) -> rs.getString("name");

        when(jdbcTemplate.query("select", mapper, "id-1")).thenReturn(List.of("value"));

        List<String> result = dao.query("select", mapper, "id-1");

        assertThat(result).containsExactly("value");
        verify(jdbcTemplate).query("select", mapper, "id-1");
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
