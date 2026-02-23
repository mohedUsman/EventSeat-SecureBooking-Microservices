package com.eventseat.review.web;

import java.net.URI;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Consistent ProblemDetail responses for review endpoints so 4xx/5xx include
 * useful detail.
 * Maps ReviewService conflicts (e.g., order_event_mismatch,
 * duplicate_review_for_attendee_event,
 * review_not_allowed_until_checked_in_or_completed,
 * order_not_found_or_unreachable) into the "detail" field.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleRse(ResponseStatusException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(ex.getStatusCode());
        pd.setTitle("Request Failed");
        pd.setType(URI.create("about:blank#review-error"));
        String detail = ex.getReason() != null ? ex.getReason() : safeMessage(ex);
        pd.setDetail(detail);
        if (ex.getCause() != null) {
            pd.setProperty("cause", safeMessage(ex.getCause()));
        }
        return pd;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Data Integrity Violation");
        pd.setType(URI.create("about:blank#data-integrity"));
        pd.setDetail("A data integrity rule was violated. " + safeMessage(ex.getMostSpecificCause()));
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Internal Server Error");
        pd.setType(URI.create("about:blank#internal-server-error"));
        pd.setDetail("Unexpected error occurred. " + safeMessage(ex));
        return pd;
    }

    private String safeMessage(Throwable t) {
        return t == null ? "" : t.getMessage();
    }
}
