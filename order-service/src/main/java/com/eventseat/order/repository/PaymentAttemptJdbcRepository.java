package com.eventseat.order.repository;

import com.eventseat.order.domain.PaymentAttemptEntity;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentAttemptJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public PaymentAttemptJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Timestamp toTs(OffsetDateTime odt) {
        return odt == null ? null : Timestamp.from(odt.toInstant());
    }

    public Long insertAttempt(Long orderId, PaymentAttemptEntity.Type type,
            PaymentAttemptEntity.Status status, String reason) {
        final String sql = "INSERT INTO payment_attempts (order_id, type, status, reason, created_at) "
                + "VALUES (?,?,?,?,?)";
        KeyHolder kh = new GeneratedKeyHolder();
        OffsetDateTime now = OffsetDateTime.now();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, orderId);
            ps.setString(2, type.name());
            ps.setString(3, status.name());
            ps.setString(4, reason);
            ps.setTimestamp(5, toTs(now));
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }
}
