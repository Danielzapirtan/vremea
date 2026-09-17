# Copilot instructions for this repository

## Project overview

This repository contains a small, single-page weather app built as a static HTML file (`index.html`). There is no framework, build system, bundler, or package manifest.

The app’s architecture is intentionally simple:

- `index.html` contains the entire UI, CSS, and JavaScript logic in one place.
- The interface is a weather dashboard with tabs for current conditions, hourly forecast, daily forecast, and location settings.
- It uses the browser’s `localStorage` to persist coordinates under keys like `wx_lat` and `wx_lon`.
- It fetches forecast data from the Open-Meteo API using the browser’s `fetch()` API.
- It relies on browser geolocation (`navigator.geolocation`) when the user chooses automatic detection.
- The app renders DOM content dynamically and updates the clock and forecast sections in place.

Keep changes compatible with this static, browser-only model. Do not introduce React/Vue, a build step, or a dependency system unless the repository explicitly adds one.

## Build, test, and lint commands

No build, test, or lint tooling is configured in this repository.

- No `package.json`, `Makefile`, or CI workflow was found.
- There is no automated test suite to run.
- For local preview, serve the site from the repo root with:

  ```bash
  python3 -m http.server 8000
  ```

  Then open `http://localhost:8000` in a browser.

If a future change adds project tooling, prefer the repository’s actual scripts and document them here rather than adding generic commands.

## Key conventions

- Keep the app as a static HTML/JS/CSS experience unless there is explicit repository support for a different stack.
- Preserve the existing single-file structure when making UI or logic changes; avoid scattering JavaScript into separate files unless the repo later adopts a real app structure.
- Match the current Romanian-language UI conventions already used in headings, labels, and user messages (for example “Locație”, “Acum”, “Ore”, “Zile”).
- Preserve the custom CSS variable palette and dark-theme design used for cards, tabs, accents, and status colors.
- Follow the current data model conventions in the JavaScript:
  - `WMO` maps weather codes to emoji/text descriptions.
  - `tempColor()` controls the visual temperature styling.
  - `renderCurrent()`, `renderHourly()`, and `renderDaily()` are the primary render functions.
  - `switchTab()` controls the tab and panel state.
- Respect the browser-only environment: use DOM APIs, `localStorage`, and `fetch()`; avoid server-side logic.
- Prefer minimal, direct DOM updates over large refactors. This codebase values a compact, hand-written implementation over framework abstractions.

## Working expectations

- When changing the weather logic, keep Open-Meteo requests and response handling aligned with the existing fetch URL parameters.
- When adjusting the location flow, preserve both manual coordinate input and geolocation detection behavior.
- When changing presentation details, keep the app mobile-friendly and centered within the existing narrow layout.
- Avoid adding dependencies unless the project later gains a package manager and workflow.

## Relevant repository files

- `index.html` — complete app implementation, including styles, UI, and behavior.

No additional assistant-specific instruction files such as `AGENTS.md`, `CLAUDE.md`, `.cursorrules`, or similar were present in this repository.
