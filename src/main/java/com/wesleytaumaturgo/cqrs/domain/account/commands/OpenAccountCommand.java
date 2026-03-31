package com.wesleytaumaturgo.cqrs.domain.account.commands;

import java.math.BigDecimal;

public record OpenAccountCommand(String ownerId, BigDecimal initialBalance) {}
