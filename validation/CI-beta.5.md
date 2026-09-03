# CI acceptance — 0.2.0-beta.5

Date: 2026-09-03. Source commit: `94285578a8903201b9fc7e6db1772e7a8461a9ac`.

[GitHub Actions run 33708024599](https://github.com/ark4ez/github-web-panel/actions/runs/33708024599) completed successfully on Windows. `check buildPlugin verifyPlugin` executed 34 URL/navigation-boundary checks and 278 product regressions (312 total). Plugin Verifier reported **Compatible** with Rider RD-262.9437.287.

Downloaded the `plugin-package` and `plugin-verification` artifacts. The downloaded package's SHA-256 matches its published `SHA256SUMS.txt`:

`8a94bfb9fad4d6fd195a71778ce823d7426ff34598073e07254e589803aed9ea  github-web-panel-0.2.0-beta.5.zip`

Inspected the packaged JAR: version 0.2.0-beta.5, vendor ark4ez, supported builds 262.9437.287 through 262.*, MIT license and privacy notice included, and no SmokeStartup fixture class or extension. The downloaded verifier verdict is `Compatible`.

The locally built ZIP installed and exercised in Rider has a different digest, recorded in [UI-beta.5.md](UI-beta.5.md). Both builds use the published source changes, but the CI package was not separately installed for live UI acceptance. CI compatibility checks do not establish fresh sign-in, attachment behavior or support for other Rider versions and operating systems.

This update publishes source and validation evidence; it does not create a Marketplace submission or a GitHub Release.
