package com.wesleytaumaturgo.cqrs.domain.account.commands;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;

import java.math.BigDecimal;

public record WithdrawMoneyCommand(AccountId accountId, BigDecimal amount) {}
