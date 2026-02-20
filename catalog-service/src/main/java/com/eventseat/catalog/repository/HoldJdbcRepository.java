package com.eventseat.catalog.repository;

import com.eventseat.catalog.domain.HoldEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

@Repository
public class HoldJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public HoldJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Timestamp toTs(OffsetDateTime odt) {
        return odt == null ? null : Timestamp.from(odt.toInstant());
    }

    private OffsetDateTime fromTs(Timestamp ts) {
        return ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC);
    }

    public long insertActive(Long attendeeId, Long eventId, String seatIdsCsv, OffsetDateTime createdAt,
            OffsetDateTime expiresAt) {
        final String sql = "INSERT INTO holds (attendee_id, event_id, seat_ids_csv, created_at, expires_at, status) " +
                "VALUES (?,?,?,?,?,?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, attendeeId);
            ps.setLong(2, eventId);
            ps.setString(3, seatIdsCsv);
            ps.setTimestamp(4, toTs(createdAt));
            ps.setTimestamp(5, toTs(expiresAt));
            ps.setString(6, HoldEntity.Status.ACTIVE.name());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? -1L : key.longValue();
    }

    public Optional<HoldEntity> findById(Long id) {
        final String sql = "SELECT id, attendee_id, event_id, seat_ids_csv, created_at, expires_at, status FROM holds WHERE id=?";
        List<HoldEntity> list = jdbcTemplate.query(sql, (rs, rn) -> {
            HoldEntity e = new HoldEntity();
            try {
                var idCol = HoldEntity.class.getDeclaredField("id");
                idCol.setAccessible(true);
                idCol.set(e, rs.getLong("id"));
            } catch (Exception ignore) {
            }
            e.setAttendeeId(rs.getLong("attendee_id"));
            e.setEventId(rs.getLong("event_id"));
            e.setSeatIdsCsv(rs.getString("seat_ids_csv"));
            e.setCreatedAt(fromTs(rs.getTimestamp("created_at")));
            e.setExpiresAt(fromTs(rs.getTimestamp("expires_at")));
            e.setStatus(HoldEntity.Status.valueOf(rs.getString("status")));
            return e;
        }, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public int markReleased(Long id) {
        final String sql = "UPDATE holds SET status=? WHERE id=? AND status=?";
        return jdbcTemplate.update(sql, HoldEntity.Status.RELEASED.name(), id, HoldEntity.Status.ACTIVE.name());
    }

    public int markExpired(Long id) {
        final String sql = "UPDATE holds SET status=? WHERE id=? AND status=?";
        return jdbcTemplate.update(sql, HoldEntity.Status.EXPIRED.name(), id, HoldEntity.Status.ACTIVE.name());
    }

    public List<HoldEntity> findExpiredActiveHolds(OffsetDateTime now) {
        final String sql = "SELECT id, attendee_id, event_id, seat_ids_csv, created_at, expires_at, status " +
                "FROM holds WHERE status=? AND expires_at < ?";
        return jdbcTemplate.query(sql, (rs, rn) -> {
            HoldEntity e = new HoldEntity();
            try {
                var idCol = HoldEntity.class.getDeclaredField("id");
                idCol.setAccessible(true);
                idCol.set(e, rs.getLong("id"));
            } catch (Exception ignore) {
            }
            e.setAttendeeId(rs.getLong("attendee_id"));
            e.setEventId(rs.getLong("event_id"));
            e.setSeatIdsCsv(rs.getString("seat_ids_csv"));
            e.setCreatedAt(fromTs(rs.getTimestamp("created_at")));
            e.setExpiresAt(fromTs(rs.getTimestamp("expires_at")));
            e.setStatus(HoldEntity.Status.valueOf(rs.getString("status")));
            return e;
        }, HoldEntity.Status.ACTIVE.name(), toTs(now));
    }

    public int updateSeatsToHeld(Long eventId, List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty())
            return 0;
        StringJoiner sj = new StringJoiner(",", "(", ")");
        for (int i = 0; i < seatIds.size(); i++)
            sj.add("?");
        String sql = "UPDATE seats SET status='HELD' WHERE event_id=? AND id IN " + sj + " AND status='AVAILABLE'";
        List<Object> args = new ArrayList<>();
        args.add(eventId);
        args.addAll(seatIds);
        return jdbcTemplate.update(sql, args.toArray());
    }

    public int updateSeatsToAvailable(Long eventId, List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty())
            return 0;
        StringJoiner sj = new StringJoiner(",", "(", ")");
        for (int i = 0; i < seatIds.size(); i++)
            sj.add("?");
        String sql = "UPDATE seats SET status='AVAILABLE' WHERE event_id=? AND id IN " + sj + " AND status='HELD'";
        List<Object> args = new ArrayList<>();
        args.add(eventId);
        args.addAll(seatIds);
        return jdbcTemplate.update(sql, args.toArray());
    }
}
