package com.wesleytaumaturgo.cqrs.manual.eventstore;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoredEventRepository extends JpaRepository<StoredEvent, Long> {

    List<StoredEvent> findByAggregateIdOrderBySequenceNumberAsc(UUID aggregateId);

    @Query("SELECT MAX(e.sequenceNumber) FROM StoredEvent e WHERE e.aggregateId = :aggregateId")
    Optional<Long> findMaxSequenceNumber(@Param("aggregateId") UUID aggregateId);
}
