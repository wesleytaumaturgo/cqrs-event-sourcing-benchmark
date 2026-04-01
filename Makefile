.PHONY: help build test run coverage clean docker-up docker-down benchmark report owasp

MAVEN := $(shell command -v ./mvnw 2>/dev/null || echo mvn)

help: ## Mostra esta ajuda
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

build: ## Compila o projeto (sem testes)
	$(MAVEN) clean package -DskipTests

test: ## Roda todos os testes com cobertura
	$(MAVEN) verify

run: docker-up ## Sobe PostgreSQL e inicia a aplicação localmente
	$(MAVEN) spring-boot:run

coverage: ## Gera relatório de cobertura JaCoCo em target/site/jacoco/
	$(MAVEN) verify jacoco:report

clean: ## Limpa artefatos de build
	$(MAVEN) clean

docker-up: ## Sobe PostgreSQL via docker-compose
	docker compose up -d postgres

docker-down: ## Para e remove containers
	docker compose down

benchmark: docker-up ## Compila e executa benchmarks JMH (gera target/jmh-result.json)
	$(MAVEN) verify -P benchmark

owasp: ## Auditoria de vulnerabilidades OWASP (gera target/dependency-check/dependency-check-report.html)
	$(MAVEN) verify -P owasp -DskipTests

report: ## Gera relatório comparativo a partir do jmh-result.json
	@echo "Relatório: target/jmh-result.json"
	@test -f target/jmh-result.json && cat target/jmh-result.json | python3 -m json.tool || echo "Execute 'make benchmark' primeiro."
