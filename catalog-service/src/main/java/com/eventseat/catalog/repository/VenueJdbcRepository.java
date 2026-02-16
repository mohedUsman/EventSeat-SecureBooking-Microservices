package com.eventseat.catalog.repository;

import com.eventseat.catalog.web.dto.VenueDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class VenueJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public VenueJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public VenueDto save(VenueDto dto) {
        final String sql = "INSERT INTO venues (name, address, city, timezone) VALUES (?,?,?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, dto.getName());
            ps.setString(2, dto.getAddress());
            ps.setString(3, dto.getCity());
            ps.setString(4, dto.getTimezone());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        dto.setId(key != null ? key.longValue() : null);
        return dto;
    }

    public int update(Long id, VenueDto dto) {
        final String sql = "UPDATE venues SET name=?, address=?, city=?, timezone=? WHERE id=?";
        return jdbcTemplate.update(sql, dto.getName(), dto.getAddress(), dto.getCity(), dto.getTimezone(), id);
    }

    public Optional<VenueDto> findById(Long id) {
        final String sql = "SELECT id, name, address, city, timezone FROM venues WHERE id=?";
        try {
            VenueDto v = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                VenueDto dto = new VenueDto();
                dto.setId(rs.getLong("id"));
                dto.setName(rs.getString("name"));
                dto.setAddress(rs.getString("address"));
                dto.setCity(rs.getString("city"));
                dto.setTimezone(rs.getString("timezone"));
                return dto;
            }, id);
            return Optional.ofNullable(v);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<VenueDto> findAll(int page, int size) {
        final String sql = "SELECT id, name, address, city, timezone FROM venues ORDER BY id DESC LIMIT ? OFFSET ?";
        int offset = Math.max(0, page) * Math.max(1, size);
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            VenueDto dto = new VenueDto();
            dto.setId(rs.getLong("id"));
            dto.setName(rs.getString("name"));
            dto.setAddress(rs.getString("address"));
            dto.setCity(rs.getString("city"));
            dto.setTimezone(rs.getString("timezone"));
            return dto;
        }, size, offset);
    }

    public int delete(Long id) {
        final String sql = "DELETE FROM venues WHERE id=?";
        return jdbcTemplate.update(sql, id);
    }

    public boolean existsById(Long id) {
        final String sql = "SELECT COUNT(1) FROM venues WHERE id=?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}
