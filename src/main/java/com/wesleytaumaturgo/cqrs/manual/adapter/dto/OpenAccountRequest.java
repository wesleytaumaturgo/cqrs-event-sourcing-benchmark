package com.wesleytaumaturgo.cqrs.manual.adapter.dto;

import java.math.BigDecimal;

public record OpenAccountRequest(String ownerId, BigDecimal initialBalance) {}
