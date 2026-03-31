package com.wesleytaumaturgo.cqrs.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * B5 — Custo de Complexidade (LOC, classes, boilerplate).
 *
 * Mede estaticamente o tamanho de cada implementação:
 * - LOC (linhas de código fonte, excluindo comentários e linhas em branco)
 * - Número de arquivos Java
 *
 * Como é análise estática, o @Benchmark apenas retorna as métricas
 * para que apareçam no jmh-result.json.
 * REQ-8.EARS-1
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(value = 1, jvmArgs = {"-Xms256m", "-Xmx512m"})
@State(Scope.Benchmark)
public class ComplexityCostAnalysis {

    private static final String SRC_ROOT = "src/main/java/com/wesleytaumaturgo/cqrs";

    private int manualLoc;
    private int axonLoc;
    private int manualFiles;
    private int axonFiles;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        manualLoc   = countLoc(Paths.get(SRC_ROOT, "manual"));
        axonLoc     = countLoc(Paths.get(SRC_ROOT, "axon"));
        manualFiles = countFiles(Paths.get(SRC_ROOT, "manual"));
        axonFiles   = countFiles(Paths.get(SRC_ROOT, "axon"));
    }

    /**
     * B5-Manual: retorna LOC da implementação Manual (excluindo linhas em branco e comentários).
     * REQ-8.EARS-1
     */
    @Benchmark
    public int manualComplexity(Blackhole bh) {
        bh.consume(manualFiles);
        return manualLoc;
    }

    /**
     * B5-Axon: retorna LOC da implementação Axon (excluindo linhas em branco e comentários).
     * REQ-8.EARS-1
     */
    @Benchmark
    public int axonComplexity(Blackhole bh) {
        bh.consume(axonFiles);
        return axonLoc;
    }

    // ── Utilitários ────────────────────────────────────────────────────────────

    /** Conta linhas de código (não em branco, não de comentário de bloco/linha). */
    private static int countLoc(Path dir) throws IOException {
        if (!dir.toFile().exists()) return 0;
        try (Stream<Path> files = Files.walk(dir)) {
            return files
                .filter(p -> p.toString().endsWith(".java"))
                .mapToInt(ComplexityCostAnalysis::locInFile)
                .sum();
        }
    }

    private static int locInFile(Path file) {
        try {
            return (int) Files.lines(file)
                .map(String::trim)
                .filter(l -> !l.isEmpty() && !l.startsWith("//") && !l.startsWith("*") && !l.startsWith("/*"))
                .count();
        } catch (IOException e) {
            return 0;
        }
    }

    /** Conta arquivos Java no diretório (recursivo). */
    private static int countFiles(Path dir) throws IOException {
        if (!dir.toFile().exists()) return 0;
        try (Stream<Path> files = Files.walk(dir)) {
            return (int) files
                .filter(p -> p.toString().endsWith(".java"))
                .count();
        }
    }
}
