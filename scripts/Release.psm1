Set-StrictMode -Version Latest

function Get-ReleaseMetadata {
    param(
        [Parameter(Mandatory)][string]$Tag,
        [Parameter(Mandatory)][string]$Properties,
        [Parameter(Mandatory)][string]$PluginXml,
        [Parameter(Mandatory)][string]$Changelog
    )
    # The release contract deliberately supports stable and numbered beta tags only.
    if ($Tag -cnotmatch '^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-beta\.(0|[1-9][0-9]*))?$') {
        throw 'Expected vMAJOR.MINOR.PATCH or vMAJOR.MINOR.PATCH-beta.NUMBER.'
    }
    $version = $Tag.Substring(1)
    $versions = [regex]::Matches($Properties, '(?m)^pluginVersion=([^\r\n]+)\r?$')
    if ($versions.Count -ne 1 -or $versions[0].Groups[1].Value -cne $version) {
        throw 'Tag and gradle.properties pluginVersion must match exactly.'
    }
    [xml]$descriptor = $PluginXml
    if ($descriptor.'idea-plugin'.id -cne 'local.github.web.panel') { throw 'Unexpected plugin ID.' }
    if ($descriptor.'idea-plugin'.version -cne $version) { throw 'Tag and plugin.xml version must match exactly.' }
    $heading = [regex]::Escape($version)
    $sections = [regex]::Matches($Changelog, "(?ms)^## $heading\r?`n(.*?)(?=^## |\z)")
    if ($sections.Count -ne 1 -or [string]::IsNullOrWhiteSpace($sections[0].Groups[1].Value)) {
        throw 'Exactly one nonempty changelog section is required for the release version.'
    }
    [pscustomobject]@{
        Tag = $Tag
        Version = $version
        Prerelease = $version.Contains('-beta.')
        Notes = $sections[0].Groups[1].Value.Trim()
    }
}

Export-ModuleMember -Function Get-ReleaseMetadata
