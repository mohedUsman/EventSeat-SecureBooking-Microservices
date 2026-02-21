package com.eventseat.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Safety net to ensure the idempotency_keys table exists before first use.
 * This complements JPA ddl-auto=update and prevents SQLSyntaxErrorException
 * when the first JDBC write happens before Hibernate creates the table.
 */
@Configuration
public class IdempotencySchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(IdempotencySchemaInitializer.class);

    @Bean
    ApplicationRunner ensureIdempotencyTable(JdbcTemplate jdbcTemplate) {
        return args -> {
            String ddl = """
                    CREATE TABLE IF NOT EXISTS idempotency_keys (
                      `key` VARCHAR(128) NOT NULL,
                      request_hash VARCHAR(256) NOT NULL,
                      response_json TEXT NULL,
                      order_id BIGINT NULL,
                      created_at DATETIME NOT NULL,
                      PRIMARY KEY (`key`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """;
            jdbcTemplate.execute(ddl);
            log.info("Ensured table 'idempotency_keys' exists (startup check completed).");
        };
    }
}
