# Release criteria

Owner: ark4ez. License: MIT. Current build: 0.2.0-beta.7 (Repo Web Panel). See [Marketplace submission](docs/MARKETPLACE.md), [previous runtime CI](validation/CI-beta.6.md) and [installed beta.6 UI acceptance](validation/ICONS-beta.6.md). Remaining interactive checks below are not implied to have passed.

## Automated gates

- Run `gradlew.bat check buildPlugin verifyPlugin` on the declared Windows/Rider target.
- Keep the Plugin Verifier report with the exact archive and SHA-256. Do not suppress unknown compatibility failures.
- Verify the production archive excludes SmokeStartup, tests, IDE libraries, credentials and developer cache.
- Preserve the plugin ID to allow upgrading existing installs. A future public rename must not silently break update continuity.

## Interactive acceptance (real GitHub account required)

- Fresh Rider profile: no GitHub request before Open GitHub, coherent toolbar at 220/320/640 px.
- Public and private repositories; password/2FA; sign out and sign in as another account; restart and confirm session behavior.
- Confirm stale or expired sessions lead to a usable Sign in path, not an infinite redirect or blank screen.
- Issues and PR search; Projects board navigation; draft comment survives hide/show and Git remote refresh.
- Submit a test comment only in a repository authorized for testing; exercise attachments and downloads with harmless files.
- Native passkeys, GitHub Mobile approval, OS file chooser, keyboard-only controls and screen reader names.
- Back/forward, reload, find next, zoom/reset/persistence, external link cancellation, offline/recovery, 403/404 guidance.
- Two Rider projects, close/reopen, disable/uninstall; inspect for disposed-browser exceptions and timer/browser leaks.
- Compare idle process CPU/memory before and after opening and hiding the panel. Record values, not guesses.

## Publication gate

- Establish the repository under ark4ez, a support URL and a private vulnerability-reporting channel.
- Confirm public name/brand usage, author contact and Marketplace developer profile.
- Finalize English listing, real screenshots and supported/unsupported authentication methods.
- Include MIT license, privacy notice and release notes; obtain the account owner's approval of Marketplace legal agreements.
- Publish only after the authenticated acceptance items needed for the advertised feature set pass. The current beta must not be described as production-certified.

Marketplace upload, public repository creation, tags and releases are separate actions. No workflow here publishes automatically.
