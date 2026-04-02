package com.wesleytaumaturgo.cqrs.manual.projection;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity para a tabela account_balance_view (V2 migration).
 * Read model da projeção de saldo — atualizado após cada evento.
 */
@Entity
@Table(name = "account_balance_view")
public class AccountBalanceView {

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    @Column(name = "version", nullable = false)
    private Long version;

    protected AccountBalanceView() {}

    public AccountBalanceView(UUID accountId, String ownerId, BigDecimal balance,
                              Instant lastUpdated, Long version) {
        this.accountId = accountId;
        this.ownerId = ownerId;
        this.balance = balance;
        this.lastUpdated = lastUpdated;
        this.version = version;
    }

    public UUID getAccountId() { return accountId; }
    public String getOwnerId() { return ownerId; }
    public BigDecimal getBalance() { return balance; }
    public Instant getLastUpdated() { return lastUpdated; }
    public Long getVersion() { return version; }

    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    public void setVersion(Long version) { this.version = version; }
}
