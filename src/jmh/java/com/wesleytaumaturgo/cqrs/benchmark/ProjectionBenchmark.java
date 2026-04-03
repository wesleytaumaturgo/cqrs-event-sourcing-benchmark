package com.wesleytaumaturgo.cqrs.benchmark;

import com.wesleytaumaturgo.cqrs.CqrsBenchmarkApplication;
import com.wesleytaumaturgo.cqrs.axon.projection.AxonAccountBalanceProjection;
import com.wesleytaumaturgo.cqrs.axon.service.AxonAccountService;
import com.wesleytaumaturgo.cqrs.domain.account.AccountId;
import com.wesleytaumaturgo.cqrs.manual.projection.AccountBalanceProjection;
import com.wesleytaumaturgo.cqrs.manual.service.ManualAccountService;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * B4 — Tempo de Projeção (comando + leitura do read model).
 *
 * Mede o tempo total de: emitir comando → projeção atualizada → leitura do saldo.
 * Em modo subscribing, a projeção é atualizada sincronamente no mesmo thread.
 * Modos: AverageTime (média) e SampleTime (distribuição p50/p95/p99).
 * REQ-8.EARS-1
 */
@BenchmarkMode({Mode.AverageTime, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx1g"})
@State(Scope.Benchmark)
public class ProjectionBenchmark {

    @Param({"manual", "axon"})
    private String impl;

    private ConfigurableApplicationContext ctx;
    private ManualAccountService      manualService;
    private AxonAccountService        axonService;
    private AccountBalanceProjection  manualProjection;
    private AxonAccountBalanceProjection axonProjection;

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

        manualService    = ctx.getBean(ManualAccountService.class);
        axonService      = ctx.getBean(AxonAccountService.class);
        manualProjection = ctx.getBean(AccountBalanceProjection.class);
        axonProjection   = ctx.getBean(AxonAccountBalanceProjection.class);

        if ("manual".equals(impl)) {
            accountId       = manualService.openAccount("proj-owner", BigDecimal.valueOf(100_000));
            manualAccountId = AccountId.of(accountId);
        } else {
            accountId = axonService.openAccount("proj-owner", BigDecimal.valueOf(100_000));
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (ctx != null) ctx.close();
    }

    /**
     * B4: Emite depósito e lê o saldo da projeção em sequência.
     * Mede latência total: comando → evento → projeção → leitura.
     * REQ-8.EARS-1
     */
    @Benchmark
    public void commandThenReadProjection(Blackhole bh) {
        if ("manual".equals(impl)) {
            manualService.deposit(accountId, BigDecimal.ONE);
            bh.consume(manualProjection.getBalance(manualAccountId));
        } else {
            axonService.deposit(accountId, BigDecimal.ONE);
            bh.consume(axonProjection.getBalance(accountId));
        }
    }
}
