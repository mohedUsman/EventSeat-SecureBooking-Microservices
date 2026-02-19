package com.eventseat.catalog.web.dto;

import java.time.OffsetDateTime;

/**
 * Search projection for events including available seats summary.
 */
public class EventSearchItem {
    private Long id;
    private Long organizerId;
    private Long venueId;
    private String title;
    private String category;
    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;
    private String status;
    private String city; // from Venue
    private long availableSeatsRemaining;

    public EventSearchItem() {
    }

    public EventSearchItem(Long id, Long organizerId, Long venueId, String title, String category,
            OffsetDateTime startDateTime, OffsetDateTime endDateTime, String status,
            String city, long availableSeatsRemaining) {
        this.id = id;
        this.organizerId = organizerId;
        this.venueId = venueId;
        this.title = title;
        this.category = category;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.status = status;
        this.city = city;
        this.availableSeatsRemaining = availableSeatsRemaining;
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

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public long getAvailableSeatsRemaining() {
        return availableSeatsRemaining;
    }

    public void setAvailableSeatsRemaining(long availableSeatsRemaining) {
        this.availableSeatsRemaining = availableSeatsRemaining;
    }
}
