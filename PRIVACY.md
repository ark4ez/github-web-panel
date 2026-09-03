# Privacy and session behavior

GitHub Web Panel is maintained by ark4ez. It embeds the actual GitHub website.

## Data paths

- The plugin reads cached Git remote URLs in the open project. It accepts only recognized github.com repository URLs and does not invoke git or fetch remotes.
- After you choose Open GitHub on first use, the selected GitHub URL is loaded directly by Rider's embedded Chromium browser. GitHub receives ordinary web requests, including IP address, browser metadata and session cookies; page resources can come from other domains. See [GitHub's privacy statement](https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement).
- The plugin has no analytics endpoint, developer backend, REST API client, DOM extraction, JavaScript injection or native JavaScript bridge.
- It does not ask for a PAT, copy Chrome/Edge cookies, export cookies or serialize credentials. Browsing URLs are kept in memory only, since they may include login redirects and tokens.
- Local preferences store only repository owner/name, section, zoom, and whether the first-use notice was accepted. The first-use choice is shared across projects in the same Rider profile.
- GitHub cookies/cache/history-related state remain under Rider/JCEF management, possibly shared with other embedded Rider browsers. The plugin does not promise a dedicated profile, encryption at rest, or persistence across restarts.
- Find-in-page text is sent to the embedded browser's native search API and is not stored by the plugin.
- Normal diagnostic logs produced by Rider/JCEF and GitHub's own behavior are outside the plugin's custom logging; do not share IDE logs without checking for sensitive URLs.

## Controls

Use GitHub's own Sign out action to end the session. The Browser action opens the current GitHub URL in the system browser, with a separate login session. Uninstalling the plugin does not necessarily delete JCEF cookies. Do not delete shared Rider browser storage as a plugin-specific logout mechanism.

Main-frame navigation is restricted to HTTPS github.com. This is not a network sandbox or a guarantee that all subresources come from GitHub. External HTTPS links can be opened after a deliberate action and confirmation; redirects and popups do not automatically launch external applications. External SSO and GitHub Enterprise are unsupported.

Attachment uploads use GitHub's own file picker and web requests. Downloads are not supported inside this beta: recognized attachment links offer a separate action to open the original github.com attachment URL in the system browser, which may require its own login. Signed CDN redirects and IDE cookies are not exported. The original attachment URL is held only in memory until the notice is dismissed or replaced.

The plugin runs with IDE process permissions, like other JetBrains plugins. Open source and absence of PAT storage do not imply a security certification.

## Reports

Do not include passwords, PATs, OTPs, cookies, private issue text, or raw auth URLs in public reports. A repository-specific private reporting channel must be established before Marketplace publication.
