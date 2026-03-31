package com.wesleytaumaturgo.cqrs.axon.service;

import com.wesleytaumaturgo.cqrs.axon.aggregate.DepositMoneyAxonCommand;
import com.wesleytaumaturgo.cqrs.axon.aggregate.OpenBankAccountAxonCommand;
import com.wesleytaumaturgo.cqrs.axon.aggregate.WithdrawMoneyAxonCommand;
import com.wesleytaumaturgo.cqrs.axon.projection.AxonAccountBalanceProjection;
import com.wesleytaumaturgo.cqrs.axon.projection.AxonBalanceView;
import com.wesleytaumaturgo.cqrs.domain.account.exceptions.AccountNotFoundException;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AxonAccountService {

    private final CommandGateway commandGateway;
    private final AxonAccountBalanceProjection projection;

    public AxonAccountService(CommandGateway commandGateway,
                              AxonAccountBalanceProjection projection) {
        this.commandGateway = commandGateway;
        this.projection = projection;
    }

    public String openAccount(String ownerId, BigDecimal initialBalance) {
        var accountId = UUID.randomUUID().toString();
        commandGateway.sendAndWait(new OpenBankAccountAxonCommand(accountId, ownerId, initialBalance));
        return accountId;
    }

    public AxonBalanceView deposit(String accountId, BigDecimal amount) {
        try {
            commandGateway.sendAndWait(new DepositMoneyAxonCommand(accountId, amount));
        } catch (AggregateNotFoundException ex) {
            throw new AccountNotFoundException(accountId);
        }
        return projection.getBalance(accountId);
    }

    public AxonBalanceView withdraw(String accountId, BigDecimal amount) {
        try {
            commandGateway.sendAndWait(new WithdrawMoneyAxonCommand(accountId, amount));
        } catch (AggregateNotFoundException ex) {
            throw new AccountNotFoundException(accountId);
        }
        return projection.getBalance(accountId);
    }

    public AxonBalanceView getBalance(String accountId) {
        return projection.getBalance(accountId);
    }
}
