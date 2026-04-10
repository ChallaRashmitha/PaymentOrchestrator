package com.yuno.payment.util;

import com.yuno.payment.exception.SqlLoadException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class SqlLoader {

    public String loadSql(String path) {
        ClassPathResource resource = new ClassPathResource("templates/" + path);
        try {
            return readResource(resource);
        } catch (IOException e) {
            throw new SqlLoadException("Failed to load SQL: " + path, e);
        }
    }

    private String readResource(ClassPathResource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
