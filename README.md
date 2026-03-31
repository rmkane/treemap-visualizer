# treemap-visualizer

Generate dependency-size visualizations (and exports) for Maven projects.

The tool runs `mvn dependency:tree`, resolves artifact sizes from local `.m2`, and produces:

- visual outputs: PNG, HTML (interactive SVG)
- data outputs: JSON, YAML

## Current status

This repository is currently a **CLI application**.

It is intentionally structured so it can transition into a **Maven plugin** next:

- format-specific generation via `Generator` implementations
- shared `OutputOptions`
- output path defaults derived from the analyzed project directory name

## Requirements

- Java 21
- Maven 3.9+

## Build

```bash
mvn clean package
```

This creates a shaded executable JAR in `target/`.

## Usage

```bash
java -jar target/treemap-visualizer-0.1.0-SNAPSHOT.jar [projectDir] [options]
```

`projectDir` defaults to current directory (`.`).

### Common options

- `--format <png|html|json|yaml>`
- `-o, --output <path>`
- `--width <px>` (visual formats)
- `--height <px>` (visual formats)
- `--refresh` (bypass dependency-tree cache)
- `--cache-dir <path>`

If `--output` is omitted, the default is:

```text
target/treemap-<projectName>.<ext>
```

Examples:

```bash
java -jar target/treemap-visualizer-0.1.0-SNAPSHOT.jar --format html
java -jar target/treemap-visualizer-0.1.0-SNAPSHOT.jar --format json
java -jar target/treemap-visualizer-0.1.0-SNAPSHOT.jar /path/to/project --format png -o /tmp/deps.png
```

## Caching behavior

Dependency tree output is cached by SHA-256 of `pom.xml`.

- hit: skips re-running `mvn dependency:tree`
- miss: runs Maven and writes cache entry
- `--refresh`: forces miss behavior and updates cache

## Logging and timings

The app logs via Logback with daily rolling logs under `logs/`.

Per run, timing logs include:

- dependency tree resolution time
- local repository resolution time
- parse + artifact-size enrichment time
- output generation time
- total generation time

## Make targets

```bash
make build
make test
make format
make lint
make output-all
make output-html
make output-png
make output-json
make output-yaml
```

Default output files are generated into `target/` and named with a `treemap-` prefix plus project directory name.

## Formatting and CI

- Spotless is configured in `pom.xml` (Eclipse formatter profile from `formatter.xml`).
- GitHub Actions CI (`.github/workflows/ci.yml`) runs on JDK 21 and executes:
  - `make lint`
  - `mvn -B verify`

## Planned transition to Maven plugin

The intended next step is a plugin goal (for example `treemap:generate`) that:

- reuses existing generators (`PngRenderer`, `HtmlRenderer`, `JsonExporter`, `YamlExporter`)
- binds outputs into the Maven lifecycle (e.g. `verify`)
- writes generated artifacts into `${project.build.directory}`
- supports plugin configuration mirroring current CLI options

That should make generation reproducible in standard Maven builds and CI without invoking the JAR manually.
