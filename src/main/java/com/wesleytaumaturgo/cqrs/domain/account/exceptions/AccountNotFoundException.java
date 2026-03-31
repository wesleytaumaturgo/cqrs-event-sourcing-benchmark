package com.wesleytaumaturgo.cqrs.domain.account.exceptions;

import com.wesleytaumaturgo.cqrs.domain.account.AccountId;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(AccountId accountId) {
        super(String.format("Account %s not found", accountId));
    }

    public AccountNotFoundException(String accountId) {
        super(String.format("Account %s not found", accountId));
    }
}
