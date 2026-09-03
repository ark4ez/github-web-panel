# Validation record

Date: 2026-09-03. Version: 0.2.0-beta.2. Owner: ark4ez. License: MIT.

## Automated evidence

Canonical command: `gradlew.bat -PlocalRider=<installed Rider> check buildPlugin verifyPlugin`.

- Windows, Rider 2026.2.1 / RD-262.9437.287, bundled Java 25 runtime; Java 21 target bytecode.
- Gradle 9.1.0, IntelliJ Platform Gradle Plugin 2.18.1, Plugin Verifier 1.410.
- Account-free regressions: 34 URL assertions plus 272 product assertions, 306 total. Covers URL boundaries, attachment classification, preferences and responsive nested toolbar layout.
- See `validation/verification-verdict.txt` and `SHA256SUMS.txt` for the final distribution's compatibility verdict and digest.
- Final Plugin Verifier verdict: **Compatible** for RD-262.9437.287. Build/check/verify completed successfully.
- The production ZIP contains one plugin JAR with LICENSE and PRIVACY.md; no smoke fixture, regression tests, IDE libraries, credentials or developer cache.
- The sandbox launcher's new running-process/backup handling was syntax-checked. Its complete updated launch path has not been exercised; the acceptance fixture was updated manually while stopped.

## Interactive evidence

The sandbox uses separate configuration/system/plugins/log directories. It contains an empty Git fixture pointing to an explicitly authorized private test repository owned by ark4ez. Tests use synthetic Issue text and a small synthetic text attachment.

Confirmed with the final 0.2.0-beta.1 distribution before the attachment fallback change:

- Closed the sandbox through Rider's Exit confirmation, verified the process ended, and relaunched with the same configuration and only the packaged plugin JAR. Opened the private repository without entering credentials again. Session persistence after a full Rider process restart is directly observed, beyond the owner's earlier provisional report.
- Created Issue #1 through the embedded GitHub UI as ark4ez.
- Entered a comment draft, clicked Rescan, and observed the draft remain intact.
- Opened GitHub's Add files picker, selected a small synthetic `.txt` file and observed its attachment link appear in the draft.
- Submitted the attachment-bearing comment through the embedded UI and observed its rendered text/link.
- Independently read GitHub's stored Issue/comment metadata through `gh` as ark4ez; confirmed the author and attachment presence. Restored the previously active CLI account afterward.

Earlier sandbox UI checks confirmed public Issues/PR navigation, Back/forward state, native page search and a wrapping toolbar at approximately 230 px width. The toolbar resize defect was fixed and covered by automated nested-layout regressions.

The ordinary Rider installation was upgraded from 0.1.0 using the final beta.2 ZIP through Install Plugin from Disk. Restarted Rider and confirmed Installed Plugins shows GitHub Web Panel 0.2.0-beta.2, author ark4ez, enabled. Opened the panel in the existing project, selected Open GitHub, and observed the private repository's Issues list without re-entering credentials. Resized the panel from approximately 250 px to 530 px and observed the website and toolbar reflow. This was read-only browsing; no Issue or comment was posted to the ordinary project.

## Download limitation

0.2.0-beta.1 blocked the attachment's redirect to GitHub's object host. Experiments with native JCEF download handling did not produce a successful Save dialog/download in this environment. Those experimental implementations are excluded from the final beta. No general permission to browse object hosts was added.

0.2.0-beta.2 offers a separate button for recognized `github.com/user-attachments/files/...` links. It preserves the Issue page and, after confirmation, passes the original GitHub URL to the system browser. That browser may require its own login. No signed CDN URL or IDE cookie is exported. In-panel downloads are unsupported; other attachment types and the system-browser download itself still require acceptance testing.

With the final beta.2 archive in the sandbox, clicked the test attachment and observed the download guidance and Open attachment in browser button. Opened its confirmation, verified the destination was github.com, then canceled; the Issue page remained intact and no external browser was launched by the canceled action.

## Remaining acceptance

- Session expiration, sign-out, account switching, specific 2FA/passkey/GitHub Mobile flows. External SSO remains unsupported.
- Other attachment formats, Projects interactions, broader draft/lifecycle preservation, keyboard-only use and assistive technology.
- Multiple projects, disable/re-enable/uninstall and browser/timer leak checks.
- Measured CPU/memory under sustained use. No performance claim is made.
- Remote CI execution; the workflow is prepared but has not run on GitHub.

The ordinary Rider installation now has 0.2.0-beta.2, with installation and private Issues browsing verified after restart. Only a private acceptance fixture repository was created. No public source repository, tag, release or Marketplace listing has been published. See RELEASE-CHECKLIST.md before advertising production readiness.
