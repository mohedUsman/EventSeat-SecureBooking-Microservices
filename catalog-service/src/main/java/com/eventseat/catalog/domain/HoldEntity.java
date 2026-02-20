package com.eventseat.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "holds")
public class HoldEntity {

    public enum Status {
        ACTIVE, EXPIRED, RELEASED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long attendeeId;

    @Column(nullable = false)
    private Long eventId;

    // Comma-separated seat ids for simplicity (keeps schema minimal; JDBC ops
    // handle arrays)
    @Column(nullable = false, length = 2000)
    private String seatIdsCsv;

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.ACTIVE;

    public HoldEntity() {
    }

    public HoldEntity(Long attendeeId, Long eventId, String seatIdsCsv, OffsetDateTime createdAt,
            OffsetDateTime expiresAt, Status status) {
        this.attendeeId = attendeeId;
        this.eventId = eventId;
        this.seatIdsCsv = seatIdsCsv;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

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

    public String getSeatIdsCsv() {
        return seatIdsCsv;
    }

    public void setSeatIdsCsv(String seatIdsCsv) {
        this.seatIdsCsv = seatIdsCsv;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
