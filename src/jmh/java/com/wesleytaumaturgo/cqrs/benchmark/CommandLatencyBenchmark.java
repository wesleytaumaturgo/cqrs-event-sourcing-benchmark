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
 * B1 — Latência de Comando (depósito único, Manual vs Axon).
 * Mede latência de comando: AverageTime (média) e SampleTime (distribuição p50/p95/p99).
 * REQ-8.EARS-1
 */
@BenchmarkMode({Mode.AverageTime, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms512m", "-Xmx1g"})
@State(Scope.Benchmark)
public class CommandLatencyBenchmark {

    @Param({"manual", "axon"})
    private String impl;

    private ConfigurableApplicationContext ctx;
    private ManualAccountService manualService;
    private AxonAccountService axonService;

    @Setup(Level.Trial)
    public void setup() {
        // REQ-8.EARS-2: configurável via variável de ambiente
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
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (ctx != null) ctx.close();
    }

    /**
     * B1: Abre conta + realiza depósito. Mede latência end-to-end de comando.
     * REQ-8.EARS-1
     */
    @Benchmark
    public void depositCommand(Blackhole bh) {
        if ("manual".equals(impl)) {
            var accountId = manualService.openAccount("bench-owner", BigDecimal.valueOf(1000));
            bh.consume(manualService.deposit(accountId, BigDecimal.TEN));
        } else {
            var accountId = axonService.openAccount("bench-owner", BigDecimal.valueOf(1000));
            bh.consume(axonService.deposit(accountId, BigDecimal.TEN));
        }
    }
}
