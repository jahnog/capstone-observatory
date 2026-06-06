## ADDED Requirements

### Requirement: Shared observatory shell across climate pages
The system SHALL present the primary GlobalWarming browser pages within a shared observatory-style shell that provides consistent branding, typography, atmospheric styling, and layout structure.

#### Scenario: shared shell appears across entry pages
- **WHEN** a user opens any primary GlobalWarming browser entry page
- **THEN** the page shows the same core shell language instead of page-local ad hoc styling

### Requirement: Interactive climate views remain usable inside the refreshed shell
The system SHALL host the existing Leaflet and Scala.js climate visualizations inside the modern shell without removing their current interactive behavior.

#### Scenario: interactive visualization loads in the refreshed shell
- **WHEN** a user opens an interactive climate page after the refresh
- **THEN** the primary map or visualization surface renders inside the designated stage area and remains usable for its current interactions

### Requirement: Responsive layout preserves the primary visualization
The system SHALL adapt the supporting UI for narrower viewports so the main climate visualization remains visible and usable instead of being crowded out by surrounding page chrome.

#### Scenario: responsive shell on a narrow viewport
- **WHEN** the page is viewed on a narrow mobile-sized or tablet-sized viewport
- **THEN** supporting panels, controls, or descriptive content stack, collapse, or reposition without obscuring the main visualization surface

### Requirement: Static deployment model remains supported
The refreshed browser experience MUST remain deployable as static pages that work with the existing generated assets and browser-side libraries, without introducing a mandatory heavyweight frontend runtime.

#### Scenario: static-host compatible refresh
- **WHEN** the refreshed pages are served from a static file host with the existing generated climate assets available
- **THEN** the shared shell loads and initializes successfully without requiring a server-rendered application or single-page app router

### Requirement: Existing external integrations are preserved
The refreshed browser pages MUST preserve the current analytics and map-library integrations required by the existing experience.

#### Scenario: analytics and map integrations remain available
- **WHEN** a refreshed GlobalWarming page loads in the browser
- **THEN** the required map-library assets and current analytics hooks are still present so the existing integrations continue to operate