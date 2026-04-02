package com.wesleytaumaturgo.cqrs.config;

import com.wesleytaumaturgo.cqrs.domain.account.exceptions.AccountNotFoundException;
import com.wesleytaumaturgo.cqrs.domain.account.exceptions.InsufficientFundsException;
import com.wesleytaumaturgo.cqrs.domain.account.exceptions.OptimisticLockingException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleAccountNotFound(AccountNotFoundException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleInsufficientFunds(InsufficientFundsException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleOptimisticLocking(OptimisticLockingException ex) {
        return Map.of("error", "Operation failed due to a concurrent update, please retry");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }
}
