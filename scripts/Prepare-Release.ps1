param(
    [Parameter(Mandatory)][string]$Tag,
    [Parameter(Mandatory)][string]$OutputDirectory,
    [switch]$RequireMain
)
$ErrorActionPreference = 'Stop'
Import-Module "$PSScriptRoot/Release.psm1" -Force
$root = Split-Path $PSScriptRoot -Parent
$metadata = Get-ReleaseMetadata -Tag $Tag -Properties (Get-Content "$root/gradle.properties" -Raw) -PluginXml (Get-Content "$root/src/main/resources/META-INF/plugin.xml" -Raw) -Changelog (Get-Content "$root/CHANGELOG.md" -Raw)
if ($RequireMain) {
    & git -C $root merge-base --is-ancestor HEAD refs/remotes/origin/main
    if ($LASTEXITCODE -ne 0) { throw 'Release commit must already be merged into origin/main.' }
    $tagCommit = & git -C $root rev-parse --verify "refs/tags/$Tag^{commit}"
    if ($LASTEXITCODE -ne 0) { throw 'Release tag does not exist locally.' }
    $headCommit = & git -C $root rev-parse HEAD
    if ($LASTEXITCODE -ne 0 -or $tagCommit -cne $headCommit) { throw 'Release tag does not point at the checked-out commit.' }
}
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$metadata | ConvertTo-Json | Set-Content "$OutputDirectory/release.json" -Encoding utf8NoBOM
$notes = @"
$($metadata.Notes)

### Install and verification

Install the plugin ZIP directly with Rider's **Settings → Plugins → Install Plugin from Disk**. Do not extract the plugin ZIP.

SHA256SUMS.txt identifies this run's package. plugin-verification.zip contains this run's automated verification reports. Automated checks do not certify interactive login, attachment or accessibility behavior.

Before publishing this draft, review [release acceptance](https://github.com/ark4ez/github-web-panel/blob/$Tag/RELEASE-CHECKLIST.md), the supported Rider/Windows scope and beta limitations in the [README](https://github.com/ark4ez/github-web-panel/blob/$Tag/README.md).

This GitHub Release does not upload to JetBrains Marketplace or change Marketplace review status.
"@
$notes | Set-Content "$OutputDirectory/RELEASE-NOTES.md" -Encoding utf8NoBOM
$metadata
