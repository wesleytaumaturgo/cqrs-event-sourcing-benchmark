package com.wesleytaumaturgo.cqrs.manual.eventstore;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoredEventRepository extends JpaRepository<StoredEvent, Long> {

    List<StoredEvent> findByAggregateIdOrderBySequenceNumberAsc(UUID aggregateId);
}
