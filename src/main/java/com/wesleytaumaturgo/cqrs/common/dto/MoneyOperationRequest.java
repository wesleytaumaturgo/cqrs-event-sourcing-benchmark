package com.wesleytaumaturgo.cqrs.common.dto;

import java.math.BigDecimal;

public record MoneyOperationRequest(BigDecimal amount) {}
