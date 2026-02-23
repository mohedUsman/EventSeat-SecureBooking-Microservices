package com.eventseat.catalog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Ensures idempotency table for inventory import exists.
 */
@Configuration
public class ImportSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(ImportSchemaInitializer.class);

    @Bean
    ApplicationRunner ensureImportIdempotency(JdbcTemplate jdbcTemplate) {
        return args -> {
            String ddl = """
                    CREATE TABLE IF NOT EXISTS idempotency_imports (
                      `key` VARCHAR(128) NOT NULL,
                      request_hash VARCHAR(256) NOT NULL,
                      response_json MEDIUMTEXT NULL,
                      created_at DATETIME NOT NULL,
                      PRIMARY KEY (`key`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """;
            jdbcTemplate.execute(ddl);
            log.info("Ensured table 'idempotency_imports' exists for catalog-service.");
        };
    }
}
