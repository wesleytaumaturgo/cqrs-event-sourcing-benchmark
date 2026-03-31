package com.wesleytaumaturgo.cqrs.axon.projection;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AxonBalanceViewRepository extends JpaRepository<AxonBalanceView, String> {}
