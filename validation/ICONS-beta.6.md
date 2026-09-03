# Icon validation — 0.2.0-beta.6

Date: 2026-09-03. Windows, local Rider 2026.2.1 / RD-262.9437.287.

- `check buildPlugin verifyPlugin`: BUILD SUCCESSFUL; 34 URL checks and 278 product regressions (312 total); Plugin Verifier 1.410: Compatible.
- The actual resource SVGs rendered successfully through the installed Rider's `SVGLoader`. Inspected the light/dark logo and the 16/20 px sidebar preview in [icon design](../docs/ICONS.md).
- Inspected the built JAR: both logo variants, all six sidebar SVGs and `GitHubPanelIconMappings.json` are packaged. `git diff --check` passed.
- ZIP SHA-256: `b7dbcc8d0e7af7fa73defbc024d4b5c0688652a651674ae01e501f39ec72bb1c`.

Installed the same local ZIP through ordinary Rider's Plugins settings and restarted. The installed JAR descriptor reports 0.2.0-beta.6. In the user's dark theme, the plugin list displays the mint logo and the sidebar displays the new outlined icon. Focusing GitHub Web changes its icon to white on the blue active background. The selected private repository's PR list loaded automatically, with the existing GitHub session retained.

Light-theme and compact-mode variants were rendered directly but were not independently selected in a live Rider UI. Source was published as `29a52eaae212fa85f9aae5ab6ea00a9601160113`; remote CI is recorded separately. No Marketplace submission was performed.
