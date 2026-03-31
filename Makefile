.DEFAULT_GOAL := help

MVN ?= mvn
# Resolves to target/<artifactId>-<version>.jar; expanded only when referenced (e.g. make run).
JAR = target/$(shell $(MVN) -q help:evaluate -Dexpression=project.build.finalName -DforceStdout).jar

.PHONY: help build clean test run jar-path

build:
	@echo "Building the project..."
	@$(MVN) clean package

clean:
	@echo "Cleaning the project..."
	@$(MVN) clean

test:
	@echo "Testing the project..."
	@$(MVN) test

run:
	@echo "Running the project..."
	@j=$(JAR); test -f "$$j" || { echo "No JAR at $$j; run 'make build' first." >&2; exit 1; }; java -jar "$$j"

# Handy for scripts: make -s jar-path
jar-path:
	@echo $(JAR)

help:
	@echo "Usage: make <target>"
	@echo "Targets:"
	@echo "  build    - Build the project"
	@echo "  clean    - Clean the project"
	@echo "  test     - Run tests"
	@echo "  run      - Run the packaged JAR (requires build first)"
	@echo "  jar-path - Print resolved JAR path (from pom finalName)"
	@echo "  help     - Show this help message"
