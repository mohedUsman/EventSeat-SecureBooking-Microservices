package com.eventseat.review.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTOs for Review API.
 */
public class ReviewDtos {

    public static class ReviewCreateRequest {
        @NotNull
        private Long eventId;

        // We require the orderId to validate attendance against order-service
        @NotNull
        private Long orderId;

        @NotNull
        @Min(1) @Max(5)
        private Integer rating;

        @Size(max = 4000)
        private String text;

        public Long getEventId() { return eventId; }
        public void setEventId(Long eventId) { this.eventId = eventId; }
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public static class ReviewResponse {
        private Long id;
        private Long attendeeId;
        private Long eventId;
        private Integer rating;
        private String text;
        private OffsetDateTime createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getAttendeeId() { return attendeeId; }
        public void setAttendeeId(Long attendeeId) { this.attendeeId = attendeeId; }
        public Long getEventId() { return eventId; }
        public void setEventId(Long eventId) { this.eventId = eventId; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class EventReviewsResponse {
        private Long eventId;
        private Double averageRating; // null if no reviews
        private List<ReviewResponse> reviews;

        public Long getEventId() { return eventId; }
        public void setEventId(Long eventId) { this.eventId = eventId; }
        public Double getAverageRating() { return averageRating; }
        public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
        public List<ReviewResponse> getReviews() { return reviews; }
        public void setReviews(List<ReviewResponse> reviews) { this.reviews = reviews; }
    }

    // Internal client view of order-service /orders/{id}
    public static class OrderView {
        private Long id;
        private Long attendeeId;
        private Long eventId;
        private String state; // PENDING | CONFIRMED | CHECKED_IN | COMPLETED | CANCELLED

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getAttendeeId() { return attendeeId; }
        public void setAttendeeId(Long attendeeId) { this.attendeeId = attendeeId; }
        public Long getEventId() { return eventId; }
        public void setEventId(Long eventId) { this.eventId = eventId; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
    }
}
