$ErrorActionPreference = 'Stop'
Import-Module "$PSScriptRoot/../scripts/Marketplace.psm1" -Force
$digest = 'a' * 64
$fixture = @{ Tag = 'v0.2.0-beta.8'; Filename = 'github-web-panel-0.2.0-beta.8.zip'; Sha256 = $digest; ChecksumText = "$digest  github-web-panel-0.2.0-beta.8.zip`n" }
$plan = Get-MarketplacePlan @fixture
if ($plan.PluginId -ne 34049 -or $plan.Channel -cne 'beta') { throw 'Unexpected Marketplace destination.' }
$checks = 1
foreach ($change in @(
    @{ Tag = 'v0.2.0' },
    @{ Tag = 'v0.2.0-beta.08' },
    @{ Tag = 'v0.2.0-beta.8/other' },
    @{ Filename = '../github-web-panel-0.2.0-beta.8.zip' },
    @{ Filename = 'github-web-panel-0.2.0-beta.7.zip' },
    @{ Sha256 = 'b' * 64 },
    @{ ChecksumText = $fixture.ChecksumText + $fixture.ChecksumText }
)) {
    $bad = $fixture.Clone()
    foreach ($key in $change.Keys) { $bad[$key] = $change[$key] }
    $rejected = $false
    try { Get-MarketplacePlan @bad | Out-Null } catch { $rejected = $true }
    if (-not $rejected) { throw 'Accepted invalid Marketplace candidate.' }
    $checks++
}
$run = @{ path = '.github/workflows/release.yml'; head_repository = @{ full_name = 'ark4ez/github-web-panel' }; status = 'completed'; conclusion = 'success'; head_sha = 'a' * 40; event = 'push' }
Assert-ReleaseRun -Run $run -TagPush
$checks++
foreach ($change in @(
    @{ path = '.github/workflows/verify.yml' },
    @{ head_repository = @{ full_name = 'someone/fork' } },
    @{ conclusion = 'failure' },
    @{ status = 'in_progress' },
    @{ head_sha = '--unexpected-option' },
    @{ event = 'pull_request' },
    @{ event = 'workflow_dispatch' }
)) {
    $bad = $run.Clone()
    foreach ($key in $change.Keys) { $bad[$key] = $change[$key] }
    $rejected = $false
    try { Assert-ReleaseRun -Run $bad -TagPush } catch { $rejected = $true }
    if (-not $rejected) { throw 'Accepted an unverified release source.' }
    $checks++
}
$manual = $run.Clone(); $manual.event = 'workflow_dispatch'
Assert-ReleaseRun -Run $manual
$checks++
Write-Output "Marketplace candidate checks: $checks passed."
