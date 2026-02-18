.PHONY: help up down build run test clean logs ps

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ── Infrastructure ──────────────────────────────────────

up: ## Start infra (Postgres, Redis, Redpanda)
	docker compose -f infra/docker-compose.yml up -d

down: ## Stop infra
	docker compose -f infra/docker-compose.yml down

ps: ## Show running containers
	docker compose -f infra/docker-compose.yml ps

logs: ## Tail infra logs
	docker compose -f infra/docker-compose.yml logs -f

# ── Build & Run ─────────────────────────────────────────

build: ## Full build + tests
	./gradlew build

run: ## Run the application (requires infra)
	./gradlew :app:bootRun

test: ## Run all tests
	./gradlew test

test-integration: ## Run integration tests only
	./gradlew :app:test

test-unit: ## Run unit tests only (excludes integration)
	./gradlew test -x :app:test

clean: ## Clean build artifacts
	./gradlew clean

# ── Frontend ────────────────────────────────────────────

ui-install: ## Install React UI dependencies
	cd modules/ui-react && npm install

ui-dev: ## Start React UI dev server
	cd modules/ui-react && npm run dev

ui-build: ## Build React UI for production
	cd modules/ui-react && npm run build

# ── Database ────────────────────────────────────────────

db-reset: ## Reset database (drop + recreate)
	docker compose -f infra/docker-compose.yml exec postgres psql -U moc -d postgres -c "DROP DATABASE IF EXISTS magiconcall; CREATE DATABASE magiconcall;"
	@echo "Database reset. Run 'make run' to apply migrations."

# ── Kafka ───────────────────────────────────────────────

kafka-topics: ## List Kafka topics
	docker compose -f infra/docker-compose.yml exec redpanda rpk topic list

kafka-create-topics: ## Create default Kafka topics
	docker compose -f infra/docker-compose.yml exec redpanda rpk topic create moc.alert.alert_created --partitions 3
