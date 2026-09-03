param([Parameter(Mandatory)][string]$OutputDirectory)
$ErrorActionPreference = 'Stop'
Import-Module "$PSScriptRoot/Marketplace.psm1" -Force
$root = Split-Path $PSScriptRoot -Parent
if ($env:GH_REPO -cne 'ark4ez/github-web-panel') { throw 'Unexpected repository.' }
function Get-GhJson {
    $result = & gh @args
    if ($LASTEXITCODE -ne 0) { throw 'GitHub metadata request failed.' }
    $result | ConvertFrom-Json
}
function Invoke-GhDownload {
    & gh @args
    if ($LASTEXITCODE -ne 0) { throw 'Asset download failed. Required CI artifacts may have expired; do not rebuild and silently substitute another ZIP.' }
}
$event = Get-Content -LiteralPath $env:GITHUB_EVENT_PATH -Raw | ConvertFrom-Json
$releaseEvent = $env:GITHUB_EVENT_NAME -ceq 'release'
if ($releaseEvent) {
    $releaseId = [string]$event.release.id
    if ($releaseId -notmatch '^[0-9]+$') { throw 'Invalid release ID.' }
    $release = Get-GhJson api "repos/$env:GH_REPO/releases/$releaseId"
    if ($release.draft -or -not $release.prerelease -or -not $release.published_at) { throw 'A published GitHub prerelease is required.' }
    $tag = [string]$release.tag_name
    # Validate the exact source tag, versions, changelog and main ancestry before downloading.
    $metadata = & "$PSScriptRoot/Prepare-Release.ps1" -Tag $tag -OutputDirectory "$env:RUNNER_TEMP/marketplace-source" -RequireMain
    if (-not $metadata.Prerelease) { throw 'Stable Marketplace publishing is not configured.' }
    $head = & git -C $root rev-parse HEAD
    if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve source commit.' }
    $runs = Get-GhJson api "repos/$env:GH_REPO/actions/workflows/release.yml/runs?head_sha=$head&event=push&status=success&per_page=100"
    $matching = @($runs.workflow_runs | Where-Object { $_.head_branch -ceq $tag })
    if ($matching.Count -eq 0) { throw 'No successful tag-triggered Prepare release run found.' }
    $run = $matching[0]
    Assert-ReleaseRun -Run $run -TagPush
    if ($run.head_sha -cne $head) { throw 'Release source and verified source differ.' }
} elseif ($env:GITHUB_EVENT_NAME -ceq 'workflow_dispatch') {
    if ($env:RELEASE_RUN_ID -notmatch '^[0-9]+$') { throw 'A numeric Prepare release run ID is required.' }
    $run = Get-GhJson api "repos/$env:GH_REPO/actions/runs/$env:RELEASE_RUN_ID"
    Assert-ReleaseRun -Run $run
    & git -C $root merge-base --is-ancestor $run.head_sha refs/remotes/origin/main
    if ($LASTEXITCODE -ne 0) { throw 'Candidate source is not on main.' }
} else { throw 'Unsupported Marketplace workflow trigger.' }

$scratch = Join-Path $env:RUNNER_TEMP ('marketplace-inspection-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $scratch -Force | Out-Null
Invoke-GhDownload run download ([string]$run.id) --repo $env:GH_REPO --name plugin-package --dir "$scratch/verified"
Invoke-GhDownload run download ([string]$run.id) --repo $env:GH_REPO --name release-metadata --dir "$scratch/metadata"
$metadata = Get-Content "$scratch/metadata/release.json" -Raw | ConvertFrom-Json
if ($releaseEvent -and $metadata.Tag -cne $tag) { throw 'Release tag and CI metadata differ.' }
$tag = [string]$metadata.Tag
$packages = @(Get-ChildItem -LiteralPath "$scratch/verified" -Filter '*.zip')
if ($packages.Count -ne 1) { throw 'Expected exactly one verified plugin ZIP.' }
$package = $packages[0]
$hash = (Get-FileHash -LiteralPath $package.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
$checksum = Get-Content "$scratch/verified/SHA256SUMS.txt" -Raw
$plan = Get-MarketplacePlan -Tag $tag -Filename $package.Name -Sha256 $hash -ChecksumText $checksum
& "$PSScriptRoot/Test-PluginPackage.ps1" -Archive $package.FullName -ExpectedVersion $plan.Version

if ($releaseEvent) {
    Invoke-GhDownload release download $tag --repo $env:GH_REPO --pattern $package.Name --pattern SHA256SUMS.txt --dir "$scratch/published"
    $publishedHash = (Get-FileHash "$scratch/published/$($package.Name)" -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($publishedHash -cne $hash -or (Get-Content "$scratch/published/SHA256SUMS.txt" -Raw).Trim() -cne $checksum.Trim()) { throw 'Published release assets differ from the verified CI package.' }
}

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
Copy-Item -LiteralPath $package.FullName -Destination $OutputDirectory
Copy-Item -LiteralPath "$scratch/verified/SHA256SUMS.txt" -Destination $OutputDirectory
$plan | Add-Member -NotePropertyName SourceRunId -NotePropertyValue $run.id
$plan | Add-Member -NotePropertyName SourceCommit -NotePropertyValue $run.head_sha
$plan | ConvertTo-Json | Set-Content "$OutputDirectory/marketplace.json" -Encoding utf8NoBOM
"Inspected $($plan.Filename) for Marketplace plugin 34049, beta. Source run: $($run.id). SHA-256: $hash. Manual runs never upload." >> $env:GITHUB_STEP_SUMMARY
