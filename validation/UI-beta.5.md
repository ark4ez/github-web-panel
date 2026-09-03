# Repository startup acceptance — 0.2.0-beta.5

Date: 2026-09-03. Target: Windows, Rider 2026.2.1 / RD-262.9437.287.

Replaced the 800 ms startup fallback with `ProjectLevelVcsManager.runAfterInitialization`, returning to the UI thread before reading cached Git models and choosing the page. No network fetch or Git subprocess is added. The one-shot navigation gate is cancelled by explicit navigation or panel disposal. Later remote updates only refresh the selector.

API provenance: [JetBrains public VCS API](https://github.com/JetBrains/intellij-community/blob/master/platform/vcs-api/src/com/intellij/openapi/vcs/ProjectLevelVcsManager.kt) and [repository initialization](https://github.com/JetBrains/intellij-community/blob/master/platform/dvcs-impl/src/com/intellij/dvcs/repo/VcsRepositoryManager.kt). The public method was also inspected in the installed Rider SDK.

Six new account-free regressions exercise delayed repository discovery, no early navigation, one-shot completion, an already-completed/no-Git initialization, user navigation precedence and cancellation on disposal. These test the navigation gate; live Rider validation is separate.

`check buildPlugin verifyPlugin` passed: 34 URL checks + 278 product regressions = 312 checks. Plugin Verifier 1.410 reported Compatible with RD-262.9437.287.

Installed the exact packaged JAR in the stopped, isolated Rider sandbox and launched it with fresh JCEF storage. On startup, the repository selector and address bar pointed directly to `microsoft/vscode/issues`, with its public Issues rendered, without manually reselecting a repository. Confirmed individual-Issue URL tracking and Copy URL feedback. Public screenshots show the signed-out session in Rider's Window mode; no private repository data or account session is included.

ZIP SHA-256: `6ef57d36c45b92498e8b110bf69879b978f2ae4bfc9c3b5bfe09a57166e39221`.

Installed the same ZIP into ordinary Rider and restarted. The installed JAR descriptor reports 0.2.0-beta.5. Opening GitHub Web after reopening the project loaded the selected private repository's PR list directly, without reselecting the repository; the existing authenticated session was retained. The earlier sandbox pass exercised the tool window visible during startup. The public screenshot fixture's original remote and workspace file were restored after its Rider process exited.

The live pass does not independently simulate every initialization race; the account-free gate regressions cover delayed/cancelled callbacks. Fresh sign-in, uploads and download handoff were not revalidated for beta.5. Remote CI status is recorded separately after publication.
