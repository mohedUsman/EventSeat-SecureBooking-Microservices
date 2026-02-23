package com.eventseat.catalog.repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Simple idempotency store for bulk inventory import.
 * Keyed by 'Idempotency-Key' header. Caches a responseJson so replays return
 * the same report.
 */
@Repository
public class IdempotencyImportJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public IdempotencyImportJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public static class ImportCacheRow {
        public String key;
        public String requestHash;
        public String responseJson;
        public OffsetDateTime createdAt;
    }

    public ImportCacheRow findByKey(String key) {
        final String sql = "SELECT `key`, request_hash, response_json, created_at FROM idempotency_imports WHERE `key`=?";
        return jdbcTemplate.query(sql, rs -> {
            if (!rs.next())
                return null;
            ImportCacheRow row = new ImportCacheRow();
            row.key = rs.getString("key");
            row.requestHash = rs.getString("request_hash");
            row.responseJson = rs.getString("response_json");
            var ts = rs.getTimestamp("created_at");
            row.createdAt = ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC);
            return row;
        }, key);
    }

    public int insert(String key, String requestHash, String responseJson) {
        final String sql = "INSERT INTO idempotency_imports (`key`, request_hash, response_json, created_at) VALUES (?,?,?,?)";
        return jdbcTemplate.update(sql, key, requestHash, responseJson,
                java.sql.Timestamp.from(OffsetDateTime.now().toInstant()));
    }

    public int updateResponse(String key, String responseJson) {
        final String sql = "UPDATE idempotency_imports SET response_json=? WHERE `key`=?";
        return jdbcTemplate.update(sql, responseJson, key);
    }
}
