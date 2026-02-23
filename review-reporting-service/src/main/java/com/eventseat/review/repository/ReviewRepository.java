package com.eventseat.review.repository;

import com.eventseat.review.domain.ReviewEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    List<ReviewEntity> findTop20ByEventIdAndStatusOrderByCreatedAtDesc(Long eventId, ReviewEntity.Status status);

    List<ReviewEntity> findByEventIdAndStatus(Long eventId, ReviewEntity.Status status);

    @Deprecated
    @Query("select avg(r.rating) from ReviewEntity r where r.eventId = :eventId")
    Double findAverageRatingByEventId(Long eventId);

    boolean existsByAttendeeIdAndEventId(Long attendeeId, Long eventId);
}
