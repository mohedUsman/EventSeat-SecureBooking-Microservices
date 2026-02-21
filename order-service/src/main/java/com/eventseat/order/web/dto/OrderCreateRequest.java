package com.eventseat.order.web.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Request to create an order idempotently.
 * Requires Idempotency-Key header on the request.
 * simulate can be: "decline", "timeout", or omitted for success path.
 */
public class OrderCreateRequest {

    @NotNull
    private Long attendeeId;

    @NotNull
    private Long eventId;

    @NotEmpty
    private List<Long> seatIds;

    @NotNull
    private String currency;

    @NotNull
    private Long holdId;

    private String simulate; // optional: "decline" | "timeout"

    public Long getAttendeeId() {
        return attendeeId;
    }

    public void setAttendeeId(Long attendeeId) {
        this.attendeeId = attendeeId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = seatIds;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getHoldId() {
        return holdId;
    }

    public void setHoldId(Long holdId) {
        this.holdId = holdId;
    }

    public String getSimulate() {
        return simulate;
    }

    public void setSimulate(String simulate) {
        this.simulate = simulate;
    }
}
