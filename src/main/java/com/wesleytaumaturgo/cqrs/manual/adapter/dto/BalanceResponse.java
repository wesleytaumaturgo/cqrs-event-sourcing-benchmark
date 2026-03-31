package com.wesleytaumaturgo.cqrs.manual.adapter.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BalanceResponse(String accountId, BigDecimal balance, Instant lastUpdated) {}
