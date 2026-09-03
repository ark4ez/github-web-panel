param(
    [string]$RiderHome = "$env:LOCALAPPDATA/Programs/Rider",
    [string]$OutputDirectory = $PSScriptRoot,
    [string]$BuildDirectory = (Join-Path ([IO.Path]::GetTempPath()) 'github-panel-build'),
    [switch]$Smoke
)
$ErrorActionPreference = 'Stop'
$riderRoot = (Resolve-Path -LiteralPath $RiderHome).Path
$metadata = Get-Content -Raw -LiteralPath (Join-Path $riderRoot 'product-info.json') | ConvertFrom-Json
if ($metadata.productCode -ne 'RD' -or $metadata.buildNumber -notlike '262.*') { throw 'This prototype targets Rider 2026.2 (262.*).' }
$javaExe = Join-Path $riderRoot 'jbr/bin/java.exe'
$javacExe = Join-Path $riderRoot 'jbr/bin/javac.exe'
$buildRoot = Join-Path $BuildDirectory ([Guid]::NewGuid().ToString('N'))
$classes = Join-Path $buildRoot 'classes'
$testClasses = Join-Path $buildRoot 'tests'
$packageRoot = Join-Path $buildRoot 'package'
$pluginLib = Join-Path $packageRoot 'github-web-panel/lib'
New-Item -ItemType Directory -Force -Path $classes,$testClasses,$pluginLib,$OutputDirectory | Out-Null
$libraryRoots = @('lib','plugins/jcef-plugin/lib','plugins/vcs-git/lib')
$jars = foreach ($relativeRoot in $libraryRoots) { Get-ChildItem -LiteralPath (Join-Path $riderRoot $relativeRoot) -Filter '*.jar' -Recurse }
$classPath = ($jars.FullName | Sort-Object -Unique) -join ';'
$sources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src/main/java') -Filter '*.java' -Recurse | Select-Object -ExpandProperty FullName)
$compileArgs = @('-encoding','UTF-8','-proc:none','--release','21','-classpath',$classPath,'-d',$classes) + $sources
# An argument file avoids Windows command-line size limits; quoted paths are javac syntax, not shell code.
$argsFile = Join-Path $buildRoot 'compile.args'
$compileArgs | ForEach-Object { '"' + $_.Replace('\','/').Replace('"','\"') + '"' } | Set-Content -Encoding utf8NoBOM -LiteralPath $argsFile
& $javacExe "@$argsFile"
if ($LASTEXITCODE -ne 0) { throw 'Plugin compilation failed.' }
Copy-Item -Path (Join-Path $PSScriptRoot 'src/main/resources/*') -Destination $classes -Recurse
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'LICENSE'),(Join-Path $PSScriptRoot 'PRIVACY.md') -Destination (Join-Path $classes 'META-INF')
$testSources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'tests/local/githubpanel') -Filter '*.java' | Where-Object Name -ne 'SmokeStartup.java' | Select-Object -ExpandProperty FullName)
& $javacExe -encoding UTF-8 --release 21 -cp $classes -d $testClasses @testSources
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
& $javaExe '-Djava.awt.headless=true' -cp "$classes;$testClasses" local.githubpanel.RegressionSuite
if ($LASTEXITCODE -ne 0) { throw 'URL tests failed.' }
if ($Smoke) {
    $smokeSource = Join-Path $PSScriptRoot 'tests/local/githubpanel/SmokeStartup.java'
    $smokeArgs = @('-encoding','UTF-8','-proc:none','--release','21','-cp',"$classes;$classPath",'-d',$classes,$smokeSource)
    $smokeArgs | ForEach-Object { '"' + $_.Replace('\','/').Replace('"','\"') + '"' } | Set-Content -Encoding utf8NoBOM -LiteralPath $argsFile
    & $javacExe "@$argsFile"
    if ($LASTEXITCODE -ne 0) { throw 'Smoke fixture compilation failed.' }
    $descriptor = Join-Path $classes 'META-INF/plugin.xml'
    (Get-Content -Raw $descriptor).Replace('</extensions>', '<postStartupActivity implementation="local.githubpanel.SmokeStartup" /></extensions>') | Set-Content -Encoding utf8NoBOM $descriptor
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$jarPath = Join-Path $pluginLib 'github-web-panel.jar'
[IO.Compression.ZipFile]::CreateFromDirectory($classes, $jarPath)
$suffix = if ($Smoke) { '-smoke' } else { '' }
$pluginVersion = ([xml](Get-Content -Raw (Join-Path $PSScriptRoot 'src/main/resources/META-INF/plugin.xml'))).'idea-plugin'.version
$archive = Join-Path $OutputDirectory "github-web-panel-$pluginVersion$suffix.zip"
if (Test-Path -LiteralPath $archive) { Remove-Item -LiteralPath $archive }
[IO.Compression.ZipFile]::CreateFromDirectory($packageRoot, $archive)
Get-FileHash -Algorithm SHA256 -LiteralPath $archive | Select-Object Path,Hash
Write-Output "Built against Rider $($metadata.version), build $($metadata.buildNumber)"
