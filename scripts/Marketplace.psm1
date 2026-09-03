Set-StrictMode -Version Latest

function Get-MarketplacePlan {
    param(
        [Parameter(Mandatory)][string]$Tag,
        [Parameter(Mandatory)][string]$Filename,
        [Parameter(Mandatory)][string]$Sha256,
        [Parameter(Mandatory)][string]$ChecksumText
    )
    if ($Tag -cnotmatch '^v(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)-beta\.(0|[1-9][0-9]*)$') { throw 'Only numbered beta tags can be uploaded to Marketplace beta.' }
    $version = $Tag.Substring(1)
    if ($Filename -cne "github-web-panel-$version.zip") { throw 'Candidate filename/version mismatch.' }
    if ($Sha256 -cnotmatch '^[0-9a-f]{64}$' -or $ChecksumText.Trim() -cne "$Sha256  $Filename") { throw 'Candidate checksum mismatch.' }
    [pscustomobject]@{ Tag = $Tag; Version = $version; Filename = $Filename; Sha256 = $Sha256; PluginId = 34049; Channel = 'beta' }
}

function Assert-ReleaseRun {
    param([Parameter(Mandatory)]$Run, [switch]$TagPush)
    if ($Run.path -cne '.github/workflows/release.yml' -or $Run.head_repository.full_name -cne 'ark4ez/github-web-panel' -or $Run.status -cne 'completed' -or $Run.conclusion -cne 'success') { throw 'A successful Prepare release run from this repository is required.' }
    if ($Run.head_sha -cnotmatch '^[0-9a-f]{40}$') { throw 'Invalid source commit.' }
    if ($Run.event -notin @('push', 'workflow_dispatch') -or ($TagPush -and $Run.event -cne 'push')) { throw 'Unexpected release run trigger.' }
}

Export-ModuleMember -Function Get-MarketplacePlan, Assert-ReleaseRun
