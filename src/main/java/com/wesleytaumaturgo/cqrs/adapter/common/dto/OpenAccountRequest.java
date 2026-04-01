package com.wesleytaumaturgo.cqrs.adapter.common.dto;

import java.math.BigDecimal;

public record OpenAccountRequest(String ownerId, BigDecimal initialBalance) {}
