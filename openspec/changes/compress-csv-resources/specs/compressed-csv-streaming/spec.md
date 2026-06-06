## ADDED Requirements

### Requirement: Extraction can load stations and temperature data from gzip-compressed resources
The `Extraction.locateTemperatures` method SHALL successfully read stations and yearly temperature CSV data when the corresponding resources on the classpath are stored in gzip-compressed form (`.csv.gz` suffix) using on-the-fly streaming decompression.

#### Scenario: Compressed resources only (repository normal case)
- **WHEN** only the gzip-compressed files (`/stations.csv.gz`, `/1975.csv.gz`, ...) are present as classpath resources
- **THEN** `locateTemperatures(1975, "/stations.csv", "/1975.csv")` (or the `.gz` variants) returns the full set of parsed `(LocalDate, Location, Double)` records without error

### Requirement: Committed resources use maximum gzip compression
The `.csv.gz` files that are committed to the repository and packaged with the project SHALL have been produced using the highest gzip compression level (command-line equivalent of `gzip -9` or `gzip --best`). This ensures the smallest possible artifact size while still allowing streaming decompression at runtime.

#### Scenario: Maximum compression is applied to repository artifacts
- **WHEN** the CSV resources are compressed for inclusion in the repository (during the change implementation)
- **THEN** the resulting `.csv.gz` files reflect maximum gzip compression (verifiable via `gzip -l` or equivalent) and the implementation tasks explicitly require use of `-9` / `--best`.

#### Scenario: Uncompressed resources only (developer / debug case)
- **WHEN** only the plain uncompressed CSV files (`/stations.csv`, `/1975.csv`, ...) are present as classpath resources
- **THEN** `locateTemperatures` returns the identical parsed records as it would from the compressed form

#### Scenario: Both forms present (preference for compressed)
- **WHEN** both `/1975.csv` and `/1975.csv.gz` exist on the classpath for the same logical year
- **THEN** the implementation prefers the compressed `.gz` variant and still produces correct records (the uncompressed copy is ignored)

#### Scenario: Missing resource
- **WHEN** neither the plain nor the `.gz` form of a requested resource can be found via `getResourceAsStream`
- **THEN** `locateTemperatures` yields an empty collection (current observed behavior for a null stream) or surfaces a clear failure consistent with prior error handling

### Requirement: Streaming decompression is used (no full-file materialization required)
The loader SHALL obtain an `InputStream` that performs gzip decompression incrementally as bytes are consumed by `BufferedSource.getLines()`, rather than decompressing the entire file into memory up front.

#### Scenario: Large yearly file is processed without excessive heap usage
- **WHEN** `locateTemperatures` is called for a year whose (compressed) CSV is tens of megabytes on disk
- **THEN** peak memory during the load phase remains proportional to a few lines/buffers plus the stations map, not to the fully expanded CSV size

### Requirement: Public API and observable results are unchanged
Callers of `Extraction.locateTemperatures` and `locationYearlyAverageRecords` SHALL observe no change in method signatures, return types, or the set and values of returned records when switching between storage forms of the underlying CSV resources.

#### Scenario: Existing call sites continue to work
- **WHEN** code written against the pre-change API (e.g. `Main.scala` lines that pass `"/stations.csv"` and `s"/$year.csv"`) is executed after the change
- **THEN** the calls compile and produce the same temperature records and yearly averages as before

### Requirement: No new runtime dependencies for compression support
The implementation of compressed CSV support SHALL introduce zero new entries in `libraryDependencies` and SHALL rely exclusively on classes already present in the Java runtime (`java.util.zip.GZIPInputStream`, `java.io.InputStream`, etc.).

#### Scenario: Project still compiles and runs with only the original declared dependencies
- **WHEN** `sbt compile` and `sbt run` (or test) are executed after the change lands
- **THEN** the build succeeds without requiring additional artifacts to be resolved for gzip handling
