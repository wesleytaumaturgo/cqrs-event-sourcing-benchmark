.PHONY: help build test coverage lint clean benchmark report

help: ## Mostra esta ajuda
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

build: ## Compila o projeto
	./mvnw clean package -DskipTests

test: ## Roda todos os testes
	./mvnw test

coverage: ## Gera relatório de cobertura (JaCoCo)
	./mvnw test jacoco:report

lint: ## Verificação estática (Checkstyle)
	./mvnw checkstyle:check

clean: ## Limpa artefatos de build
	./mvnw clean

benchmark: ## Executa todos os cenários de benchmark
	./mvnw spring-boot:run -Dspring.profiles.active=benchmark

report: ## Gera relatório comparativo (markdown + CSV)
	./mvnw spring-boot:run -Dspring.profiles.active=report
