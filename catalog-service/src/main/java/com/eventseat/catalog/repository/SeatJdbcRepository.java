package com.eventseat.catalog.repository;

import com.eventseat.catalog.web.dto.SeatDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.StringJoiner;

@Repository
public class SeatJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public SeatJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SeatDto save(SeatDto dto) {
        final String sql = "INSERT INTO seats (event_id, section, row_label, seat_number, base_price, currency, status) "
                +
                "VALUES (?,?,?,?,?,?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, dto.getEventId());
            ps.setString(2, dto.getSection());
            ps.setString(3, dto.getRowLabel());
            ps.setString(4, dto.getSeatNumber());
            ps.setBigDecimal(5, dto.getBasePrice() == null ? BigDecimal.ZERO : dto.getBasePrice());
            ps.setString(6, dto.getCurrency());
            ps.setString(7, dto.getStatus());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        dto.setId(key != null ? key.longValue() : null);
        return dto;
    }

    public int update(Long id, SeatDto dto) {
        final String sql = "UPDATE seats SET event_id=?, section=?, row_label=?, seat_number=?, base_price=?, currency=?, status=? WHERE id=?";
        return jdbcTemplate.update(sql,
                dto.getEventId(),
                dto.getSection(),
                dto.getRowLabel(),
                dto.getSeatNumber(),
                dto.getBasePrice(),
                dto.getCurrency(),
                dto.getStatus(),
                id);
    }

    public Optional<SeatDto> findById(Long id) {
        final String sql = "SELECT id, event_id, section, row_label, seat_number, base_price, currency, status FROM seats WHERE id=?";
        try {
            SeatDto s = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                SeatDto dto = new SeatDto();
                dto.setId(rs.getLong("id"));
                dto.setEventId(rs.getLong("event_id"));
                dto.setSection(rs.getString("section"));
                dto.setRowLabel(rs.getString("row_label"));
                dto.setSeatNumber(rs.getString("seat_number"));
                dto.setBasePrice(rs.getBigDecimal("base_price"));
                dto.setCurrency(rs.getString("currency"));
                dto.setStatus(rs.getString("status"));
                return dto;
            }, id);
            return Optional.ofNullable(s);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<SeatDto> findAll(int page, int size) {
        final String sql = "SELECT id, event_id, section, row_label, seat_number, base_price, currency, status " +
                "FROM seats ORDER BY id DESC LIMIT ? OFFSET ?";
        int safeSize = Math.max(1, size);
        int safePage = Math.max(0, page);
        int offset = safePage * safeSize;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            SeatDto dto = new SeatDto();
            dto.setId(rs.getLong("id"));
            dto.setEventId(rs.getLong("event_id"));
            dto.setSection(rs.getString("section"));
            dto.setRowLabel(rs.getString("row_label"));
            dto.setSeatNumber(rs.getString("seat_number"));
            dto.setBasePrice(rs.getBigDecimal("base_price"));
            dto.setCurrency(rs.getString("currency"));
            dto.setStatus(rs.getString("status"));
            return dto;
        }, safeSize, offset);
    }

    public List<SeatDto> findAllByEventId(Long eventId, int page, int size) {
        final String sql = "SELECT id, event_id, section, row_label, seat_number, base_price, currency, status " +
                "FROM seats WHERE event_id=? ORDER BY id DESC LIMIT ? OFFSET ?";
        int safeSize = Math.max(1, size);
        int safePage = Math.max(0, page);
        int offset = safePage * safeSize;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            SeatDto dto = new SeatDto();
            dto.setId(rs.getLong("id"));
            dto.setEventId(rs.getLong("event_id"));
            dto.setSection(rs.getString("section"));
            dto.setRowLabel(rs.getString("row_label"));
            dto.setSeatNumber(rs.getString("seat_number"));
            dto.setBasePrice(rs.getBigDecimal("base_price"));
            dto.setCurrency(rs.getString("currency"));
            dto.setStatus(rs.getString("status"));
            return dto;
        }, eventId, safeSize, offset);
    }

    public int delete(Long id) {
        final String sql = "DELETE FROM seats WHERE id=?";
        return jdbcTemplate.update(sql, id);
    }

    public boolean existsById(Long id) {
        final String sql = "SELECT COUNT(1) FROM seats WHERE id=?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    /**
     * Returns a map of seatId -> status for seats that match the given eventId and
     * ids.
     * Any requested id not present in the result is either non-existent or does not
     * belong to the eventId.
     */
    public Map<Long, String> findStatusesForEventAndIds(Long eventId, List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty())
            return Map.of();
        StringJoiner sj = new StringJoiner(",", "(", ")");
        for (int i = 0; i < seatIds.size(); i++)
            sj.add("?");
        String sql = "SELECT id, status FROM seats WHERE event_id=? AND id IN " + sj;
        List<Object> args = new ArrayList<>();
        args.add(eventId);
        args.addAll(seatIds);
        Map<Long, String> out = new HashMap<>();
        jdbcTemplate.query(sql, args.toArray(), rs -> {
            out.put(rs.getLong("id"), rs.getString("status"));
        });
        return out;
    }

    public Long findIdByNaturalKey(Long eventId, String section, String rowLabel, String seatNumber) {
        final String sql = "SELECT id FROM seats WHERE event_id=? AND " +
                "COALESCE(section,'') = COALESCE(?, '') AND COALESCE(row_label,'') = COALESCE(?, '') AND COALESCE(seat_number,'') = COALESCE(?, '') LIMIT 1";
        List<Long> ids = jdbcTemplate.query(sql, (rs, rn) -> rs.getLong("id"),
                eventId, section, rowLabel, seatNumber);
        return ids.isEmpty() ? null : ids.get(0);
    }

    public Long upsertByNaturalKey(Long eventId, String section, String rowLabel, String seatNumber,
            java.math.BigDecimal basePrice, String currency) {
        Long existingId = findIdByNaturalKey(eventId, section, rowLabel, seatNumber);
        com.eventseat.catalog.web.dto.SeatDto dto = new com.eventseat.catalog.web.dto.SeatDto();
        dto.setEventId(eventId);
        dto.setSection(section);
        dto.setRowLabel(rowLabel);
        dto.setSeatNumber(seatNumber);
        dto.setBasePrice(basePrice);
        dto.setCurrency(currency);
        dto.setStatus("AVAILABLE");
        if (existingId == null) {
            com.eventseat.catalog.web.dto.SeatDto saved = save(dto);
            return saved.getId();
        } else {
            update(existingId, dto);
            return existingId;
        }
    }
}
