package com.eventseat.catalog.web.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ImportDtos {

    public static class ImportRowRequest {
        public String section;
        public String rowLabel;
        public String seatNumber;
        public BigDecimal basePrice;
        public String currency;
    }

    public static class ImportRowResult {
        private int rowNumber; // 1-based (including header row counting)
        private boolean success;
        private String message;
        private Long seatId; // when upserted/updated

        public int getRowNumber() {
            return rowNumber;
        }

        public void setRowNumber(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Long getSeatId() {
            return seatId;
        }

        public void setSeatId(Long seatId) {
            this.seatId = seatId;
        }
    }

    public static class ImportReport {
        private Long eventId;
        private String idempotencyKey;
        private String requestHash;
        private int total;
        private int success;
        private int failed;
        private List<ImportRowResult> rows = new ArrayList<>();

        public Long getEventId() {
            return eventId;
        }

        public void setEventId(Long eventId) {
            this.eventId = eventId;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public void setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }

        public String getRequestHash() {
            return requestHash;
        }

        public void setRequestHash(String requestHash) {
            this.requestHash = requestHash;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getSuccess() {
            return success;
        }

        public void setSuccess(int success) {
            this.success = success;
        }

        public int getFailed() {
            return failed;
        }

        public void setFailed(int failed) {
            this.failed = failed;
        }

        public List<ImportRowResult> getRows() {
            return rows;
        }

        public void setRows(List<ImportRowResult> rows) {
            this.rows = rows;
        }
    }
}
