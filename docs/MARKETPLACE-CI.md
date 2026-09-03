# Marketplace beta CI

`Publish Marketplace beta` submits the exact verified GitHub Release ZIP to JetBrains Marketplace plugin **34049**, channel **beta**. It uses the official multipart upload API without rebuilding the package or passing a Marketplace credential to Gradle.

## Publication flow

1. Follow [Release CI](RELEASING.md) to prepare a numbered beta tag and unpublished GitHub Release draft.
2. Review live acceptance and limitations, the changelog and attached artifacts.
3. Publish that GitHub **prerelease** as the authorized maintainer. This triggers the Marketplace workflow. The version must match `vMAJOR.MINOR.PATCH-beta.NUMBER`; stable releases are ignored.
4. The workflow verifies main ancestry, exact tag/version/changelog, a successful tag-triggered `Prepare release` run, the plugin package contents, and agreement between the published GitHub Release assets and the original CI ZIP/checksum.
5. A separate job submits the inspected package to plugin 34049 / beta using `MARKETPLACE_TOKEN`. That job executes no checked-out repository scripts, allows no HTTP redirects and makes no automatic upload retries.
6. Inspect the Marketplace update page for the actual review status. HTTP upload success is not approval. Beta updates become available through the beta repository after approval; this workflow does not target Stable.

Publish within the original CI artifacts' 14-day retention period. If those artifacts have expired, preparation fails rather than silently rebuilding a replacement ZIP. Prepare a new candidate/version when necessary. Do not replace an existing release asset or move its tag.

The initial 0.2.0-beta.7 was uploaded manually and is already under review. Do not resubmit it merely to test this workflow. Use a future selected beta for the first real CI upload.

## One-time credential setup

- Environment: `marketplace-beta` in [repository environment settings](https://github.com/ark4ez/github-web-panel/settings/environments).
- Deployment policy: only tag refs matching `v*-beta.*`. No branch deployments. The workflow also applies the stricter numbered-beta version check.
- Secret: `MARKETPLACE_TOKEN`, stored **inside that environment**, not as a repository-wide secret, plaintext file, Gradle property or chat message.
- Create a dedicated token under the publishing account in [Marketplace My Tokens](https://plugins.jetbrains.com/author/me/tokens). Name it for this repository and use the narrowest available publishing scope. Copy it directly into the environment secret. Tokens are shown only when created; rotate/revoke through the Marketplace account if needed.
- GitHub Release publication is the owner's explicit release action. This flow does not add another required reviewer to the environment, so solo maintenance remains possible.

The workflow can be installed and tested before token setup. A real publication without the secret fails with a setup message. Do not place a fake token in the environment to make the check green.

## Safe trial run

Open **Actions → Publish Marketplace beta → Run workflow** on main and enter a successful `Prepare release` run ID. Manual runs inspect its source, package, checksum and release metadata, and save a `marketplace-candidate` artifact with the exact ZIP and a public upload plan. They never read the Marketplace secret or upload anything. A candidate from an unmerged branch, failed run or different repository is rejected.

No tag or GitHub Release is required for this trial. It validates preparation, not authenticated HTTP upload, Marketplace approval or installation from the beta channel.

## Failure and retry

- Missing token: finish environment setup, then rerun the failed upload job on the original `release` event.
- HTTP 4xx: inspect token authorization, version conflicts and Marketplace status before retrying.
- Timeout or connection failure: the server may already have received the package. Check the existing updates first; do not blindly rerun or increment versions to hide an uncertain outcome.
- Changed/expired artifacts, source or checksum mismatch: do not bypass the gate. Investigate the release and prepare a new reviewed candidate when necessary.

Official references: [upload API](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html), [approval process](https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html), [GitHub environment protection](https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments).
