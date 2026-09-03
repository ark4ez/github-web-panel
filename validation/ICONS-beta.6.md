# Icon validation — 0.2.0-beta.6

Date: 2026-09-03. Windows, local Rider 2026.2.1 / RD-262.9437.287.

- `check buildPlugin verifyPlugin`: BUILD SUCCESSFUL; 34 URL checks and 278 product regressions (312 total); Plugin Verifier 1.410: Compatible.
- The actual resource SVGs rendered successfully through the installed Rider's `SVGLoader`. Inspected the light/dark logo and the 16/20 px sidebar preview in [icon design](../docs/ICONS.md).
- Inspected the built JAR: both logo variants, all six sidebar SVGs and `GitHubPanelIconMappings.json` are packaged. `git diff --check` passed.
- ZIP SHA-256: `b7dbcc8d0e7af7fa73defbc024d4b5c0688652a651674ae01e501f39ec72bb1c`.

This validates rendering and packaging, not automatic theme/compact-mode selection or active-state recoloring in a running Rider window. The ordinary Rider installation remains beta.5. No remote CI, source publication or Marketplace submission was performed for beta.6 in this pass.
