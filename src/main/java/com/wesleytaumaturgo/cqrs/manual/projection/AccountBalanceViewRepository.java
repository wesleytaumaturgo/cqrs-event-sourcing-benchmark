package com.wesleytaumaturgo.cqrs.manual.projection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccountBalanceViewRepository extends JpaRepository<AccountBalanceView, UUID> {}
