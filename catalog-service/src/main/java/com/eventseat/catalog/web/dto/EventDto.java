package com.eventseat.catalog.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public class EventDto {
    private Long id;

    @NotNull
    @Min(1)
    private Long organizerId;

    @NotNull
    @Min(1)
    private Long venueId;

    @NotBlank
    @Size(max = 250)
    private String title;

    @NotBlank
    @Size(max = 80)
    private String category;

    @NotNull
    private OffsetDateTime startDateTime;

    @NotNull
    private OffsetDateTime endDateTime;

    @NotBlank
    @Size(max = 20)
    private String status; // DRAFT, PUBLISHED, CANCELLED

    public EventDto() {
    }

    public EventDto(Long id, Long organizerId, Long venueId, String title, String category,
            OffsetDateTime startDateTime, OffsetDateTime endDateTime, String status) {
        this.id = id;
        this.organizerId = organizerId;
        this.venueId = venueId;
        this.title = title;
        this.category = category;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(Long organizerId) {
        this.organizerId = organizerId;
    }

    public Long getVenueId() {
        return venueId;
    }

    public void setVenueId(Long venueId) {
        this.venueId = venueId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public OffsetDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(OffsetDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public OffsetDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(OffsetDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
