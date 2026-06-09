# Global Warming Observatory

Spatial visualization of four decades of global temperature data (1975–2015). The
project ingests historical weather-station records, spatially interpolates them
across the globe, computes each region's deviation from a long-run baseline, and
renders the results as colour-coded, zoomable map tiles — served through an
interactive Scala.js / Leaflet web viewer.

**[▶ Live demo](https://jnbigdatatemp.s3.amazonaws.com/index.html)** ·
**[Write-up](https://jahnog.github.io/global-warming-analysis/)**

## What it does

- **Extraction** — parses raw station metadata and yearly temperature CSVs into
  located temperature records.
- **Visualization** — interpolates temperatures over the sphere (great-circle
  distance + inverse-distance weighting), maps values to a colour scale, and
  produces world images with [scrimage](https://github.com/sksamuel/scrimage).
- **Manipulation** — builds temperature grids and computes deviations against a
  baseline period; a k-d tree accelerates nearest-neighbour interpolation.
- **Interaction / TileGeneration** — slices the world into web-map tiles
  (`<zoom>/<x>-<y>.png`) for both the temperature and deviation layers, with a
  resumable, low-priority generation workflow (see [`TILE_GENERATION.md`](TILE_GENERATION.md)).
- **capstone-ui** — a Scala.js front end that overlays the generated tiles on a
  Leaflet map and lets you step through the years.

## Tech stack

Scala 2.12 · sbt · Scala.js · Leaflet · scrimage · ScalaTest / ScalaCheck

## Build & run

```bash
# compile and run the test suite
sbt test

# generate map tiles into target/{temperatures,deviations}/<year>/<zoom>/<x>-<y>.png
sbt "runMain observatory.Main"

# compile the Scala.js front end
./compilejs.sh
```

Tile generation is configurable via environment variables (year ranges, zoom,
layers, output root) — see [`TILE_GENERATION.md`](TILE_GENERATION.md) for the full
list and a small "smoke run" example.

Requires a JDK (tested on Java 21) and sbt.

## Origin

This started as the capstone project of the *Functional Programming in Scala*
specialization (EPFL) and has since been reworked into a standalone, modernized
climate-visualization project.
