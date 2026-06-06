# Observatory CSV Data

This directory contains the station list and yearly temperature observation files used by
`observatory.Extraction` (and the rest of the capstone pipeline).

## Files
- `stations.csv` / `stations.csv.gz` — station metadata
- `YYYY.csv` / `YYYY.csv.gz` — daily temperature records for year YYYY (1975–2015 in the provided set)

## Dual support (plain vs. compressed)
The loader in `Extraction.openResourceStream` prefers the `.gz` sibling when present
(`getClass.getResourceAsStream(logical + ".gz")` first, then falls back to the plain logical name).
This allows:
- Committing only the much smaller `.gz` files (dramatically reduces repo size).
- Local development ergonomics: you may keep an uncompressed `.csv` next to the `.gz` for quick inspection or editing; the program will still use the compressed stream.

## Regeneration and re-compression
If the underlying data ever needs to be refreshed (e.g. from a new NOAA GHCN export or the original
course assignment data drop):

1. Obtain or generate the raw observations in the exact 3-column format the parser expects
   (the historical files use a simple CSV layout that `locateTemperatures` reads via `getLines` + splitting).
2. Name the station file `stations.csv` and yearly files `1975.csv` … `2015.csv` (or the subset you need).
3. Place them in `src/main/resources/`.
4. Create the maximally-compressed gzip siblings **in place**:
   ```sh
   gzip -9 stations.csv          # produces stations.csv.gz (level 9 / best)
   gzip -9 1975.csv 1976.csv ... # repeat for each year, or loop:
   for f in *.csv; do gzip -9 "$f"; done
   ```
   - Use `-9` (or `--best`) to ensure maximum compression as required by the change spec.
   - Leave the original `.csv` files if desired for local use (they can remain untracked).
5. Verify:
   ```sh
   gunzip -t *.csv.gz            # all must report OK
   ls -lh *.csv.gz               # sizes should be substantially smaller (≈75-80% reduction typical)
   ```
6. Update git:
   - `git add src/main/resources/*.csv.gz src/main/resources/README.md`
   - `git rm --cached src/main/resources/*.csv` (if they were ever tracked)
   - Do **not** commit the large plain `.csv` files.

## Notes
- The provided `.csv.gz` files were produced with `gzip -9` and pass `gunzip -t`.
- Both the Scala `java.util.zip.GZIPInputStream` (used at runtime) and standard `gunzip`/`zcat` can read them.
- Compression happens offline; there is no runtime cost beyond normal streaming decompression.
- If you only need a subset of years for development, you can keep a minimal set of `.gz` files; missing years will result in the historical empty-collection (or clear error) behavior from `locateTemperatures`.

See also the OpenSpec change `compress-csv-resources` for the full rationale and implementation details.
