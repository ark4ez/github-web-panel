# Initial Marketplace beta submission

Date: 2026-09-03. Owner: ark4ez. Product: Repo Web Panel 0.2.0-beta.7.

The owner approved renaming the public product to Repo Web Panel and submitting the initial beta. The existing plugin ID `local.github.web.panel` and GitHub Web tool window are preserved. Only product metadata, listing/privacy text and documentation changed from beta.6; Java runtime source is unchanged.

Source commit: `a9ca105030b23f8817278248132b913b3f0b52b6`.

Local `check buildPlugin verifyPlugin` succeeded: 34 URL/navigation-boundary checks and 278 product regressions (312 total). Plugin Verifier 1.410 reported Compatible with local Windows Rider RD-262.9437.287. The JAR contains the intended name, version, unchanged plugin ID, MIT license, privacy notice and both logos, with no SmokeStartup or regression-suite fixture.

The exact local ZIP submitted to Marketplace has SHA-256:

`04a8f803a392c65ec797162bca5fe4766615a13f10eb2dedc7e8f181cad1a0bb  github-web-panel-0.2.0-beta.7.zip`

[GitHub CI run 33710273898](https://github.com/ark4ez/github-web-panel/actions/runs/33710273898) also completed successfully for the source commit above. The submitted ZIP is the local verified binary, not a claim of byte-identical CI output. Ordinary Rider remains on the live-tested beta.6 binary; beta.7 has not received independent exact-binary live UI acceptance.

Marketplace confirmed successful upload and assigned plugin ID **34049**. The page states that it was sent for moderation and is not public yet. The selected vendor is ark4ez, license MIT, category Issue Trackers and release channel beta. Existing vendor Non-trader status was retained; no vendor details, legal declarations or authentication settings were changed.

Listing: https://plugins.jetbrains.com/plugin/34049-repo-web-panel

The Versions page confirms update ID **1160406**, version **0.2.0-beta.7**, status **Under review**, compatibility range **262.9437.287 — 262.*** and supported product **Rider 2026.2.1**. Marketplace's own compatibility verification has no verdict yet; that pending result is separate from the successful local verifier and CI above.

Two genuine public, signed-out screenshots were uploaded and saved: `docs/images/issues-window.png` and `docs/images/copy-issue-url.png`. The page confirmed persistent screenshot IDs `df83bfdd-49e8-4427-90d0-359d980e4640` and `e713e1a6-b9f8-4f67-b92a-d807815ed3a5`. These beta.5 captures show navigation UI unchanged in beta.7; see [provenance](../docs/images/README.md). Their resolution is below Marketplace's recommended 1200 × 760, but upload and save succeeded.

The dedicated Getting Started, Privacy Policy and Contacts & Resources sections remain empty: their edit controls were disabled in the UI, including after returning to Overview. No disabled control was bypassed. The submitted description already contains privacy, source and support links, and the repository README provides setup instructions. These optional listing fields can be completed when editing is available.

After approval, the plugin-specific beta repository is `https://plugins.jetbrains.com/plugins/beta/34049`, following JetBrains' [custom-channel documentation](https://plugins.jetbrains.com/docs/marketplace/custom-release-channels.html). Installation from that repository has not yet been verified.

Approval is pending. Beta-only distribution does not imply default Stable-channel availability. No GitHub Release or stable Marketplace release was created.
