package com.wesleytaumaturgo.cqrs.axon.aggregate;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.math.BigDecimal;

public record OpenBankAccountAxonCommand(
    @TargetAggregateIdentifier String accountId,
    String ownerId,
    BigDecimal initialBalance
) {}
