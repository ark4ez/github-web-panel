# Address bar acceptance — 0.2.0-beta.4

Date: 2026-09-03. Windows; Rider 2026.2.1 / RD-262.9437.287.

The address bar displays the main frame's current GitHub URL. Its read-only text can be selected and copied; Copy URL explicitly writes the full displayed URL to the IDE/system clipboard. The plugin does not persist full browsing URLs in its preferences. Query strings and fragments are retained by the implementation.

`check buildPlugin verifyPlugin` passed: 34 URL checks plus 272 existing product regressions; Plugin Verifier 1.410 reported Compatible. The existing automated checks do not exercise the new clipboard UI.

ZIP SHA-256: `b29a93496f1198599b6aaeb639599e5dbc7085748745f043297160351380374c`.

Installed the ZIP into ordinary Rider using Install Plugin from Disk and restarted. The existing authenticated session remained available. Confirmed the address bar followed navigation from the personal PR inbox to a private repository's PR list and then an individual PR. Clicking Copy URL showed feedback; pasting into the plugin's local Find field reproduced the full displayed PR URL. Closed Find after checking. No comments or other posts were submitted.

Query/fragment navigation, fresh sign-in, uploads and download handoff were not revalidated in this UI pass. Remote CI has not run for these local changes.
