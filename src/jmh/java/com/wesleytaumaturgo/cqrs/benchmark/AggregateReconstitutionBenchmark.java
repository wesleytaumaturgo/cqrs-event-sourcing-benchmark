package com.wesleytaumaturgo.cqrs.benchmark;

import com.wesleytaumaturgo.cqrs.CqrsBenchmarkApplication;
import com.wesleytaumaturgo.cqrs.axon.service.AxonAccountService;
import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.domain.account.BankAccount;
import com.wesleytaumaturgo.cqrs.manual.eventstore.EventStore;
import com.wesleytaumaturgo.cqrs.manual.service.ManualAccountService;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * B3 — Reconstrução de Aggregate via replay de 100 eventos.
 *
 * Manual: mede loadEvents() + BankAccount.reconstitute() diretamente.
 * Axon:   mede tempo de um comando sobre aggregate com 100 eventos no histórico
 *         (Axon faz replay internamente ao despachar o comando).
 * REQ-8.EARS-1
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgs = {"-Xms512m", "-Xmx1g"})
@State(Scope.Benchmark)
public class AggregateReconstitutionBenchmark {

    private static final int PRE_SEED_EVENTS = 100;

    @Param({"manual", "axon"})
    private String impl;

    private ConfigurableApplicationContext ctx;
    private ManualAccountService manualService;
    private AxonAccountService   axonService;
    private EventStore           eventStore;

    private String    accountId;
    private AccountId manualAccountId;

    @Setup(Level.Trial)
    public void setup() {
        // REQ-8.EARS-2
        var url      = System.getenv().getOrDefault("SPRING_DATASOURCE_URL",      "jdbc:postgresql://localhost:5432/benchmark");
        var username = System.getenv().getOrDefault("SPRING_DATASOURCE_USERNAME", "user");
        var password = System.getenv().getOrDefault("SPRING_DATASOURCE_PASSWORD", "pass");

        ctx = SpringApplication.run(CqrsBenchmarkApplication.class,
            "--spring.datasource.url="      + url,
            "--spring.datasource.username=" + username,
            "--spring.datasource.password=" + password,
            "--spring.flyway.enabled=true",
            "--axon.eventhandling.processors.axon-account-projection.mode=subscribing");

        manualService = ctx.getBean(ManualAccountService.class);
        axonService   = ctx.getBean(AxonAccountService.class);
        eventStore    = ctx.getBean(EventStore.class);

        // Pré-popular conta com 100 eventos de depósito
        if ("manual".equals(impl)) {
            accountId = manualService.openAccount("reconst-owner", BigDecimal.valueOf(100_000));
            manualAccountId = AccountId.of(accountId);
            for (int i = 0; i < PRE_SEED_EVENTS; i++) {
                manualService.deposit(accountId, BigDecimal.ONE);
            }
        } else {
            accountId = axonService.openAccount("reconst-owner", BigDecimal.valueOf(100_000));
            for (int i = 0; i < PRE_SEED_EVENTS; i++) {
                axonService.deposit(accountId, BigDecimal.ONE);
            }
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (ctx != null) ctx.close();
    }

    /**
     * B3: Reconstitui aggregate a partir de 100 eventos persistidos.
     * Manual: replay explícito via EventStore.
     * Axon:   emite comando (Axon reconstitui internamente via seu EventStore).
     * REQ-8.EARS-1
     */
    @Benchmark
    public void reconstituteAggregate(Blackhole bh) {
        if ("manual".equals(impl)) {
            var events  = eventStore.loadEvents(manualAccountId);
            var account = BankAccount.reconstitute(manualAccountId, events);
            bh.consume(account);
        } else {
            // Axon: cada comando força reconstitution interna do aggregate
            bh.consume(axonService.deposit(accountId, BigDecimal.ONE));
        }
    }
}
