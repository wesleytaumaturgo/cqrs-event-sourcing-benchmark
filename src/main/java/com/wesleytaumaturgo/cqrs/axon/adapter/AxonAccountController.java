package com.wesleytaumaturgo.cqrs.axon.adapter;

import com.wesleytaumaturgo.cqrs.axon.service.AxonAccountService;
import com.wesleytaumaturgo.cqrs.common.dto.AccountCreatedResponse;
import com.wesleytaumaturgo.cqrs.common.dto.BalanceResponse;
import com.wesleytaumaturgo.cqrs.common.dto.MoneyOperationRequest;
import com.wesleytaumaturgo.cqrs.common.dto.OpenAccountRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/axon")
public class AxonAccountController {

    private final AxonAccountService service;

    public AxonAccountController(AxonAccountService service) {
        this.service = service;
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountCreatedResponse> openAccount(@RequestBody OpenAccountRequest req) {
        var accountId = service.openAccount(req.ownerId(), req.initialBalance());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AccountCreatedResponse(accountId));
    }

    @PostMapping("/accounts/{id}/deposits")
    public ResponseEntity<BalanceResponse> deposit(@PathVariable String id,
                                                   @RequestBody MoneyOperationRequest req) {
        var view = service.deposit(id, req.amount());
        return ResponseEntity.ok(new BalanceResponse(view.getAccountId(), view.getBalance(), view.getLastUpdated()));
    }

    @PostMapping("/accounts/{id}/withdrawals")
    public ResponseEntity<BalanceResponse> withdraw(@PathVariable String id,
                                                    @RequestBody MoneyOperationRequest req) {
        var view = service.withdraw(id, req.amount());
        return ResponseEntity.ok(new BalanceResponse(view.getAccountId(), view.getBalance(), view.getLastUpdated()));
    }

    @GetMapping("/accounts/{id}/balance")
    public ResponseEntity<BalanceResponse> balance(@PathVariable String id) {
        var view = service.getBalance(id);
        return ResponseEntity.ok(new BalanceResponse(view.getAccountId(), view.getBalance(), view.getLastUpdated()));
    }
}
