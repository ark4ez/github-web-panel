param([string]$RiderHome = "$env:LOCALAPPDATA/Programs/Rider", [string]$SandboxDirectory, [switch]$Smoke)
$ErrorActionPreference = 'Stop'
if (!$SandboxDirectory) { $SandboxDirectory = Join-Path $PSScriptRoot 'sandbox' }
$sandboxRoot = [IO.Path]::GetFullPath($SandboxDirectory)
$projectDirectory = Join-Path $sandboxRoot 'github-panel-smoke-project'
if (Get-CimInstance Win32_Process -Filter "Name='rider64.exe'" | Where-Object {
    $_.CommandLine -and $_.CommandLine.IndexOf($projectDirectory, [StringComparison]::OrdinalIgnoreCase) -ge 0
}) { throw 'Close the Rider sandbox before rebuilding its plugin.' }
New-Item -ItemType Directory -Force -Path $sandboxRoot | Out-Null
& (Join-Path $PSScriptRoot 'build.ps1') -RiderHome $RiderHome -OutputDirectory $sandboxRoot -BuildDirectory (Join-Path $sandboxRoot 'build') -Smoke:$Smoke
$pluginDirectory = Join-Path $sandboxRoot 'plugins'
New-Item -ItemType Directory -Force -Path $pluginDirectory | Out-Null
$suffix = if ($Smoke) { '-smoke' } else { '' }
$pluginVersion = ([xml](Get-Content -Raw (Join-Path $PSScriptRoot 'src/main/resources/META-INF/plugin.xml'))).'idea-plugin'.version
# Keep old versioned/unversioned JARs out of the plugin classpath. Preserve the previous installation outside plugins.
$previousPlugin = Join-Path $pluginDirectory 'github-web-panel'
if (Test-Path -LiteralPath $previousPlugin) {
    $resolvedPlugin = (Resolve-Path -LiteralPath $previousPlugin).Path
    $expectedPlugin = [IO.Path]::GetFullPath((Join-Path $sandboxRoot 'plugins/github-web-panel'))
    if ($resolvedPlugin -ne $expectedPlugin -or ((Get-Item -LiteralPath $previousPlugin).Attributes -band [IO.FileAttributes]::ReparsePoint)) {
        throw 'Unexpected sandbox plugin path; refusing to move it.'
    }
    $backupDirectory = Join-Path $sandboxRoot 'plugin-backups'
    New-Item -ItemType Directory -Force -Path $backupDirectory | Out-Null
    $backupPath = [IO.Path]::GetFullPath((Join-Path $backupDirectory ([Guid]::NewGuid().ToString())))
    if (!$backupPath.StartsWith($sandboxRoot.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Backup path escapes the sandbox.'
    }
    Move-Item -LiteralPath $resolvedPlugin -Destination $backupPath
}
Expand-Archive -LiteralPath (Join-Path $sandboxRoot "github-web-panel-$pluginVersion$suffix.zip") -DestinationPath $pluginDirectory -Force
$projectDirectory = Join-Path $sandboxRoot 'github-panel-smoke-project'
New-Item -ItemType Directory -Force -Path $projectDirectory | Out-Null
if (!(Test-Path -LiteralPath (Join-Path $projectDirectory '.git'))) {
    & git init --quiet $projectDirectory
    if ($LASTEXITCODE -ne 0) { throw 'Could not initialize sandbox project.' }
    & git -C $projectDirectory remote add origin https://github.com/JetBrains/intellij-community.git
    if ($LASTEXITCODE -ne 0) { throw 'Could not set sandbox remote.' }
}
$ideaDirectory = Join-Path $projectDirectory '.idea'
New-Item -ItemType Directory -Force -Path $ideaDirectory | Out-Null
'<project version="4"><component name="VcsDirectoryMappings"><mapping directory="$PROJECT_DIR$" vcs="Git" /></component></project>' | Set-Content -LiteralPath (Join-Path $ideaDirectory 'vcs.xml') -Encoding utf8NoBOM
'# GitHub Web Panel sandbox — only a Git remote; no source checkout or credentials.' | Set-Content -LiteralPath (Join-Path $projectDirectory 'README.md') -Encoding utf8NoBOM
$properties = @(
    ('idea.config.path=' + (Join-Path $sandboxRoot 'config').Replace('\','/'))
    ('idea.system.path=' + (Join-Path $sandboxRoot 'system').Replace('\','/'))
    ('idea.plugins.path=' + $pluginDirectory.Replace('\','/'))
    ('idea.log.path=' + (Join-Path $sandboxRoot 'logs').Replace('\','/'))
    'idea.initially.ask.config=false',
    'idea.trust.all.projects=true'
)
if ($Smoke) { $properties += 'github.panel.smoke.directory=' + (Join-Path $sandboxRoot 'evidence').Replace('\','/') }
$propertiesPath = Join-Path $sandboxRoot 'sandbox.properties'
$properties | Set-Content -LiteralPath $propertiesPath -Encoding utf8NoBOM
# Only the new process inherits this settings path; the running Rider is unaffected.
$previousProperties = $env:RIDER_PROPERTIES
try {
    $env:RIDER_PROPERTIES = $propertiesPath
    # A visible window is intentional: this is the interactive prototype the user asked to try.
    $sandboxProcess = Start-Process -FilePath (Join-Path $RiderHome 'bin/rider64.exe') -ArgumentList ('"' + $projectDirectory + '"') -PassThru
    Write-Output "Sandbox Rider process: $($sandboxProcess.Id)"
    Write-Output "Sandbox path: $sandboxRoot"
} finally {
    if ($null -eq $previousProperties) { Remove-Item Env:RIDER_PROPERTIES -ErrorAction SilentlyContinue }
    else { $env:RIDER_PROPERTIES = $previousProperties }
}
