package com.wesleytaumaturgo.cqrs.benchmark;

import com.wesleytaumaturgo.cqrs.CqrsBenchmarkApplication;
import com.wesleytaumaturgo.cqrs.axon.service.AxonAccountService;
import com.wesleytaumaturgo.cqrs.manual.service.ManualAccountService;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * B2 — Throughput de Eventos (eventos/segundo, Manual vs Axon).
 * Cada chamada ao @Benchmark persiste 1 evento de depósito.
 * REQ-8.EARS-1
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgs = {"-Xms512m", "-Xmx1g"})
@State(Scope.Benchmark)
public class EventThroughputBenchmark {

    @Param({"manual", "axon"})
    private String impl;

    private ConfigurableApplicationContext ctx;
    private ManualAccountService manualService;
    private AxonAccountService axonService;

    /** Conta pré-existente para não incluir custo de openAccount na medição. */
    private String accountId;

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

        // Criar conta com saldo alto para não esgotar em medição de throughput
        if ("manual".equals(impl)) {
            accountId = manualService.openAccount("throughput-owner", BigDecimal.valueOf(1_000_000));
        } else {
            accountId = axonService.openAccount("throughput-owner", BigDecimal.valueOf(1_000_000));
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (ctx != null) ctx.close();
    }

    /**
     * B2: Persiste 1 evento de depósito por iteração. Mede eventos/segundo.
     * REQ-8.EARS-1
     */
    @Benchmark
    public void storeDepositEvent(Blackhole bh) {
        if ("manual".equals(impl)) {
            bh.consume(manualService.deposit(accountId, BigDecimal.ONE));
        } else {
            bh.consume(axonService.deposit(accountId, BigDecimal.ONE));
        }
    }
}
