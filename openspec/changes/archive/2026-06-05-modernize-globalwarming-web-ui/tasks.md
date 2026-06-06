## 1. Establish the shared observatory shell

- [x] 1.1 Create shared frontend assets for typography, color tokens, atmospheric backgrounds, layout surfaces, and responsive shell behavior that can be reused across the GlobalWarming pages.
- [x] 1.2 Define stable visualization host regions and shared shell markup conventions that can wrap both the existing Leaflet map view and the Scala.js-driven climate UI without changing their core behavior.

## 2. Refactor the browser entry pages

- [x] 2.1 Update `index.html` to use the new observatory shell while preserving its current Scala.js bundle loading and analytics integration.
- [x] 2.2 Update `interaction.html` and `interaction2.html` to use the shared shell, preserve their current map or visualization rendering, and keep required third-party browser assets available.

## 3. Validate behavior and deployment simplicity

- [x] 3.1 Verify the refreshed landing and interactive pages at desktop and narrow viewport sizes, confirming that the main visualization remains visible and usable.
- [x] 3.2 Confirm the refreshed pages still work with the current static-host deployment model, relative asset paths, analytics hooks, and map-library integrations intact.