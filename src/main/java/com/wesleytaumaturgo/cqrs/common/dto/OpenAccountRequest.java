package com.wesleytaumaturgo.cqrs.common.dto;

import java.math.BigDecimal;

public record OpenAccountRequest(String ownerId, BigDecimal initialBalance) {}
