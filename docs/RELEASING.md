# Release CI

`Prepare release` runs when an authorized maintainer pushes a `v*` tag. It reuses `Verify plugin` to build and verify the exact tagged source, then creates an **unpublished GitHub Release draft**. Tags themselves are public. This workflow does not create tags or upload to JetBrains Marketplace.

## Prepare a version

1. Use a branch and PR to update `pluginVersion` in `gradle.properties`, the version in `src/main/resources/META-INF/plugin.xml`, and the version's nonempty section in `CHANGELOG.md`.
2. Complete the applicable [release acceptance](../RELEASE-CHECKLIST.md), describing remaining beta limitations honestly. CI does not replace account-based or live Rider testing.
3. Merge the PR with `verify` passing. Do not put `[skip ci]` in PR commits.
4. After the owner has selected the release, tag that merged commit. Tag syntax is exactly `vMAJOR.MINOR.PATCH` or `vMAJOR.MINOR.PATCH-beta.NUMBER`; for example, `v0.2.0-beta.8`. Both source version declarations must match. Never move an existing release tag.

Example, only after version 0.2.0-beta.8 has been prepared, merged and selected for release:

```powershell
git switch main
git pull --ff-only
git tag -a v0.2.0-beta.8 -m 'Repo Web Panel 0.2.0-beta.8'
git push origin v0.2.0-beta.8
```

Only push the intended tag, not all local tags. A tag pushed with Actions' own `GITHUB_TOKEN` normally does not start another workflow; push the release tag as the authorized maintainer.

## Gates and output

- The tag is a supported version and agrees with Gradle, plugin.xml and the changelog.
- The tag points at the checked-out source, already reachable from `origin/main`.
- Release guard tests, plugin regressions, build and Rider Plugin Verifier succeed.
- The ZIP contains exactly the intended plugin JAR, expected identity/version, MIT license, privacy notice and icons, without test fixtures or IDE libraries.
- The publication job independently checks the downloaded ZIP's SHA-256. It executes no checked-out repository scripts and receives write permission only for creating the draft.

The draft attaches the installable plugin ZIP, `SHA256SUMS.txt`, and `plugin-verification.zip`. Notes are extracted from the matching changelog section. A `-beta.N` version is marked prerelease; stable versions remain drafts too. The final **Publish release** action is manual after acceptance review. Existing releases and assets are never overwritten by a rerun. If a failed attempt left a partial draft, inspect it before manually deleting that draft and rerunning; preserve the tag.

No extra secret is needed for GitHub draft creation: the job uses its scoped `GITHUB_TOKEN`. No Marketplace token is stored or consumed. Current initial Marketplace submission and review status are recorded [separately](../validation/MARKETPLACE-beta.7.md). A later Marketplace CI stage will need owner-configured credentials and an explicit beta/default channel decision.

## Validation-only run

Once this workflow exists on main, open **Actions → Prepare release → Run workflow** and enter the expected version tag. A manual run validates metadata, builds the ZIP and uploads artifacts, but never creates a tag or release, and does not assert tag existence or main ancestry. It can be used on a development branch to check a future release candidate. Those extra source guards run on real tag pushes.

CI artifacts expire after 14 days. Download important acceptance evidence before expiry. Artifacts attached to a published GitHub Release provide the intended longer-lived download path.

References: [GitHub release CLI](https://cli.github.com/manual/gh_release_create), [workflow permissions](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax#permissions), [triggering workflows](https://docs.github.com/en/actions/how-tos/writing-workflows/choosing-when-your-workflow-runs/triggering-a-workflow), [JetBrains publishing](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html).
