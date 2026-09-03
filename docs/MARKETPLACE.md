# Marketplace submission draft

Prepared 2026-09-03 for ark4ez. The owner approved the public name Repo Web Panel and initial beta submission. The upload form uses the existing ark4ez vendor, MIT, the public source URL and the beta channel, with Issue Trackers selected. Submission outcome is recorded below when available.

## Listing fields

- Name: Repo Web Panel.
- Version: 0.2.0-beta.7.
- Plugin ID: `local.github.web.panel` (preserve for existing installations).
- Vendor: ark4ez.
- Vendor website: https://github.com/ark4ez
- Vendor contact: reuse the existing owner-managed ark4ez vendor profile; do not substitute a GitHub noreply commit address. The existing vendor page shows Non-trader; this session did not change that declaration.
- Pricing: Free.
- License: MIT; full text in repository LICENSE and inside the package.
- Source and homepage: https://github.com/ark4ez/github-web-panel
- Support: https://github.com/ark4ez/github-web-panel/issues
- Private security reports: https://github.com/ark4ez/github-web-panel/security/advisories/new
- Privacy: https://github.com/ark4ez/github-web-panel/blob/main/PRIVACY.md
- Intended channel: `beta`; choose the relevant version-control/issue-tracking tags offered by the form.

The beta channel requires users to add its custom repository in Rider; it is not equivalent to default-channel discovery. After a numeric Marketplace plugin ID exists, prefer the plugin-specific beta repository URL documented by JetBrains.

## Short description

Keep GitHub Issues and pull requests beside your code, using GitHub's own website in a dedicated tool window.

## English description

Repo Web Panel opens GitHub's own website inside a dedicated Rider tool window. View Issues and pull requests for a repository detected from your project's Git remotes, with the familiar GitHub web interface.

Keep the panel docked beside your editor, move it to another side, or switch to a separate window using Rider's View Mode menu. The address bar follows the current page; select or copy its URL to share an exact link elsewhere.

Features:

- Repository selection from recognized github.com Git remotes.
- Shortcuts to Issues, pull requests and Projects pages.
- Back, Forward, Reload, find-in-page and zoom controls.
- A selectable address bar and Copy URL action.
- Light and dark icons, including compact sidebar variants.

Sign in directly on github.com for repositories you are authorized to access. No personal access token is requested. GitHub receives normal website requests, and Rider's embedded browser manages cookies and cache, which may be shared with other embedded Rider browsers. This plugin does not add analytics or a developer-hosted backend. See the linked privacy notice for the full data flow and session limits.

Beta scope: Windows and local Rider 2026.2.1 (build 262.9437.287) through 262.* with the bundled runtime and JCEF browser plugin. Binary compatibility and live UI checks have been performed on 2026.2.1. Other operating systems, remote development, GitHub Enterprise and external SSO are unsupported. Session expiry, account switching and specific passkey/2FA flows are not release-certified.

Downloads inside the panel are unsupported. Recognized file-attachment links offer an explicit action to open the original link in your system browser, which has a separate login session. A small text attachment upload was tested; other formats and complete Projects workflows still require further beta testing.

Free and open source under the MIT license. Maintained by ark4ez. Independent project; not affiliated with or endorsed by GitHub, JetBrains or Microsoft.

## Getting started

1. Install from the beta channel and restart Rider if requested.
2. Open View → Tool Windows → GitHub Web.
3. Read the first-use connection notice and choose Open GitHub.
4. Select the repository and Issues or PR. Use More → Sign in to GitHub if needed.
5. Copy the current URL from the address bar. Use the tool-window View Mode menu to dock, float or open it in a separate window.

## Initial release notes

First Marketplace beta. Includes repository-aware startup, GitHub Issues/PR/Projects page shortcuts, compact navigation, page search, zoom, Copy URL and dedicated light/dark icons. Windows Rider 2026.2.1 compatibility verified. Read the beta limitations before use.

## Package and evidence

Upload candidate: `github-web-panel-0.2.0-beta.7.zip`, rebuilt after the owner-approved rename and listing/privacy text updates. Its exact checksum and verification evidence are recorded in `validation/MARKETPLACE-beta.7.md`. Runtime Java code is unchanged from beta.6; live dark-theme installation, active sidebar icon, startup and existing authenticated browsing for that version are recorded in [icon acceptance](../validation/ICONS-beta.6.md). The beta.7 binary does not inherit an exact-binary live UI acceptance claim.

The previous runtime source revision `29a52eaae212fa85f9aae5ab6ea00a9601160113` passed [beta.6 CI](https://github.com/ark4ez/github-web-panel/actions/runs/33708914319); it is distinct from the renamed beta.7 artifact.

Use the two actual public, signed-out captures in `docs/images`: `issues-window.png` (caption: GitHub Issues in a separate Rider window) and `copy-issue-url.png` (caption: Copy an individual Issue URL). These were captured on beta.5; the navigation UI and GitHub Web tool-window label are unchanged in beta.7. See [provenance](images/README.md). The icon preview is artwork, not an installed-UI screenshot.

## Owner and submission requirements

- Sign in using the Marketplace account intended to publish as ark4ez; GitHub source ownership does not automatically establish Marketplace ownership.
- Confirm the public vendor contact email and truthful vendor/trader details. Free/MIT alone does not determine trader status.
- The owner must review and approve the Marketplace Developer Agreement as required by RELEASE-CHECKLIST.md; no acceptance is implied by this draft.
- The owner approved Repo Web Panel as the public display name; the plugin ID remains unchanged and the description explains GitHub compatibility.
- After upload, record the assigned plugin ID, beta channel and actual review status. An uploaded package is not an approved listing.

Official references checked 2026-09-03: [new plugin upload](https://plugins.jetbrains.com/docs/marketplace/uploading-a-new-plugin.html), [approval criteria](https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html), [custom channels](https://plugins.jetbrains.com/docs/marketplace/custom-release-channels.html), [GitHub brand guidance](https://brand.github.com/foundations/logo).
