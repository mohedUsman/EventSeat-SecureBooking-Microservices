package com.eventseat.catalog.repository;

import com.eventseat.catalog.web.dto.EventDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class EventJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public EventJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Timestamp toTimestamp(OffsetDateTime odt) {
        return odt == null ? null : Timestamp.from(odt.toInstant());
    }

    private OffsetDateTime fromTimestamp(Timestamp ts) {
        return ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC);
    }

    public EventDto save(EventDto dto) {
        final String sql = "INSERT INTO events (organizer_id, venue_id, title, category, start_date_time, end_date_time, status) "
                +
                "VALUES (?,?,?,?,?,?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, dto.getOrganizerId());
            ps.setLong(2, dto.getVenueId());
            ps.setString(3, dto.getTitle());
            ps.setString(4, dto.getCategory());
            ps.setTimestamp(5, toTimestamp(dto.getStartDateTime()));
            ps.setTimestamp(6, toTimestamp(dto.getEndDateTime()));
            ps.setString(7, dto.getStatus());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        dto.setId(key != null ? key.longValue() : null);
        return dto;
    }

    public int update(Long id, EventDto dto) {
        final String sql = "UPDATE events SET organizer_id=?, venue_id=?, title=?, category=?, start_date_time=?, end_date_time=?, status=? "
                +
                "WHERE id=?";
        return jdbcTemplate.update(sql,
                dto.getOrganizerId(),
                dto.getVenueId(),
                dto.getTitle(),
                dto.getCategory(),
                toTimestamp(dto.getStartDateTime()),
                toTimestamp(dto.getEndDateTime()),
                dto.getStatus(),
                id);
    }

    public Optional<EventDto> findById(Long id) {
        final String sql = "SELECT id, organizer_id, venue_id, title, category, start_date_time, end_date_time, status FROM events WHERE id=?";
        try {
            EventDto v = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                EventDto dto = new EventDto();
                dto.setId(rs.getLong("id"));
                dto.setOrganizerId(rs.getLong("organizer_id"));
                dto.setVenueId(rs.getLong("venue_id"));
                dto.setTitle(rs.getString("title"));
                dto.setCategory(rs.getString("category"));
                dto.setStartDateTime(fromTimestamp(rs.getTimestamp("start_date_time")));
                dto.setEndDateTime(fromTimestamp(rs.getTimestamp("end_date_time")));
                dto.setStatus(rs.getString("status"));
                return dto;
            }, id);
            return Optional.ofNullable(v);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<EventDto> findAll(int page, int size) {
        final String sql = "SELECT id, organizer_id, venue_id, title, category, start_date_time, end_date_time, status "
                +
                "FROM events ORDER BY id DESC LIMIT ? OFFSET ?";
        int safeSize = Math.max(1, size);
        int safePage = Math.max(0, page);
        int offset = safePage * safeSize;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            EventDto dto = new EventDto();
            dto.setId(rs.getLong("id"));
            dto.setOrganizerId(rs.getLong("organizer_id"));
            dto.setVenueId(rs.getLong("venue_id"));
            dto.setTitle(rs.getString("title"));
            dto.setCategory(rs.getString("category"));
            dto.setStartDateTime(fromTimestamp(rs.getTimestamp("start_date_time")));
            dto.setEndDateTime(fromTimestamp(rs.getTimestamp("end_date_time")));
            dto.setStatus(rs.getString("status"));
            return dto;
        }, safeSize, offset);
    }

    public int delete(Long id) {
        final String sql = "DELETE FROM events WHERE id=?";
        return jdbcTemplate.update(sql, id);
    }

    public boolean existsById(Long id) {
        final String sql = "SELECT COUNT(1) FROM events WHERE id=?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}
