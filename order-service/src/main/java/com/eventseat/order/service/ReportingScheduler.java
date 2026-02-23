package com.eventseat.order.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled reporting jobs:
 * - Nightly Sales Reconciliation (orders vs seat ledger by event)
 * - Monthly Event Statement (CSV export per event: orders + revenue in the
 * month)
 *
 * Notes:
 * - This is local-only tooling; safe defaults are provided for cron and output
 * dir.
 * - Uses single shared schema (eventseat) so cross-service tables are visible.
 */
@Component
public class ReportingScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReportingScheduler.class);

    private final JdbcTemplate jdbc;

    @Value("${reporting.export.dir:docs/exports}")
    private String exportDir;

    public ReportingScheduler(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Nightly at 02:30 (configurable): Compare SOLD seats vs sum of ordered seats
     * (CONFIRMED or COMPLETED) by event. Log mismatches for manual review.
     */
    @Scheduled(cron = "${reporting.reconcile.cron:0 30 2 * * *}")
    public void nightlySalesReconciliation() {
        try {
            Map<Long, Integer> soldByEvent = querySoldSeatsByEvent();
            Map<Long, Integer> orderedSeatCount = queryOrderedSeatCountByEvent();

            // Union of keys
            var eventIds = soldByEvent.keySet();
            eventIds.addAll(orderedSeatCount.keySet());

            int ok = 0, mismatches = 0;
            for (Long eventId : eventIds) {
                int sold = soldByEvent.getOrDefault(eventId, 0);
                int ordered = orderedSeatCount.getOrDefault(eventId, 0);
                if (sold == ordered) {
                    ok++;
                    log.info("[Reconcile] OK eventId={} sold={}, orderedSeats={}", eventId, sold, ordered);
                } else {
                    mismatches++;
                    log.warn("[Reconcile] MISMATCH eventId={} sold={}, orderedSeats={}, diff={}",
                            eventId, sold, ordered, (sold - ordered));
                }
            }
            log.info("[Reconcile] Completed: ok={}, mismatches={}", ok, mismatches);
        } catch (Exception ex) {
            log.error("Nightly Sales Reconciliation failed", ex);
        }
    }

    /**
     * Monthly on 1st day at 03:00 (configurable): Export per-event totals for the
     * previous month (orders count and revenue; states CONFIRMED/COMPLETED).
     * Output CSV: event-statements-YYYY-MM.csv in reporting.export.dir
     */
    @Scheduled(cron = "${reporting.monthly.cron:0 0 3 1 * *}")
    public void monthlyEventStatementExport() {
        try {
            YearMonth prev = YearMonth.now(ZoneOffset.UTC).minusMonths(1);
            LocalDate from = prev.atDay(1);
            LocalDate to = prev.atEndOfMonth();

            Timestamp fromTs = Timestamp.from(from.atStartOfDay().toInstant(ZoneOffset.UTC));
            Timestamp toTs = Timestamp.from(to.atTime(23, 59, 59).toInstant(ZoneOffset.UTC));

            String sql = """
                    SELECT event_id, COUNT(*) AS orders_cnt, COALESCE(SUM(amount),0) AS revenue
                    FROM orders
                    WHERE state IN ('CONFIRMED','COMPLETED')
                      AND created_at BETWEEN ? AND ?
                    GROUP BY event_id
                    ORDER BY event_id
                    """;

            List<Row> rows = jdbc.query(sql, (rs, rn) -> {
                Row r = new Row();
                r.eventId = rs.getLong("event_id");
                r.count = rs.getInt("orders_cnt");
                r.revenue = rs.getBigDecimal("revenue");
                return r;
            }, fromTs, toTs);

            ensureDir(exportDir);
            String fileName = "event-statements-" + prev.format(DateTimeFormatter.ofPattern("yyyy-MM", Locale.ROOT))
                    + ".csv";
            Path out = Path.of(exportDir, fileName);
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(out.toFile(), false))) {
                bw.write("event_id,orders_count,revenue\n");
                for (Row r : rows) {
                    bw.write(r.eventId + "," + r.count + "," + r.revenue + "\n");
                }
            }
            log.info("[Monthly Statement] Exported {} rows to {}", rows.size(), out.toAbsolutePath());
        } catch (Exception ex) {
            log.error("Monthly Event Statement export failed", ex);
        }
    }

    private void ensureDir(String dir) throws IOException {
        File f = new File(dir);
        if (!f.exists()) {
            Files.createDirectories(f.toPath());
        }
    }

    private static class Row {
        long eventId;
        int count;
        BigDecimal revenue;
    }

    private Map<Long, Integer> querySoldSeatsByEvent() {
        String sql = "SELECT event_id, COUNT(*) AS sold_cnt FROM seats WHERE status='SOLD' GROUP BY event_id";
        return jdbc.query(sql, (rs) -> {
            Map<Long, Integer> map = new HashMap<>();
            while (rs.next()) {
                map.put(rs.getLong("event_id"), rs.getInt("sold_cnt"));
            }
            return map;
        });
    }

    private Map<Long, Integer> queryOrderedSeatCountByEvent() {
        // We count seats from CSV field seat_ids_csv for orders in CONFIRMED/COMPLETED
        String sql = "SELECT event_id, seat_ids_csv FROM orders WHERE state IN ('CONFIRMED','COMPLETED')";
        List<Map.Entry<Long, Integer>> entries = jdbc.query(sql, (rs) -> {
            Map<Long, Integer> list = new HashMap<>();
            while (rs.next()) {
                long eventId = rs.getLong("event_id");
                String csv = rs.getString("seat_ids_csv");
                int seats = countCsv(csv);
                list.merge(eventId, seats, Integer::sum);
            }
            return list.entrySet().stream().collect(Collectors.toList());
        });

        Map<Long, Integer> out = new HashMap<>();
        for (var e : entries) {
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    private int countCsv(String csv) {
        if (csv == null || csv.isBlank())
            return 0;
        // Count commas + 1 after trimming
        String s = csv.trim();
        int commas = 0;
        for (int i = 0; i < s.length(); i++)
            if (s.charAt(i) == ',')
                commas++;
        return commas + 1;
    }
}
