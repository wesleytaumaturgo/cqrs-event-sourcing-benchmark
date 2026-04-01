package com.wesleytaumaturgo.cqrs.domain.account.exceptions;

import com.wesleytaumaturgo.cqrs.domain.account.Money;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(Money balance, Money requested) {
        super(String.format("Insufficient funds: balance=%s, requested=%s", balance, requested));
    }
}
