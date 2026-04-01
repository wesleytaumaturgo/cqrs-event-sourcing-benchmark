package com.wesleytaumaturgo.cqrs.manual.adapter;

import com.wesleytaumaturgo.cqrs.adapter.common.dto.AccountCreatedResponse;
import com.wesleytaumaturgo.cqrs.adapter.common.dto.BalanceResponse;
import com.wesleytaumaturgo.cqrs.adapter.common.dto.MoneyOperationRequest;
import com.wesleytaumaturgo.cqrs.adapter.common.dto.OpenAccountRequest;
import com.wesleytaumaturgo.cqrs.manual.service.ManualAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/manual")
public class ManualAccountController {

    private final ManualAccountService service;

    public ManualAccountController(ManualAccountService service) {
        this.service = service;
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountCreatedResponse openAccount(@RequestBody OpenAccountRequest request) {
        var accountId = service.openAccount(request.ownerId(), request.initialBalance());
        return new AccountCreatedResponse(accountId);
    }

    @PostMapping("/accounts/{id}/deposits")
    public BalanceResponse deposit(@PathVariable String id,
                                   @RequestBody MoneyOperationRequest request) {
        var view = service.deposit(id, request.amount());
        return new BalanceResponse(id, view.getBalance(), view.getLastUpdated());
    }

    @PostMapping("/accounts/{id}/withdrawals")
    public BalanceResponse withdraw(@PathVariable String id,
                                    @RequestBody MoneyOperationRequest request) {
        var view = service.withdraw(id, request.amount());
        return new BalanceResponse(id, view.getBalance(), view.getLastUpdated());
    }

    @GetMapping("/accounts/{id}/balance")
    public BalanceResponse getBalance(@PathVariable String id) {
        var view = service.getBalance(id);
        return new BalanceResponse(id, view.getBalance(), view.getLastUpdated());
    }
}
