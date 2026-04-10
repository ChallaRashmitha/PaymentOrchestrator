package com.yuno.payment.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlLoaderTest {

    private final SqlLoader sqlLoader = new SqlLoader();

    @Test
    void loadSqlReadsSqlFromTemplatesClasspathDirectory() {
        String sql = sqlLoader.loadSql("sql/test/test.sql");

        assertThat(sql).contains("SELECT 1");
    }

    @Test
    void loadSqlThrowsClearExceptionWhenResourceIsMissing() {
        assertThatThrownBy(() -> sqlLoader.loadSql("sql/missing.sql"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to load SQL: sql/missing.sql");
    }
}
