package com.wesleytaumaturgo.cqrs.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.wesleytaumaturgo.cqrs.CqrsBenchmarkApplication;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Boundary enforcement para arquitetura hexagonal.
 *
 * REQ: domain puro, manual e axon isolados, zero ciclos.
 */
@AnalyzeClasses(
    packagesOf = CqrsBenchmarkApplication.class,
    importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {

    /**
     * Regra 1 — Domain isolation.
     * O domínio não deve conhecer nenhuma camada de infraestrutura,
     * nem frameworks (Spring, Axon).
     */
    @ArchTest
    static final ArchRule domain_must_not_depend_on_infra =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
                .resideInAnyPackage(
                    "..manual..",
                    "..axon..",
                    "..config..",
                    "..common..",
                    "org.springframework..",
                    "org.axonframework.."
                )
            .because("domain é puro — zero dependências de infraestrutura, frameworks ou DTOs de adapter");

    /**
     * Regra 2 — Manual não depende de Axon.
     * A implementação manual é independente do Axon Framework.
     */
    @ArchTest
    static final ArchRule manual_must_not_depend_on_axon =
        noClasses()
            .that().resideInAPackage("..manual..")
            .should().dependOnClassesThat()
                .resideInAPackage("..axon..")
            .because("implementação manual é autônoma — não deve conhecer o Axon");

    /**
     * Regra 3 — Axon não depende de Manual.
     * A implementação Axon é independente da implementação manual.
     */
    @ArchTest
    static final ArchRule axon_must_not_depend_on_manual =
        noClasses()
            .that().resideInAPackage("..axon..")
            .should().dependOnClassesThat()
                .resideInAPackage("..manual..")
            .because("implementação Axon é autônoma — não deve conhecer a implementação manual");

    /**
     * Regra 4 — Sem ciclos entre pacotes.
     * Nenhum conjunto de pacotes deve formar dependências circulares.
     */
    @ArchTest
    static final ArchRule no_cycles =
        slices()
            .matching("com.wesleytaumaturgo.cqrs.(*)..")
            .should().beFreeOfCycles()
            .because("ciclos entre pacotes indicam acoplamento incorreto");
}
