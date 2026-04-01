package com.wesleytaumaturgo.cqrs.axon.aggregate;

import com.wesleytaumaturgo.cqrs.domain.account.Money;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record OpenBankAccountAxonCommand(
    @TargetAggregateIdentifier String accountId,
    String ownerId,
    Money initialBalance
) {}
