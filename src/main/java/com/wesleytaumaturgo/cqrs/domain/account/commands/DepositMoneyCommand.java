package com.wesleytaumaturgo.cqrs.domain.account.commands;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.Money;

public record DepositMoneyCommand(AccountId accountId, Money amount) {}
