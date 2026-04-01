package com.wesleytaumaturgo.cqrs.domain.account.commands;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.Money;

public record WithdrawMoneyCommand(AccountId accountId, Money amount) {}
