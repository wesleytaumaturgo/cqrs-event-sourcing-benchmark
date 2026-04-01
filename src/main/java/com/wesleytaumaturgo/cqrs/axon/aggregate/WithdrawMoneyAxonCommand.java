package com.wesleytaumaturgo.cqrs.axon.aggregate;

import com.wesleytaumaturgo.cqrs.domain.account.Money;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record WithdrawMoneyAxonCommand(
    @TargetAggregateIdentifier String accountId,
    Money amount
) {}
