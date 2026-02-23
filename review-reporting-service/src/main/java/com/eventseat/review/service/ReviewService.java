package com.eventseat.review.service;

import com.eventseat.review.domain.ReviewEntity;
import com.eventseat.review.repository.ReviewRepository;
import com.eventseat.review.web.dto.ReviewDtos.EventReviewsResponse;
import com.eventseat.review.web.dto.ReviewDtos.OrderView;
import com.eventseat.review.web.dto.ReviewDtos.ReviewCreateRequest;
import com.eventseat.review.web.dto.ReviewDtos.ReviewResponse;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReviewService {

    private final ReviewRepository repo;
    private final WebClient.Builder webClientBuilder;

    public ReviewService(ReviewRepository repo, WebClient.Builder webClientBuilder) {
        this.repo = repo;
        this.webClientBuilder = webClientBuilder;
    }

    public ReviewResponse createReview(Long attendeeId, String bearerToken, ReviewCreateRequest req) {
        // 1) Validate attendance by querying order-service
        OrderView order = fetchOrder(bearerToken, req.getOrderId());
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "order_not_found_or_unreachable");
        }
        if (!Objects.equals(order.getAttendeeId(), attendeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "order_does_not_belong_to_attendee");
        }
        if (!Objects.equals(order.getEventId(), req.getEventId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "order_event_mismatch");
        }
        // Allow review only after attendance/consumption
        if (!( "COMPLETED".equalsIgnoreCase(order.getState()) || "CHECKED_IN".equalsIgnoreCase(order.getState()) )) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "review_not_allowed_until_checked_in_or_completed");
        }

        // 2) Enforce one review per attendee+event
        if (repo.existsByAttendeeIdAndEventId(attendeeId, req.getEventId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "duplicate_review_for_attendee_event");
        }

        // 3) Persist as PENDING (requires moderation)
        ReviewEntity e = new ReviewEntity();
        e.setAttendeeId(attendeeId);
        e.setEventId(req.getEventId());
        e.setRating(req.getRating());
        e.setText(req.getText());
        e.setStatus(ReviewEntity.Status.PENDING);
        ReviewEntity saved = repo.save(e);

        return toResponse(saved);
    }

    public EventReviewsResponse getEventReviews(Long eventId) {
        // Public reads only show APPROVED reviews
        List<ReviewEntity> approvedAll = repo.findByEventIdAndStatus(eventId, ReviewEntity.Status.APPROVED);
        double avg = approvedAll.isEmpty() ? Double.NaN :
                approvedAll.stream().mapToInt(r -> r.getRating() == null ? 0 : r.getRating()).average().orElse(Double.NaN);

        List<ReviewEntity> latest = repo.findTop20ByEventIdAndStatusOrderByCreatedAtDesc(eventId, ReviewEntity.Status.APPROVED);

        EventReviewsResponse r = new EventReviewsResponse();
        r.setEventId(eventId);
        r.setAverageRating(approvedAll.isEmpty() ? null : avg);
        r.setReviews(latest.stream().map(this::toResponse).collect(Collectors.toList()));
        return r;
    }

    public ReviewResponse approveReview(Long id) {
        ReviewEntity e = findOr404(id);
        if (e.getStatus() == ReviewEntity.Status.APPROVED) {
            return toResponse(e);
        }
        e.setStatus(ReviewEntity.Status.APPROVED);
        return toResponse(repo.save(e));
    }

    public ReviewResponse rejectReview(Long id) {
        ReviewEntity e = findOr404(id);
        if (e.getStatus() == ReviewEntity.Status.REJECTED) {
            return toResponse(e);
        }
        e.setStatus(ReviewEntity.Status.REJECTED);
        return toResponse(repo.save(e));
    }

    private ReviewEntity findOr404(Long id) {
        Optional<ReviewEntity> opt = repo.findById(id);
        if (opt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "review_not_found");
        }
        return opt.get();
    }

    private ReviewResponse toResponse(ReviewEntity e) {
        ReviewResponse r = new ReviewResponse();
        r.setId(e.getId());
        r.setAttendeeId(e.getAttendeeId());
        r.setEventId(e.getEventId());
        r.setRating(e.getRating());
        r.setText(e.getText());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }

    private OrderView fetchOrder(String bearerToken, Long orderId) {
        try {
            return webClientBuilder.build()
                .get()
                .uri("http://order-service/api/v1/orders/{id}", orderId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(OrderView.class)
                .block();
        } catch (Exception ex) {
            return null;
        }
    }
}
