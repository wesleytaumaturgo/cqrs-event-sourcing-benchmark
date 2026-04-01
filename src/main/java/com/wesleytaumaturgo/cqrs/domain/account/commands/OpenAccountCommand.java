package com.wesleytaumaturgo.cqrs.domain.account.commands;

import com.wesleytaumaturgo.cqrs.domain.account.Money;

public record OpenAccountCommand(String ownerId, Money initialBalance) {}
