package com.eventseat.catalog.web;

import java.util.Map;

/**
 * Thrown when a hold cannot be created because one or more seats are not
 * AVAILABLE or don't match the event.
 * Carries a per-seat diagnostics map to help clients fix the request.
 */
public class HoldConflictException extends RuntimeException {
    private final Map<Long, Object> seatDiagnostics;

    public HoldConflictException(String message, Map<Long, Object> seatDiagnostics) {
        super(message);
        this.seatDiagnostics = seatDiagnostics;
    }

    public Map<Long, Object> getSeatDiagnostics() {
        return seatDiagnostics;
    }
}
