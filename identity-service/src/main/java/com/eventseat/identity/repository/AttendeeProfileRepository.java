package com.eventseat.identity.repository;

import com.eventseat.identity.domain.AttendeeProfileEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendeeProfileRepository extends JpaRepository<AttendeeProfileEntity, Long> {
    Optional<AttendeeProfileEntity> findByUserId(Long userId);
}
