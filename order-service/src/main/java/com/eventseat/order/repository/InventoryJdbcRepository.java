package com.eventseat.order.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Reads cross-service inventory data (same MySQL schema "eventseat").
 * Verifies holds/seats and performs HELD -> SOLD transitions atomically.
 */
@Repository
public class InventoryJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public InventoryJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public static class HoldRow {
        public Long id;
        public Long attendeeId;
        public Long eventId;
        public String seatIdsCsv;
        public String status; // ACTIVE | EXPIRED | RELEASED
        public OffsetDateTime expiresAt;
    }

    private StringJoiner placeholders(int n) {
        StringJoiner sj = new StringJoiner(",", "(", ")");
        for (int i = 0; i < n; i++)
            sj.add("?");
        return sj;
    }

    public HoldRow getActiveHold(Long holdId) {
        final String sql = "SELECT id, attendee_id, event_id, seat_ids_csv, status, expires_at " +
                "FROM holds WHERE id=?";
        List<HoldRow> list = jdbcTemplate.query(sql, (rs, rn) -> {
            HoldRow h = new HoldRow();
            h.id = rs.getLong("id");
            h.attendeeId = rs.getLong("attendee_id");
            h.eventId = rs.getLong("event_id");
            h.seatIdsCsv = rs.getString("seat_ids_csv");
            h.status = rs.getString("status");
            Timestamp ts = rs.getTimestamp("expires_at");
            h.expiresAt = ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC);
            return h;
        }, holdId);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Returns map seatId -> basePrice for the given event and seat ids,
     * only if all seats are currently HELD. If any seat not HELD or mismatched
     * event,
     * returns a map with missing entries (caller should validate size).
     */
    public Map<Long, BigDecimal> getHeldSeatPrices(Long eventId, List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty())
            return Map.of();
        StringJoiner sj = placeholders(seatIds.size());
        String sql = "SELECT id, base_price FROM seats WHERE event_id=? AND id IN " + sj + " AND status='HELD'";
        List<Object> args = new ArrayList<>();
        args.add(eventId);
        args.addAll(seatIds);
        Map<Long, BigDecimal> out = new HashMap<>();
        jdbcTemplate.query(sql, args.toArray(), (rs) -> {
            out.put(rs.getLong("id"), rs.getBigDecimal("base_price"));
        });
        return out;
    }

    /**
     * Transitions seats from HELD to SOLD. Returns number of updated rows.
     */
    public int updateSeatsToSold(Long eventId, List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty())
            return 0;
        StringJoiner sj = placeholders(seatIds.size());
        String sql = "UPDATE seats SET status='SOLD' WHERE event_id=? AND id IN " + sj + " AND status='HELD'";
        List<Object> args = new ArrayList<>();
        args.add(eventId);
        args.addAll(seatIds);
        return jdbcTemplate.update(sql, args.toArray());
    }
}
