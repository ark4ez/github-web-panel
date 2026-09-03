# Changelog

## 0.2.0-beta.2

- Recognized attachment links offer an explicit external-browser action while preserving the current Issue page. In-panel downloads remain unsupported on the tested Rider runtime.
- Verified full-restart session persistence, private repository access, Issue creation, comment submission and a small text attachment upload in a dedicated private fixture.
- Sandbox launcher refuses to rebuild a running fixture and backs up the previous plugin directory to prevent duplicate JARs.
- Added attachment URL boundary regressions and updated the privacy notice and acceptance record.

## 0.2.0-beta.1

- First-use connection notice; JCEF is created only after choosing Open GitHub.
- Responsive toolbar with keyboard-focusable controls and page search.
- Remember the selected section and bounded zoom; never persist full browsing URLs.
- Explain 403/404 and network failures without assuming that the user is logged out.
- External HTTPS links require a separate explicit action. Automatic redirects do not open a browser.
- Refresh cached Git models only while visible and on repository events; preserve the page during remote changes.
- Pinned Gradle build, account-free regressions, Plugin Verifier configuration and CI.
- MIT license; author ark4ez.

This is a beta. Authenticated workflows and cross-platform compatibility are not release-certified.

## 0.1.0

- Local prototype: GitHub website, remote detection and basic navigation in a Rider tool window.
