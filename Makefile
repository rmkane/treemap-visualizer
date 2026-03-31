.DEFAULT_GOAL := help

MVN ?= mvn
# Resolves to target/<artifactId>-<version>.jar; expanded for treemap-html / treemap-png / treemap-jar-path.
JAR = target/$(shell $(MVN) -q help:evaluate -Dexpression=project.build.finalName -DforceStdout).jar
ARTIFACT_ID = $(shell $(MVN) -q help:evaluate -Dexpression=project.artifactId -DforceStdout)

HTML_OUT ?= target/$(ARTIFACT_ID).html
PNG_OUT ?= target/$(ARTIFACT_ID).png
JSON_OUT ?= target/$(ARTIFACT_ID).json
YAML_OUT ?= target/$(ARTIFACT_ID).yaml

.PHONY: help build clean test format lint output-all output-html output-png output-json output-yaml jar-path

build:
	@echo "Building the project..."
	@$(MVN) clean package

clean:
	@echo "Cleaning the project..."
	@$(MVN) clean

test:
	@echo "Testing the project..."
	@$(MVN) test

format:
	@echo "Formatting code..."
	@$(MVN) spotless:apply

lint:
	@echo "Checking formatting/lint..."
	@$(MVN) spotless:check

output-all: output-html output-png output-json output-yaml
	@echo "Wrote $(HTML_OUT), $(PNG_OUT), $(JSON_OUT), $(YAML_OUT)"

output-html:
	@echo "Generating $(HTML_OUT)..."
	@j=$(JAR); test -f "$$j" || { echo "No JAR at $$j; run 'make build' first." >&2; exit 1; }; java -jar "$$j" -o $(HTML_OUT)

output-png:
	@echo "Generating $(PNG_OUT)..."
	@j=$(JAR); test -f "$$j" || { echo "No JAR at $$j; run 'make build' first." >&2; exit 1; }; java -jar "$$j" -o $(PNG_OUT)

output-json:
	@echo "Generating $(JSON_OUT)..."
	@j=$(JAR); test -f "$$j" || { echo "No JAR at $$j; run 'make build' first." >&2; exit 1; }; java -jar "$$j" -o $(JSON_OUT)

output-yaml:
	@echo "Generating $(YAML_OUT)..."
	@j=$(JAR); test -f "$$j" || { echo "No JAR at $$j; run 'make build' first." >&2; exit 1; }; java -jar "$$j" -o $(YAML_OUT)

jar-path:
	@echo $(JAR)

help:
	@echo "Usage: make <target>"
	@echo ""
	@echo "Targets:"
	@echo "  build           - Build the project (fat JAR)"
	@echo "  clean           - Clean the project"
	@echo "  test            - Run tests"
	@echo "  format          - Apply Spotless formatting"
	@echo "  lint            - Check Spotless formatting"
	@echo "  output-all      - Generate HTML, PNG, JSON, and YAML (see *_OUT vars)"
	@echo "  output-html     - Write $(HTML_OUT) (override: HTML_OUT=path)"
	@echo "  output-png      - Write $(PNG_OUT) (override: PNG_OUT=path)"
	@echo "  output-json     - Write $(JSON_OUT) (override: JSON_OUT=path)"
	@echo "  output-yaml     - Write $(YAML_OUT) (override: YAML_OUT=path)"
	@echo "  jar-path        - Print resolved JAR path (from pom finalName)"
	@echo "  help            - Show this help message"
