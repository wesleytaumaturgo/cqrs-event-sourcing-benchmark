package com.wesleytaumaturgo.cqrs.axon.projection;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "axon_account_balance_view")
public class AxonBalanceView {

    @Id
    @Column(name = "account_id")
    private String accountId;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    @Column(name = "version", nullable = false)
    private Long version;

    protected AxonBalanceView() {}

    public AxonBalanceView(String accountId, String ownerId, BigDecimal balance,
                           Instant lastUpdated, Long version) {
        this.accountId = accountId;
        this.ownerId = ownerId;
        this.balance = balance;
        this.lastUpdated = lastUpdated;
        this.version = version;
    }

    public String getAccountId() { return accountId; }
    public String getOwnerId() { return ownerId; }
    public BigDecimal getBalance() { return balance; }
    public Instant getLastUpdated() { return lastUpdated; }
    public Long getVersion() { return version; }

    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    public void setVersion(Long version) { this.version = version; }
}
