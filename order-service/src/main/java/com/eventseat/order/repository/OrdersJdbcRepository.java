package com.eventseat.order.repository;

import com.eventseat.order.domain.OrderEntity;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class OrdersJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrdersJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Timestamp toTs(OffsetDateTime odt) {
        return odt == null ? null : Timestamp.from(odt.toInstant());
    }

    private OffsetDateTime fromTs(Timestamp ts) {
        return ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC);
    }

    public Long insertPending(Long attendeeId, Long eventId, BigDecimal amount, String currency, String seatIdsCsv) {
        final String sql = "INSERT INTO orders (attendee_id, event_id, amount, currency, seat_ids_csv, state, created_at, updated_at) "
                + "VALUES (?,?,?,?,?,?,?,?)";
        KeyHolder kh = new GeneratedKeyHolder();
        OffsetDateTime now = OffsetDateTime.now();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, attendeeId);
            ps.setLong(2, eventId);
            ps.setBigDecimal(3, amount);
            ps.setString(4, currency);
            ps.setString(5, seatIdsCsv);
            ps.setString(6, OrderEntity.State.PENDING.name());
            ps.setTimestamp(7, toTs(now));
            ps.setTimestamp(8, toTs(now));
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? null : key.longValue();
    }

    public int updateState(Long id, OrderEntity.State state) {
        final String sql = "UPDATE orders SET state=?, updated_at=? WHERE id=?";
        return jdbcTemplate.update(sql, state.name(), toTs(OffsetDateTime.now()), id);
    }

    public Optional<OrderEntity> findById(Long id) {
        final String sql = "SELECT id, attendee_id, event_id, amount, currency, seat_ids_csv, state, created_at, updated_at "
                + "FROM orders WHERE id=?";
        try {
            OrderEntity o = jdbcTemplate.queryForObject(sql, (rs, rn) -> {
                OrderEntity e = new OrderEntity();
                try {
                    var f = OrderEntity.class.getDeclaredField("id");
                    f.setAccessible(true);
                    f.set(e, rs.getLong("id"));
                } catch (Exception ignore) {
                }
                e.setAttendeeId(rs.getLong("attendee_id"));
                e.setEventId(rs.getLong("event_id"));
                e.setAmount(rs.getBigDecimal("amount"));
                e.setCurrency(rs.getString("currency"));
                e.setSeatIdsCsv(rs.getString("seat_ids_csv"));
                e.setState(OrderEntity.State.valueOf(rs.getString("state")));
                e.setCreatedAt(fromTs(rs.getTimestamp("created_at")));
                e.setUpdatedAt(fromTs(rs.getTimestamp("updated_at")));
                return e;
            }, id);
            return Optional.ofNullable(o);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }
}
