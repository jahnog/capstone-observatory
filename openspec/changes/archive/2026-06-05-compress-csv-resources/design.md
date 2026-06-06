## Context

The data extraction layer (`observatory.Extraction`) loads large CSV files (one per year 1975-2015 plus `stations.csv`) from the classpath via `getClass.getResourceAsStream(...)` followed by `scala.io.BufferedSource` and `.getLines()`. These files total tens of megabytes uncompressed and dominate the size of the repository and any packaged artifacts. The project targets Scala 2.12 on Java 21 (post recent compatibility change) with no appetite for new heavy dependencies. Callers (Main, tests, grading harness) pass resource paths such as `"/stations.csv"` and `s"/$year.csv"`.

## Goals / Non-Goals

**Goals:**
- Reduce on-disk and in-repo size of the CSV data by a large factor (gzip typically achieves 70-85% for this tabular numeric data) while keeping load-time memory usage low.
- Support transparent / dual-mode reading: the same logical path argument should resolve to either an on-disk `.csv` (for easy editing) or `.csv.gz` (for production/repo) using streaming decompression.
- Use only JDK built-ins (`java.util.zip.GZIPInputStream`) — zero new libraryDependencies.
- Preserve exact observable behavior for `locateTemperatures` and `locationYearlyAverageRecords` (same records, same order characteristics, same error handling).
- Keep the change minimal and localized to resource acquisition + one helper.

**Non-Goals:**
- Do not introduce a general-purpose "resource loader" abstraction or change other parts of the observatory pipeline.
- Do not change file formats, add indexes, or switch to binary/Parquet (would be overkill and break the educational intent of the milestone).
- Do not require callers to change every call site if a simple helper + convention can keep most paths identical.
- Do not add build-time compression plugins or sbt tasks unless they prove necessary for developer ergonomics; manual or simple script compression is acceptable for this data.

## Decisions

**Decision: Use gzip (not zstd, bzip2, lz4, or custom Deflater streams)**
- Rationale: `java.util.zip.GZIPInputStream` is in the JDK since 1.1, requires no dependency, supports true streaming (read as you go, no full buffer), and gives excellent ratio on CSV. Higher-ratio formats like bzip2 or zstd would require external jars (commons-compress, zstd-jni) that would bloat the project and complicate the student submission/grading environment.
- Alternatives considered: Snappy/LZ4 (faster but worse ratio), pure Deflater (no header, less interoperable), embedding a tiny decompressor. Rejected for complexity vs. benefit.

**Decision: Prefer `.csv.gz` when present, fall back to plain `.csv` for the same logical name**
- Given a path like `"/1975.csv"`, the loader first probes `getResourceAsStream("/1975.csv.gz")`; if that resource exists, wrap the resulting stream in `GZIPInputStream`. Otherwise fall back to the original path.
- This allows:
  - Repo to ship only the `.gz` files.
  - A developer to temporarily drop an uncompressed copy next to it for inspection/editing without changing any call sites.
- Call sites can stay exactly as written (`"/stations.csv"`) or can be updated to the `.gz` form; both will work after the helper lands.
- The helper will be private to Extraction or a small package object utility.

**Decision: Wrap at the InputStream level before constructing BufferedSource**
- `new BufferedSource(new GZIPInputStream(rawIs))` works because GZIPInputStream is a proper InputStream and `getLines()` will see the decompressed bytes on demand.
- No need to change any line-parsing logic, date math, or the fold/groupBy code.
- Resource lookup stays via `getClass.getResourceAsStream` (works for both files inside the jar and in the filesystem during `sbt run` / IDE).

**Decision: Keep the public method signatures unchanged**
- `locateTemperatures(year, stationsFile: String, temperaturesFile: String)` signature and semantics stay identical.
- Only internal stream acquisition changes. This minimizes blast radius on tests and the grading harness.

**Decision: Compression performed offline using maximum gzip compression (`gzip -9` / `--best`) and checked in**
- At implementation time we MUST explicitly compress every CSV using the highest gzip compression level: `gzip -9` (or `gzip --best`). The resulting `.csv.gz` files are then `git add`ed and the original plain `.csv` files are `git rm`ed.
- No runtime compression; data is static.
- A small note or script can be added under `data/` or in README for future regeneration if the source CSVs ever need to be updated from the original NOAA dumps. The regeneration instructions must also specify use of `-9` / maximum compression.

## Risks / Trade-offs

- [Risk: Some environments / classloaders may report the `.gz` resource differently] → Mitigation: the dual-probe logic is defensive; we also keep the plain-name fallback. Manual verification on `sbt run` and `sbt test` will be part of the task.
- [Risk: GZIPInputStream can throw IOException on corrupt data; current code has minimal error handling] → Mitigation: wrap creation in a small try or let the existing exception surface (same as today for a missing file). Add a clear message.
- [Trade-off: Slightly higher CPU on first read vs. uncompressed] → Acceptable; the data is read once per run and the CPU cost of gzip is negligible compared to the later grid/tile work. Memory savings and startup I/O wins dominate.
- [Trade-off: Binary `.gz` files are less pleasant to `git diff`] → Irrelevant for these data files; they were never meant to be diffed line-by-line. `.gitattributes` can mark them as binary if desired (optional polish).
- [Risk: Submission / capstone packaging steps may hard-code expectations about resource file names] → Mitigation: because the change keeps the logical names and the loader is inside the student code, the harness that calls `Extraction.locateTemperatures` should continue to work unchanged. We will verify against the project/StudentBuild* helpers if they copy resources.
