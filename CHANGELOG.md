# Changelog

## 0.2.0-beta.5

- Wait for Rider's VCS initialization before selecting the initial repository page, including projects whose Git models take longer than 800 ms to load.
- Preserve explicit navigation while startup is pending, and ignore delayed callbacks after disposal.
- Clarify that Copy URL writes the selected address to the IDE/system clipboard.

## 0.2.0-beta.4

- Show the current page URL in a selectable, read-only address bar.
- Add Copy URL for pasting exact links, including query strings and fragments, into other apps.
- Follow main-frame address changes without saving browsing URLs in plugin preferences.

## 0.2.0-beta.3

- Move Back, Forward and Reload into Rider's native tool-window title bar.
- Replace the wrapping grid of buttons with one repository/section row.
- Put page search, browser, sign-in, zoom, rescan and help in the More menu.
- Shorten the first-use screen and keep find-in-page controls on one line.

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
