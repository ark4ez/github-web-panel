param([Parameter(Mandatory)][string]$Archive, [string]$ExpectedVersion)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$root = Split-Path $PSScriptRoot -Parent
$version = $ExpectedVersion
if ([string]::IsNullOrWhiteSpace($version)) { $version = ((Get-Content "$root/gradle.properties" | Where-Object { $_ -cmatch '^pluginVersion=' }) -replace '^pluginVersion=', '').Trim() }
if ((Split-Path $Archive -Leaf) -cne "github-web-panel-$version.zip") { throw 'Unexpected package filename.' }
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $Archive).Path)
try {
    $files = @($zip.Entries | Where-Object { -not $_.FullName.EndsWith('/') })
    if ($files.Count -ne 1 -or $files[0].FullName -cne "github-web-panel/lib/github-web-panel-$version.jar") { throw 'Unexpected files in production ZIP.' }
    $stream = $files[0].Open()
    $jar = [IO.Compression.ZipArchive]::new($stream, [IO.Compression.ZipArchiveMode]::Read)
    try {
        foreach ($entry in $jar.Entries) {
            if ($entry.FullName -match '(SmokeStartup|RegressionSuite|Test\.class$|\.env|\.log$|\.jks$|\.p12$|^com/intellij/)') { throw 'Unexpected fixture, IDE library or local data in production JAR.' }
        }
        foreach ($required in @('META-INF/plugin.xml', 'META-INF/LICENSE', 'META-INF/PRIVACY.md', 'META-INF/pluginIcon.svg', 'META-INF/pluginIcon_dark.svg')) {
            if (-not $jar.GetEntry($required)) { throw "Missing required package entry: $required" }
        }
        $reader = [IO.StreamReader]::new($jar.GetEntry('META-INF/plugin.xml').Open())
        try { [xml]$descriptor = $reader.ReadToEnd() } finally { $reader.Dispose() }
        if ($descriptor.'idea-plugin'.id -cne 'local.github.web.panel' -or $descriptor.'idea-plugin'.version -cne $version) { throw 'Packaged plugin identity/version mismatch.' }
    } finally { $jar.Dispose(); $stream.Dispose() }
} finally { $zip.Dispose() }
