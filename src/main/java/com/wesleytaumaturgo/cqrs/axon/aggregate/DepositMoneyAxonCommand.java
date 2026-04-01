package com.wesleytaumaturgo.cqrs.axon.aggregate;

import com.wesleytaumaturgo.cqrs.domain.account.Money;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record DepositMoneyAxonCommand(
    @TargetAggregateIdentifier String accountId,
    Money amount
) {}
