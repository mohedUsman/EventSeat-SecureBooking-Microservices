package com.eventseat.catalog.web.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Request to create a hold on a set of seats for an attendee.
 * Note: attendeeId may be cross-checked against JWT uid in controller/service.
 */
public class HoldCreateRequest {

    @NotNull
    private Long attendeeId;

    @NotNull
    private Long eventId;

    @NotEmpty
    private List<Long> seatIds;

    @Min(1)
    private Integer ttlMinutes; // optional; defaults in service when null

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

    public Integer getTtlMinutes() {
        return ttlMinutes;
    }

    public void setTtlMinutes(Integer ttlMinutes) {
        this.ttlMinutes = ttlMinutes;
    }
}
