package com.eventseat.order.repository;

import com.eventseat.order.domain.IdempotencyKeyEntity;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class IdempotencyKeyJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public IdempotencyKeyJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Timestamp toTs(OffsetDateTime odt) {
        return odt == null ? null : Timestamp.from(odt.toInstant());
    }

    public boolean tryInsert(String key, String requestHash) {
        final String sql = "INSERT INTO idempotency_keys (`key`, request_hash, created_at) VALUES (?,?,?)";
        try {
            jdbcTemplate.update(sql, key, requestHash, toTs(OffsetDateTime.now()));
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    public void storeResponse(String key, Long orderId, String responseJson) {
        final String sql = "UPDATE idempotency_keys SET order_id=?, response_json=? WHERE `key`=?";
        jdbcTemplate.update(sql, orderId, responseJson, key);
    }

    public Optional<IdempotencyKeyEntity> findByKey(String key) {
        final String sql = "SELECT `key`, request_hash, response_json, order_id, created_at FROM idempotency_keys WHERE `key`=?";
        var list = jdbcTemplate.query(sql, (rs, rn) -> {
            IdempotencyKeyEntity e = new IdempotencyKeyEntity();
            e.setKey(rs.getString("key"));
            e.setRequestHash(rs.getString("request_hash"));
            e.setResponseJson(rs.getString("response_json"));
            long oid = rs.getLong("order_id");
            e.setOrderId(rs.wasNull() ? null : oid);
            var cat = rs.getTimestamp("created_at");
            e.setCreatedAt(cat == null ? null : cat.toInstant().atOffset(ZoneOffset.UTC));
            return e;
        }, key);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
