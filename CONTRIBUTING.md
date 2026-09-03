# Contributing

Use Java 21-compatible source. The build tool and IDE target are pinned in Gradle. Run:

```powershell
./gradlew.bat check buildPlugin verifyPlugin
```

Keep changes small and include a regression for navigation policy, lifecycle or layout bugs. UI changes should also be checked in the isolated Rider sandbox at both narrow and wide widths. The account-free suite cannot prove login, passkey, file-transfer or accessibility behavior.

Do not add telemetry, a developer backend, PAT storage, cookie export, DOM scraping, script injection, broad host wildcards or certificate bypasses. Browser content is untrusted. Only explicit user actions may open external links. If proposing external SSO or a dedicated browser profile, first document the credential boundaries and test the actual supported runtime APIs.

Do not commit IDE caches, sandbox state, login URLs, cookies or private screenshots. The smoke fixture may inspect only the known public test page and must never enter the production archive.

Changes to main go through a pull request with the required `verify` check passing against the latest main and all review threads resolved. Another person's approval is optional for solo maintenance. Do not use `[skip ci]` in PR commits: the required check must run.

Each release needs the exact ZIP, SHA-256, Plugin Verifier report and a completed acceptance record in RELEASE-CHECKLIST.md. [Release CI](docs/RELEASING.md) prepares an unpublished GitHub Release draft from an approved version tag. The final publication decision and Marketplace uploads are manual. License contributions under the repository's MIT license.
