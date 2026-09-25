.PHONY: help package verify verify-platform verify-flutter check-contracts \
	local-up local-down local-build local-restart local-logs local-ps \
	dev-up dev-down dev-build dev-restart dev-logs dev-ps

help:
	@echo "Backend:"
	@echo "  make package          - mvn package -DskipTests (build all service jars)"
	@echo "  make verify           - mvn verify (compile + unit + integration tests, all modules)"
	@echo "  make verify-platform  - scripts/verify-platform.sh (Postgres/Kafka isolation + smoke)"
	@echo "  make check-contracts  - scripts/check-contracts.py (OpenAPI + Kafka schema sanity)"
	@echo "  make verify-flutter   - scripts/verify-flutter.sh (user_app analyze + test)"
	@echo ""
	@echo "Local stack (infra/local, :latest images, full app stack):"
	@echo "  make local-up         - build + start everything, detached"
	@echo "  make local-down       - stop and remove containers (keeps volumes)"
	@echo "  make local-restart    - local-down then local-up"
	@echo "  make local-build      - rebuild images without starting"
	@echo "  make local-logs       - follow logs for all local services"
	@echo "  make local-ps         - show local service status"
	@echo ""
	@echo "Dev stack (infra/dev, pinned versions, full app stack):"
	@echo "  make dev-up           - build + start everything, detached"
	@echo "  make dev-down         - stop and remove containers (keeps volumes)"
	@echo "  make dev-restart      - dev-down then dev-up"
	@echo "  make dev-build        - rebuild images without starting"
	@echo "  make dev-logs         - follow logs for all dev services"
	@echo "  make dev-ps           - show dev service status"

package:
	mvn -B -f backend/pom.xml package -DskipTests

verify:
	mvn -B -f backend/pom.xml verify

verify-platform:
	sh scripts/verify-platform.sh

check-contracts:
	python3 scripts/check-contracts.py

verify-flutter:
	sh scripts/verify-flutter.sh

local-up:
	docker compose -f infra/local/compose.yaml up -d --build

local-down:
	docker compose -f infra/local/compose.yaml down

local-restart: local-down local-up

local-build:
	docker compose -f infra/local/compose.yaml build

local-logs:
	docker compose -f infra/local/compose.yaml logs -f

local-ps:
	docker compose -f infra/local/compose.yaml ps

dev-up:
	docker compose -f infra/dev/compose.yaml up -d --build

dev-down:
	docker compose -f infra/dev/compose.yaml down

dev-restart: dev-down dev-up

dev-build:
	docker compose -f infra/dev/compose.yaml build

dev-logs:
	docker compose -f infra/dev/compose.yaml logs -f

dev-ps:
	docker compose -f infra/dev/compose.yaml ps
