package com.wesleytaumaturgo.cqrs.domain.account.exceptions;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(BigDecimal balance, BigDecimal requested) {
        super(String.format("Insufficient funds: balance=%s, requested=%s",
            balance.toPlainString(), requested.toPlainString()));
    }
}
