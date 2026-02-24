package com.eventseat.catalog.service;

import com.eventseat.catalog.repository.IdempotencyImportJdbcRepository;
import com.eventseat.catalog.repository.SeatJdbcRepository;
import com.eventseat.catalog.web.dto.ImportDtos.ImportReport;
import com.eventseat.catalog.web.dto.ImportDtos.ImportRowRequest;
import com.eventseat.catalog.web.dto.ImportDtos.ImportRowResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventoryImportService {

    private final SeatJdbcRepository seatRepo;
    private final IdempotencyImportJdbcRepository idemRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InventoryImportService(SeatJdbcRepository seatRepo, IdempotencyImportJdbcRepository idemRepo) {
        this.seatRepo = seatRepo;
        this.idemRepo = idemRepo;
    }

    public ImportReport importInventory(Long eventId, String idempotencyKey, String originalFilename,
            String contentType, byte[] bytes) {
        if (eventId == null || eventId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "eventId_required");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idempotency_key_required");
        }
        String requestHash = sha256Hex(
                (originalFilename == null ? "" : originalFilename) + "|" + eventId + "|" + sha256Hex(bytes));

        // Idempotency check
        var cached = idemRepo.findByKey(idempotencyKey);
        if (cached != null) {
            if (!requestHash.equals(cached.requestHash)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "idempotency_key_reuse_with_different_request");
            }
            if (cached.responseJson != null && !cached.responseJson.isBlank()) {
                try {
                    return objectMapper.readValue(cached.responseJson, ImportReport.class);
                } catch (IOException e) {
                    // fallthrough (re-process if cache can't be parsed)
                }
            }
        }

        List<ImportRowRequest> rows = parseFile(originalFilename, contentType, bytes);

        ImportReport report = new ImportReport();
        report.setEventId(eventId);
        report.setIdempotencyKey(idempotencyKey);
        report.setRequestHash(requestHash);
        report.setTotal(rows.size());

        int rowNum = 1; // 1-based for report
        int ok = 0, fail = 0;
        for (ImportRowRequest r : rows) {
            ImportRowResult rr = new ImportRowResult();
            rr.setRowNumber(rowNum++);

            String validation = validateRow(r);
            if (validation != null) {
                rr.setSuccess(false);
                rr.setMessage(validation);
                report.getRows().add(rr);
                fail++;
                continue;
            }

            try {
                Long seatId = seatRepo.upsertByNaturalKey(eventId,
                        trim(r.section), trim(r.rowLabel), trim(r.seatNumber),
                        r.basePrice, r.currency == null ? null : r.currency.toUpperCase());
                rr.setSuccess(true);
                rr.setSeatId(seatId);
                rr.setMessage("upserted");
                ok++;
            } catch (Exception ex) {
                rr.setSuccess(false);
                rr.setMessage("db_error: " + shortMsg(ex.getMessage()));
                fail++;
            }
            report.getRows().add(rr);
        }
        report.setSuccess(ok);
        report.setFailed(fail);

        // Cache result in idempotency store
        try {
            String json = objectMapper.writeValueAsString(report);
            if (cached == null) {
                idemRepo.insert(idempotencyKey, requestHash, json);
            } else {
                idemRepo.updateResponse(idempotencyKey, json);
            }
        } catch (Exception ex) {
            // non-fatal
        }

        return report;
    }

    private String validateRow(ImportRowRequest r) {
        if (r == null)
            return "row_null";
        if (r.basePrice == null)
            return "basePrice_required";
        if (r.basePrice.compareTo(BigDecimal.ZERO) < 0)
            return "basePrice_negative";
        if (r.currency == null || r.currency.isBlank())
            return "currency_required";
        String cur = r.currency.trim();
        if (cur.length() != 3)
            return "currency_must_be_3_letters";
        // section/rowLabel/seatNumber are optional, but enforce max length sanity
        if (lenGt(r.section, 50))
            return "section_too_long";
        if (lenGt(r.rowLabel, 50))
            return "rowLabel_too_long";
        if (lenGt(r.seatNumber, 50))
            return "seatNumber_too_long";
        return null;
    }

    private boolean lenGt(String s, int n) {
        return s != null && s.length() > n;
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private List<ImportRowRequest> parseFile(String filename, String contentType, byte[] bytes) {
        String lowerName = filename == null ? "" : filename.toLowerCase();
        String ct = contentType == null ? "" : contentType.toLowerCase();
        try {
            if (lowerName.endsWith(".csv") || ct.contains("text/csv")) {
                try (InputStream in = new BufferedInputStream(new ByteArrayInputStream(bytes))) {
                    return parseCsv(in);
                }
            } else if (lowerName.endsWith(".xlsx")
                    || ct.contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
                try (InputStream in = new BufferedInputStream(new ByteArrayInputStream(bytes))) {
                    return parseXlsx(in);
                }
            } else {
                // Fallback attempt CSV
                try (InputStream in = new BufferedInputStream(new ByteArrayInputStream(bytes))) {
                    return parseCsv(in);
                }
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file_parse_error");
        }
    }

    private List<ImportRowRequest> parseCsv(InputStream in) throws IOException {
        CSVParser parser = CSVParser.parse(in, StandardCharsets.UTF_8, CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build());
        List<ImportRowRequest> rows = new ArrayList<>();
        for (CSVRecord rec : parser) {
            ImportRowRequest r = new ImportRowRequest();
            r.section = get(rec, "section");
            r.rowLabel = get(rec, "rowLabel");
            r.seatNumber = get(rec, "seatNumber");
            r.currency = get(rec, "currency");
            r.basePrice = parseDecimal(get(rec, "basePrice"));
            rows.add(r);
        }
        return rows;
    }

    private String get(CSVRecord rec, String name) {
        try {
            return rec.isMapped(name) ? emptyToNull(rec.get(name)) : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String emptyToNull(String s) {
        if (s == null)
            return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private List<ImportRowRequest> parseXlsx(InputStream in) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(in)) {
            XSSFSheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null)
                return List.of();
            Iterator<Row> it = sheet.iterator();
            if (!it.hasNext())
                return List.of();
            Row header = it.next();
            int colSection = findCol(header, "section");
            int colRow = findCol(header, "rowLabel");
            int colSeat = findCol(header, "seatNumber");
            int colPrice = findCol(header, "basePrice");
            int colCur = findCol(header, "currency");
            List<ImportRowRequest> rows = new ArrayList<>();
            while (it.hasNext()) {
                Row row = it.next();
                ImportRowRequest r = new ImportRowRequest();
                r.section = cellStr(row, colSection);
                r.rowLabel = cellStr(row, colRow);
                r.seatNumber = cellStr(row, colSeat);
                r.currency = cellStr(row, colCur);
                r.basePrice = parseDecimal(cellStr(row, colPrice));
                rows.add(r);
            }
            return rows;
        }
    }

    private int findCol(Row header, String name) {
        String target = name.toLowerCase();
        for (int i = 0; i < header.getLastCellNum(); i++) {
            Cell c = header.getCell(i);
            if (c == null)
                continue;
            String v = c.getStringCellValue();
            if (v != null && v.trim().toLowerCase().equals(target))
                return i;
        }
        return -1;
    }

    private String cellStr(Row row, int col) {
        if (col < 0)
            return null;
        Cell c = row.getCell(col);
        if (c == null)
            return null;
        if (c.getCellType() == CellType.STRING)
            return emptyToNull(c.getStringCellValue());
        if (c.getCellType() == CellType.NUMERIC)
            return String.valueOf(c.getNumericCellValue());
        if (c.getCellType() == CellType.BOOLEAN)
            return c.getBooleanCellValue() ? "true" : "false";
        return null;
    }

    private BigDecimal parseDecimal(String s) {
        if (s == null || s.isBlank())
            return null;
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String shortMsg(String msg) {
        if (msg == null)
            return null;
        String t = msg.replaceAll("\\s+", " ").trim();
        return t.length() > 180 ? t.substring(0, 180) : t;
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(data);
            return toHex(dig);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String sha256Hex(String s) {
        return sha256Hex(s.getBytes(StandardCharsets.UTF_8));
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit((b & 0xF), 16));
        }
        return sb.toString();
    }
}
