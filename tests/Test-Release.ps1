$ErrorActionPreference = 'Stop'
Import-Module "$PSScriptRoot/../scripts/Release.psm1" -Force
$fixture = @{
    Tag = 'v0.2.0-beta.8'
    Properties = "pluginVersion=0.2.0-beta.8`n"
    PluginXml = '<idea-plugin><id>local.github.web.panel</id><version>0.2.0-beta.8</version></idea-plugin>'
    Changelog = "# Changelog`n`n## 0.2.0-beta.8`n`n- Current notes.`n`n## 0.2.0-beta.7`n`n- Older notes.`n"
}
$checks = 0
$beta = Get-ReleaseMetadata @fixture
if (-not $beta.Prerelease -or $beta.Notes -cne '- Current notes.') { throw 'Beta notes/classification failed.' }
$checks++
$stable = $fixture.Clone()
$stable.Tag = 'v1.0.0'
$stable.Properties = 'pluginVersion=1.0.0'
$stable.PluginXml = $stable.PluginXml.Replace('0.2.0-beta.8', '1.0.0')
$stable.Changelog = "## 1.0.0`r`n`r`n- Stable notes.`r`n"
if ((Get-ReleaseMetadata @stable).Prerelease) { throw 'Stable version was marked prerelease.' }
$checks++
foreach ($badTag in @('0.2.0-beta.8', 'v0.2.0-beta.08', 'v00.2.0', 'v0.2.0-rc.1', 'v0.2.0;echo bad', 'v0.2.0-beta.9')) {
    $bad = $fixture.Clone(); $bad.Tag = $badTag
    $rejected = $false
    try { Get-ReleaseMetadata @bad | Out-Null } catch { $rejected = $true }
    if (-not $rejected) { throw "Accepted invalid/mismatched tag: $badTag" }
    $checks++
}
foreach ($change in @(
    @{ Properties = "pluginVersion=0.2.0-beta.8`npluginVersion=0.2.0-beta.8`n" },
    @{ PluginXml = $fixture.PluginXml.Replace('0.2.0-beta.8', '0.2.0-beta.7') },
    @{ PluginXml = $fixture.PluginXml.Replace('local.github.web.panel', 'other.plugin') },
    @{ Changelog = "## 0.2.0-beta.8`n`n## 0.2.0-beta.7`n- Wrong notes.`n" },
    @{ Changelog = "## 0.2.0-beta.7`n- Wrong notes.`n" },
    @{ Changelog = $fixture.Changelog + "`n## 0.2.0-beta.8`n- Duplicate.`n" }
)) {
    $bad = $fixture.Clone()
    foreach ($key in $change.Keys) { $bad[$key] = $change[$key] }
    $rejected = $false
    try { Get-ReleaseMetadata @bad | Out-Null } catch { $rejected = $true }
    if (-not $rejected) { throw 'Accepted mismatched/ambiguous release metadata.' }
    $checks++
}
Write-Output "Release metadata checks: $checks passed."

# Exercise the real tag/ancestry guard in a disposable repository, with no remotes.
$tempBase = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$fixtureRoot = Join-Path $tempBase ('repo-web-panel-release-' + [guid]::NewGuid().ToString('N'))
function Invoke-FixtureGit {
    & git -C $fixtureRoot @args | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Fixture git command failed.' }
}
try {
    New-Item -ItemType Directory -Path "$fixtureRoot/scripts", "$fixtureRoot/src/main/resources/META-INF" -Force | Out-Null
    Copy-Item "$PSScriptRoot/../scripts/Prepare-Release.ps1", "$PSScriptRoot/../scripts/Release.psm1" "$fixtureRoot/scripts/"
    $fixture.Properties | Set-Content "$fixtureRoot/gradle.properties"
    $fixture.PluginXml | Set-Content "$fixtureRoot/src/main/resources/META-INF/plugin.xml"
    $fixture.Changelog | Set-Content "$fixtureRoot/CHANGELOG.md"
    Invoke-FixtureGit init --initial-branch main
    Invoke-FixtureGit config user.name 'Release Test'
    Invoke-FixtureGit config user.email 'release-test@example.invalid'
    Invoke-FixtureGit config commit.gpgsign false
    Invoke-FixtureGit config core.hooksPath "$fixtureRoot/no-hooks"
    Invoke-FixtureGit add .
    Invoke-FixtureGit commit -m 'Fixture main'
    Invoke-FixtureGit update-ref refs/remotes/origin/main HEAD
    Invoke-FixtureGit tag $fixture.Tag
    & "$fixtureRoot/scripts/Prepare-Release.ps1" -Tag $fixture.Tag -OutputDirectory "$fixtureRoot/result" -RequireMain | Out-Null

    # The source guard must refuse a release commit outside main.
    Invoke-FixtureGit -c advice.detachedHead=false checkout --detach
    'unmerged' | Set-Content "$fixtureRoot/unmerged.txt"
    Invoke-FixtureGit add unmerged.txt
    Invoke-FixtureGit commit -m 'Unmerged fixture'
    $rejected = $false
    try { & "$fixtureRoot/scripts/Prepare-Release.ps1" -Tag $fixture.Tag -OutputDirectory "$fixtureRoot/result" -RequireMain | Out-Null } catch { $rejected = $true }
    if (-not $rejected) { throw 'Accepted release source outside main.' }

    # Even a main commit must match the requested tag's exact target.
    Invoke-FixtureGit update-ref refs/remotes/origin/main HEAD
    $rejected = $false
    try { & "$fixtureRoot/scripts/Prepare-Release.ps1" -Tag $fixture.Tag -OutputDirectory "$fixtureRoot/result" -RequireMain | Out-Null } catch { $rejected = $true }
    if (-not $rejected) { throw 'Accepted release source that differs from tag.' }
    Write-Output 'Release source checks: 3 passed.'
} finally {
    $resolved = [IO.Path]::GetFullPath($fixtureRoot)
    if (-not $resolved.StartsWith($tempBase, [StringComparison]::OrdinalIgnoreCase) -or (Split-Path $resolved -Leaf) -notmatch '^repo-web-panel-release-[0-9a-f]{32}$') {
        throw 'Refusing to remove unexpected test fixture path.'
    }
    if (Test-Path -LiteralPath $resolved) { Remove-Item -LiteralPath $resolved -Recurse -Force }
}
