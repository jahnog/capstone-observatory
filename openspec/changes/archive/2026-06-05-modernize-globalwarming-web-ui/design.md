## Context

GlobalWarming currently exposes its browser experience through standalone HTML files with inline styles, direct Leaflet CDN usage, and script tags that load the existing Scala.js output. That keeps the project simple, but it also means the landing page and interactive views feel disconnected and visually dated compared with the recently modernized BarnesHut observatory interface. The refresh needs to improve visual quality and cohesion without turning a static, hostable project into a frontend framework migration.

## Goals / Non-Goals

**Goals:**
- Give the project a modern observatory-style visual shell that feels consistent with BarnesHut while still fitting the climate domain.
- Unify the landing page and interactive pages through shared layout primitives, design tokens, and responsive behavior.
- Preserve the current Scala.js application logic, Leaflet rendering, and analytics integration.
- Keep the browser stack lightweight enough to remain easy to host and maintain.

**Non-Goals:**
- Rewriting the climate visualization logic or data-generation pipeline.
- Introducing a heavyweight component framework such as React, Vue, or Angular.
- Changing the scientific outputs, map tile generation, or interaction semantics beyond what is needed to fit the new shell.

## Decisions

### Use a framework-free presentation layer first
The refresh will be built from shared static HTML structure, a common CSS file, and only the smallest amount of page bootstrapping JavaScript needed to attach the existing interactive surfaces. This keeps the implementation close to the current deployment model while still allowing a modern visual treatment.

Alternative considered: introduce a Vite app that mirrors BarnesHut more directly. Rejected as the primary approach because the GlobalWarming pages do not need a component runtime to achieve the desired visual refresh, and a bundler would add maintenance overhead before it adds product value.

### Define a shared observatory shell contract across entry pages
The browser pages will adopt a common shell with a branded header, atmospheric background treatment, a primary visualization stage, and supporting metadata or control surfaces. Each page can vary its content inside that shell, but the layout vocabulary and styling tokens will be shared.

Alternative considered: redesign each HTML page independently. Rejected because it would duplicate styling decisions and make the experience drift again over time.

### Wrap existing visualization hosts instead of replacing them
The current Leaflet map container and Scala.js mount points will be moved into dedicated host regions inside the new shell. The design assumes the page chrome changes around the visualization, not that the visualization engine itself is reimplemented.

Alternative considered: rebuild the interactive browser code around a new frontend architecture. Rejected because it would expand scope into application behavior and create unnecessary regression risk.

### Keep assets static-host friendly and centralized
Shared visual assets such as typography, gradients, spacing, and shell behavior will live in reusable browser assets rather than inline page-local code. If a small dev helper is needed, it should remain optional and must not turn the deployed site into a runtime-dependent single-page application.

Alternative considered: keep all styling inline per page. Rejected because the same modernization work would then be harder to maintain and nearly impossible to keep consistent.

## Risks / Trade-offs

- Existing Scala.js and Leaflet bootstrapping may assume a full-page host element. → Mitigation: introduce stable host container IDs and adapt the shell around those IDs rather than changing the rendering logic deeply.
- Multiple HTML entry points can still drift if they hand-roll markup. → Mitigation: move shared styling and any repeated shell initialization into common assets.
- A visual shell inspired by BarnesHut could feel mismatched if copied too literally. → Mitigation: reuse the same level of polish and structure while shifting the palette and imagery toward climate and cartography rather than space.
- External CDN dependencies can complicate long-term reliability. → Mitigation: preserve current dependencies for now, but keep the new shell compatible with future local vendoring if needed.

## Migration Plan

1. Extract the common visual language into shared CSS and minimal page bootstrap assets.
2. Refactor `index.html`, `interaction.html`, and `interaction2.html` to consume the shared shell and define stable visualization host regions.
3. Adjust page-specific scripts or Scala.js integration points so the existing map and climate UI attach inside the new layout without changing their behavior.
4. Validate the updated pages in a browser for layout consistency, responsive behavior, and preserved interactive rendering.

## Open Questions

- Should the landing page stay mostly informational, or should it expose a lightweight live preview of the visualization shell?
- Is the existing CDN-based Leaflet dependency acceptable for the refreshed pages, or should local asset vendoring be bundled into a later follow-up change?
- Does the project want a tiny local preview workflow for the static pages, or is direct file/static-host validation sufficient?