package com.eventseat.catalog.repository;

import com.eventseat.catalog.web.dto.EventSearchItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Repository
public class EventSearchRepository {

    private final JdbcTemplate jdbcTemplate;

    public EventSearchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Timestamp toTs(OffsetDateTime odt) {
        return odt == null ? null : Timestamp.from(odt.toInstant());
    }

    public List<EventSearchItem> search(
            int page,
            int size,
            String city,
            String category,
            Long organizerId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            OffsetDateTime startFrom,
            OffsetDateTime endUntil) {
        StringBuilder sql = new StringBuilder();
        List<Object> args = new ArrayList<>();

        sql.append(
                "SELECT e.id, e.organizer_id, e.venue_id, e.title, e.category, e.start_date_time, e.end_date_time, e.status, "
                        +
                        "       v.city, " +
                        "       (SELECT COUNT(1) FROM seats s2 WHERE s2.event_id = e.id AND s2.status = 'AVAILABLE') AS available_seats_remaining "
                        +
                        "FROM events e " +
                        "JOIN venues v ON v.id = e.venue_id " +
                        "WHERE 1=1 ");

        if (city != null && !city.isBlank()) {
            sql.append(" AND v.city = ? ");
            args.add(city);
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND e.category = ? ");
            args.add(category);
        }
        if (organizerId != null) {
            sql.append(" AND e.organizer_id = ? ");
            args.add(organizerId);
        }
        if (minPrice != null && maxPrice != null) {
            // Event qualifies if it has ANY seat priced within the range
            sql.append(
                    " AND EXISTS (SELECT 1 FROM seats s3 WHERE s3.event_id = e.id AND s3.base_price BETWEEN ? AND ?) ");
            args.add(minPrice);
            args.add(maxPrice);
        } else if (minPrice != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM seats s3 WHERE s3.event_id = e.id AND s3.base_price >= ?) ");
            args.add(minPrice);
        } else if (maxPrice != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM seats s3 WHERE s3.event_id = e.id AND s3.base_price <= ?) ");
            args.add(maxPrice);
        }
        if (startFrom != null) {
            sql.append(" AND e.start_date_time >= ? ");
            args.add(toTs(startFrom));
        }
        if (endUntil != null) {
            sql.append(" AND e.end_date_time <= ? ");
            args.add(toTs(endUntil));
        }

        sql.append(" ORDER BY e.start_date_time ASC ");
        int safeSize = Math.max(1, size);
        int safePage = Math.max(0, page);
        int offset = safePage * safeSize;
        sql.append(" LIMIT ? OFFSET ? ");
        args.add(safeSize);
        args.add(offset);

        return jdbcTemplate.query(sql.toString(), args.toArray(), (rs, rowNum) -> {
            EventSearchItem item = new EventSearchItem();
            item.setId(rs.getLong("id"));
            item.setOrganizerId(rs.getLong("organizer_id"));
            item.setVenueId(rs.getLong("venue_id"));
            item.setTitle(rs.getString("title"));
            item.setCategory(rs.getString("category"));
            Timestamp sTs = rs.getTimestamp("start_date_time");
            Timestamp eTs = rs.getTimestamp("end_date_time");
            item.setStartDateTime(sTs == null ? null : sTs.toInstant().atOffset(ZoneOffset.UTC));
            item.setEndDateTime(eTs == null ? null : eTs.toInstant().atOffset(ZoneOffset.UTC));
            item.setStatus(rs.getString("status"));
            item.setCity(rs.getString("city"));
            item.setAvailableSeatsRemaining(rs.getLong("available_seats_remaining"));
            return item;
        });
    }
}
