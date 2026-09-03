# CI acceptance — 0.2.0-beta.6

Date: 2026-09-03. Source commit: `29a52eaae212fa85f9aae5ab6ea00a9601160113`.

[GitHub Actions run 33708914319](https://github.com/ark4ez/github-web-panel/actions/runs/33708914319) completed successfully on Windows. All 34 URL/navigation-boundary checks and 278 product regressions passed (312 total). The build, binary compatibility verification, checksum recording and artifact uploads all succeeded. The downloaded Plugin Verifier verdict reports **Compatible** with Rider RD-262.9437.287.

Downloaded the `plugin-package` and `plugin-verification` artifacts. The package's SHA-256 matches the uploaded `SHA256SUMS.txt`:

`0b0681b8b9911a84ec7ed9f83afe8f0fa47ec67b5c87dde62a8748b55ca338bf  github-web-panel-0.2.0-beta.6.zip`

The local ZIP installed into ordinary Rider has a different digest and separate [live icon acceptance](ICONS-beta.6.md). The CI package was not separately installed. No GitHub Release or Marketplace submission was created.
