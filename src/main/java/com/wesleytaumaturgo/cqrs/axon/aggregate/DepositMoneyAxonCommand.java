package com.wesleytaumaturgo.cqrs.axon.aggregate;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;

public record DepositMoneyAxonCommand(
    @TargetAggregateIdentifier String accountId,
    BigDecimal amount
) {}
