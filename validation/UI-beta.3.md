# Compact UI acceptance — 0.2.0-beta.3

Date: 2026-09-03. Windows; Rider 2026.2.1 / RD-262.9437.287.

The packaged beta replaces the wrapping grid with a single repository/section row. Back, Forward, Reload and More use Rider's native title actions. Search, browser, sign-in, zoom, rescan and help are in More. The first-use explanation is shorter and page search uses one row.

`check buildPlugin verifyPlugin` passed: 34 URL checks plus 272 existing product regressions; Plugin Verifier 1.410 reported Compatible. Those existing layout regressions cover the wrapping utility, not native title actions. Native UI evidence below is separate.

Installed the exact packaged JAR in the isolated Rider sandbox, with a public Git fixture. Confirmed the native title actions and More menu appear, Find highlights a matching phrase and Escape closes it, PR selection loads the public repository's pull requests, and the compact header fits at approximately 220 px and 640 px. GitHub's own page reflows after resizing; very narrow website layouts remain GitHub-controlled. No posts or authentication changes were made.

ZIP SHA-256: `0ebd2f20732f35171cbbf41d79034651a862f8ad505caf1eb1c6426397c92c50`.

Installed the same ZIP into the ordinary Rider through Install Plugin from Disk and restarted the IDE. Settings showed GitHub Web Panel 0.2.0-beta.3 enabled. The existing authenticated session survived, and the compact section selector opened both Issues and pull requests in an authorized private repository. The left dock position and approximately 615 px width were retained; the Project pane was hidden again to restore the GitHub pane's full height. No posts were made. The sandbox fixture remote was restored after testing.

The beta.2 validation record remains historical evidence for that build. Beta.3's fresh sign-in, uploads, download handoff and full lifecycle have not been revalidated. Remote CI has not run for these local changes.
