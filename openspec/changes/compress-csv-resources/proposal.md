## Why

The CSV resource files (yearly temperature data 1975-2015 plus stations) in `src/main/resources` are very large, significantly inflating the repository size, slowing clones, builds, CI, and downloads for users. This change is needed now to reduce storage/transfer costs while preserving efficient streaming access to the data at runtime without materializing full uncompressed copies in memory.

## What Changes

- Compress all CSV files in `src/main/resources` using **maximum gzip compression** (`gzip -9` or `gzip --best` — the best practical compression method that supports true streaming on-the-fly decompression via the JDK's `GZIPInputStream` with no additional dependencies or full-file buffering).
- Modify the data loading code in `Extraction` to detect and read either an uncompressed `.csv` or a compressed `.csv.gz` resource for the same logical name, wrapping the stream appropriately.
- Update call sites and documentation as needed; the change should be transparent so that existing call patterns continue to work (or require only name suffix updates).
- Remove the large uncompressed CSVs from version control after compression.

## Capabilities

### New Capabilities
- `compressed-csv-streaming`: The extraction pipeline supports reading large CSV resource files directly in gzip-compressed form using streaming decompression (`java.util.zip.GZIPInputStream`) so that the program can consume either the uncompressed or compressed variant without changing higher-level logic.

### Modified Capabilities
- None.

## Impact

- `src/main/resources/` (all `*.csv` files replaced by `*.csv.gz`).
- `src/main/scala/observatory/Extraction.scala` (resource stream acquisition and `BufferedSource` creation).
- `src/main/scala/observatory/Main.scala` (call sites that pass resource paths).
- Repository size, `.git` history (after filter-branch or fresh clone), packaging/submission artifacts, and any test or grading harness that expects the resources.
- No new library dependencies; relies only on `java.util.zip` already present in the JDK.
