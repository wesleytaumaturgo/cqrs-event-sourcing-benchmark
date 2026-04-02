package com.wesleytaumaturgo.cqrs.manual.projection;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountBalanceViewRepository extends JpaRepository<AccountBalanceView, UUID> {}
