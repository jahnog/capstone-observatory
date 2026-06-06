## 1. Implement dual-mode (plain + gzip) resource streaming in Extraction

- [x] 1.1 Add the required imports to `Extraction.scala`: `java.io.InputStream` and `java.util.zip.GZIPInputStream`.
- [x] 1.2 Introduce a private helper (e.g. `def openResourceStream(logicalPath: String): InputStream`) that first probes `getClass.getResourceAsStream(logicalPath + ".gz")`, wraps a successful hit in `new GZIPInputStream(...)`, and otherwise falls back to `getClass.getResourceAsStream(logicalPath)`.
- [x] 1.3 Refactor the stations reader creation (`stationsReader = new BufferedSource(...)`) to use the new helper with the supplied `stationsFile` argument.
- [x] 1.4 Refactor the temperatures reader creation (`tempReader = new BufferedSource(...)`) to use the helper with the supplied `temperaturesFile` argument.
- [x] 1.5 Ensure `close()` calls on the `BufferedSource` instances remain in place (they will close the underlying decompressing stream correctly).
- [x] 1.6 Update the Scaladoc for `locateTemperatures` to document that both `"/foo.csv"` and `"/foo.csv.gz"` (and the transparent logical form) are supported.

## 2. Compress the large CSV resources and update version control

- [x] 2.1 For every `.csv` file under `src/main/resources/`, produce a corresponding `.csv.gz` by explicitly invoking the gzip command with maximum compression: `gzip -9 <original.csv> > <original.csv>.gz` (or `gzip --best`). Leave the original uncompressed `.csv` file in place temporarily.
- [x] 2.2 Verify that maximum compression was used (e.g. inspect with `gzip -l *.csv.gz` or compare ratios; the resulting files must reflect level 9 / best compression). Also verify the produced `.gz` files are valid (`gunzip -t *.csv.gz` or `zcat` smoke test) and that their sizes are substantially smaller.
- [x] 2.3 Remove the uncompressed `.csv` files from git tracking (`git rm src/main/resources/*.csv`) while keeping the new `.csv.gz` siblings.
- [x] 2.4 Stage the compressed files (`git add src/main/resources/*.csv.gz`) and confirm the net diff dramatically reduces repository payload.

## 3. Update call sites and ancillary references (minimal)

- [x] 3.1 Inspect `Main.scala` (the three call sites to `locateTemperatures`) and decide whether to leave the logical `"/$year.csv"` names (relying on transparent resolution) or to append `.gz` for explicitness; apply the chosen minimal edit.
- [x] 3.2 Search the rest of the tree (READMEs, scripts, `project/`, `capstone-ui/`, any docs) for hard-coded references to the CSV resource names and update or annotate them only where they would otherwise mislead a reader.
- [x] 3.3 If any example or shell command in the repo previously copied or referenced the raw CSVs, adjust the example to mention the compressed form or the dual support.

## 4. Build, test, and runtime validation against the spec

- [x] 4.1 Execute `sbt clean compile` (with the current Java 21 toolchain) and confirm the project builds with zero new dependency warnings and no compilation errors related to the stream changes.
- [x] 4.2 Run the test suite (`sbt test`) focusing on any extraction or capstone tests; verify that `Extraction.locateTemperatures` and downstream averages produce identical numeric results before vs. after the storage-format change.
- [x] 4.3 Perform a limited end-to-end exercise (e.g. `sbt "runMain observatory.Main"` restricted to one or two years via code temp change, or direct REPL calls) and spot-check that realistic temperature records are still produced and that memory/CPU behavior remains acceptable.
- [x] 4.4 (verification of "either" requirement) Temporarily restore one uncompressed `.csv` (or copy a plain version) while the `.gz` is also present and confirm the loader still succeeds and prefers the compressed form.
- [x] 4.5 Confirm that a deliberately missing resource still yields the historical empty-collection behavior (or a clear error) so the "missing resource" scenario from the spec is satisfied.

## 5. Close-out and documentation of the change

- [x] 5.1 Add a brief note (in a new or existing `data/README` or similar) describing how a maintainer would regenerate the CSVs from source and re-compress them if the underlying NOAA data ever needs to be refreshed.
- [x] 5.2 Run `openspec status --change compress-csv-resources` and ensure all artifacts show the expected "done" state.
- [ ] 5.3 Commit the code changes, the compressed resources, the removal of the large plain CSVs, and the planning artifacts together as the implementation of this change.
