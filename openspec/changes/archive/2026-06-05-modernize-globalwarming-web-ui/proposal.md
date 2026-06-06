## Why

The GlobalWarming browser experience still uses a minimal assignment-era HTML shell, which makes the project feel dated next to the recently refreshed BarnesHut site and leaves the interactive map views without a coherent modern presentation. This change is needed now to give the project a cleaner public-facing experience while keeping the implementation lightweight and aligned with the existing Scala.js and Leaflet stack.

## What Changes

- Add a modern observatory-style web shell for the GlobalWarming pages, with a clearer visual hierarchy, responsive layout, and a more polished atmosphere.
- Introduce shared frontend styling and lightweight page bootstrapping so the landing page and interactive map views feel like one product instead of separate assignment artifacts.
- Preserve the current Scala.js application logic, Leaflet-based map rendering, and analytics integration while simplifying the browser-side structure around them.
- Prefer the smallest practical framework footprint, favoring static HTML, shared CSS, and a minimal build/dev layer over a component-heavy frontend rewrite.

## Capabilities

### New Capabilities
- `modern-climate-observatory-ui`: The web experience presents GlobalWarming through a modern, responsive observatory-style interface that wraps the existing interactive climate visualizations without changing their core scientific behavior.

### Modified Capabilities
- None.

## Impact

- Affected browser entry points such as `index.html`, `interaction.html`, and `interaction2.html`.
- Affected frontend assets including shared styles, lightweight page scripts, and any new minimal tooling required to serve or bundle the refreshed shell.
- Affected Scala.js UI integration points where the new page structure must host the existing map and visualization surfaces.
- No expected changes to the underlying climate data processing pipeline or map tile generation logic.