#Requires -Version 7.0
<#
.SYNOPSIS
    Writes the git-ignored .env file that build.ps1 reads for the toolchain of this project.

.DESCRIPTION
    This script detects a JDK 21 and an Apache Maven installation. It also accepts an
    unpacked BIRT runtime. It validates each path. It then writes JAVA_HOME, MAVEN_HOME
    and BIRT_RUNTIME_DIR to the .env file next to this script.

    Git ignores .env, so no machine-specific path reaches the repository. The file
    .env.example documents the keys.

.PARAMETER JavaHome
    JDK 21 installation root. It must contain bin\java.exe.
    The script detects this path when you omit the parameter.

.PARAMETER MavenHome
    Apache Maven installation root. It must contain bin\mvn.cmd.
    The script detects this path when you omit the parameter.

.PARAMETER BirtRuntimeDir
    The unpacked birt-runtime-4.24.0 directory that contains ReportEngine\lib.
    This parameter is optional, and a value enables RuntimeSmokeIT. The script never
    guesses this path. You must pass it, or you must set $env:BIRT_RUNTIME_DIR.

.PARAMETER NonInteractive
    The script never prompts. It exits with code 1 and names the parameter to pass.

.PARAMETER Force
    The script overwrites an existing .env file without a question.

.EXAMPLE
    .\setup.ps1

.EXAMPLE
    .\setup.ps1 -JavaHome C:\Path\To\jdk-21 -MavenHome C:\Path\To\apache-maven-3.9.9 -NonInteractive
#>
[CmdletBinding()]
param(
    [string] $JavaHome,
    [string] $MavenHome,
    [string] $BirtRuntimeDir,
    [switch] $NonInteractive,
    [switch] $Force
)

$ErrorActionPreference = 'Stop'

# BIRT 4.24 targets Java 21. An older JDK cannot run this build.
$MinimumJavaMajor = 21
$PreferredJavaMajor = 21

$EnvFilePath = Join-Path $PSScriptRoot '.env'

function ConvertTo-CleanPath {
    param([string] $Path)
    if ([string]::IsNullOrWhiteSpace($Path)) { return '' }
    $trimmed = $Path.Trim().Trim('"').Trim("'").Trim()
    if ($trimmed.Length -gt 3) { $trimmed = $trimmed.TrimEnd('\', '/') }
    return $trimmed
}

# Runs <path>\bin\java.exe -version. Returns the path, the raw version string and the
# major version. Returns $null when the path is not a usable Java installation.
function Get-JdkInfo {
    param([string] $Path)

    $jdkHome = ConvertTo-CleanPath $Path
    if ($jdkHome -eq '') { return $null }

    $exe = Join-Path $jdkHome 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $exe -PathType Leaf)) { return $null }

    try {
        $output = (& $exe -version 2>&1) | ForEach-Object { $_.ToString() }
    }
    catch {
        return $null
    }
    if (-not $output) { return $null }

    $banner = ($output | Select-Object -First 1)
    $match = [regex]::Match(($output -join "`n"), 'version\s+"([^"]+)"')
    if (-not $match.Success) { return $null }

    $version = $match.Groups[1].Value
    # "21.0.12.1" -> 21, "25.0.3" -> 25, "1.8.0_501" -> 8
    $numbers = @([regex]::Matches($version, '\d+') | ForEach-Object { [int] $_.Value })
    if ($numbers.Count -eq 0) { return $null }
    $major = if ($numbers[0] -eq 1 -and $numbers.Count -gt 1) { $numbers[1] } else { $numbers[0] }

    return [pscustomobject]@{
        Path    = $jdkHome
        Version = $version
        Major   = $major
        Banner  = $banner
    }
}

# Collects the candidate JAVA_HOME values, most trusted first, and returns the first
# usable one.
function Get-JavaHomeCandidate {
    param([string] $Explicit)

    $candidates = [System.Collections.Generic.List[string]]::new()

    if (-not [string]::IsNullOrWhiteSpace($Explicit)) { $candidates.Add($Explicit) }
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) { $candidates.Add($env:JAVA_HOME) }

    foreach ($registryRoot in @('HKLM:\SOFTWARE\JavaSoft\JDK', 'HKLM:\SOFTWARE\Eclipse Adoptium\JDK')) {
        if (-not (Test-Path -LiteralPath $registryRoot)) { continue }
        try {
            Get-ChildItem -LiteralPath $registryRoot -ErrorAction Stop |
                Sort-Object -Property PSChildName -Descending |
                ForEach-Object {
                    $value = (Get-ItemProperty -LiteralPath $_.PSPath -ErrorAction SilentlyContinue).JavaHome
                    if (-not [string]::IsNullOrWhiteSpace($value)) { $candidates.Add($value) }
                }
        }
        catch {
            Write-Verbose "Cannot read $registryRoot : $($_.Exception.Message)"
        }
    }

    # The usual install roots. The script derives them from the environment, so this
    # file holds no absolute path.
    $searchRoots = [System.Collections.Generic.List[string]]::new()
    foreach ($programFiles in @($env:ProgramFiles, ${env:ProgramFiles(x86)})) {
        if ([string]::IsNullOrWhiteSpace($programFiles)) { continue }
        foreach ($vendor in @('Java', 'Eclipse Adoptium', 'Microsoft')) {
            $searchRoots.Add((Join-Path $programFiles $vendor))
        }
    }
    if (-not [string]::IsNullOrWhiteSpace($env:USERPROFILE)) {
        $searchRoots.Add((Join-Path $env:USERPROFILE '.jdks'))
    }

    foreach ($searchRoot in $searchRoots) {
        if ([string]::IsNullOrWhiteSpace($searchRoot) -or -not (Test-Path -LiteralPath $searchRoot -PathType Container)) { continue }
        Get-ChildItem -LiteralPath $searchRoot -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue |
            Sort-Object -Property Name -Descending |
            ForEach-Object { $candidates.Add($_.FullName) }
    }

    $seen = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
    $usable = [System.Collections.Generic.List[object]]::new()
    foreach ($candidate in $candidates) {
        $clean = ConvertTo-CleanPath $candidate
        if ($clean -eq '' -or -not $seen.Add($clean)) { continue }
        $info = Get-JdkInfo $clean
        if ($null -ne $info -and $info.Major -ge $MinimumJavaMajor) { $usable.Add($info) }
    }

    # BIRT 4.24 targets Java 21. Prefer an exact 21.x over a newer JDK.
    $exact = $usable | Where-Object { $_.Major -eq $PreferredJavaMajor } | Select-Object -First 1
    if ($exact) { return $exact }
    return ($usable | Select-Object -First 1)
}

function Get-MavenHomeInfo {
    param([string] $Path)

    $mavenRoot = ConvertTo-CleanPath $Path
    if ($mavenRoot -eq '') { return $null }

    $mvn = Join-Path $mavenRoot 'bin\mvn.cmd'
    if (-not (Test-Path -LiteralPath $mvn -PathType Leaf)) { return $null }

    return [pscustomobject]@{ Path = $mavenRoot; Mvn = $mvn }
}

function Get-MavenHomeCandidate {
    param([string] $Explicit)

    $candidates = [System.Collections.Generic.List[string]]::new()
    if (-not [string]::IsNullOrWhiteSpace($Explicit))       { $candidates.Add($Explicit) }
    if (-not [string]::IsNullOrWhiteSpace($env:MAVEN_HOME)) { $candidates.Add($env:MAVEN_HOME) }
    if (-not [string]::IsNullOrWhiteSpace($env:M2_HOME))    { $candidates.Add($env:M2_HOME) }

    $onPath = Get-Command mvn.cmd -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($onPath) {
        # <home>\bin\mvn.cmd -> <home>
        $binDir = Split-Path -Parent $onPath.Source
        if ($binDir) { $candidates.Add((Split-Path -Parent $binDir)) }
    }

    foreach ($candidate in $candidates) {
        $info = Get-MavenHomeInfo $candidate
        if ($null -ne $info) { return $info }
    }
    return $null
}

function Get-BirtRuntimeInfo {
    param([string] $Path)

    $dir = ConvertTo-CleanPath $Path
    if ($dir -eq '') { return $null }
    if (-not (Test-Path -LiteralPath (Join-Path $dir 'ReportEngine\lib') -PathType Container)) { return $null }
    return [pscustomobject]@{ Path = $dir }
}

function Read-ValidatedValue {
    param(
        [string]   $Prompt,
        [scriptblock] $Validator,
        [string]   $ParameterName,
        [string]   $Requirement
    )

    if ($NonInteractive) {
        Write-Host ''
        Write-Host "setup.ps1: cannot determine $ParameterName." -ForegroundColor Red
        Write-Host "  $Requirement"
        Write-Host "  Run setup.ps1 again with -$ParameterName <path>. This run is non-interactive, so it does not prompt."
        exit 1
    }

    while ($true) {
        $answer = Read-Host $Prompt
        if ([string]::IsNullOrWhiteSpace($answer)) {
            Write-Host "  A value is required. $Requirement" -ForegroundColor Yellow
            continue
        }
        $result = & $Validator $answer
        if ($null -ne $result) { return $result }
        Write-Host "  This path is not usable: $answer" -ForegroundColor Yellow
        Write-Host "  $Requirement" -ForegroundColor Yellow
    }
}

# --------------------------------------------------------------------------------------
# JAVA_HOME
# --------------------------------------------------------------------------------------
Write-Host 'Detecting toolchain...'

if (-not [string]::IsNullOrWhiteSpace($JavaHome)) {
    $jdk = Get-JdkInfo $JavaHome
    if ($null -eq $jdk) {
        Write-Host "setup.ps1: -JavaHome '$JavaHome' has no bin\java.exe, or java did not run." -ForegroundColor Red
        exit 1
    }
    if ($jdk.Major -lt $MinimumJavaMajor) {
        Write-Host "setup.ps1: -JavaHome '$JavaHome' is Java $($jdk.Major) ($($jdk.Version)). BIRT 4.24 needs Java $MinimumJavaMajor or newer." -ForegroundColor Red
        exit 1
    }
}
else {
    $jdk = Get-JavaHomeCandidate -Explicit $null
}

if ($null -eq $jdk) {
    $jdk = Read-ValidatedValue `
        -Prompt        "Path to a JDK $PreferredJavaMajor installation root" `
        -ParameterName 'JavaHome' `
        -Requirement   "It must contain bin\java.exe and report Java $MinimumJavaMajor or newer." `
        -Validator     {
            param($value)
            $info = Get-JdkInfo $value
            if ($null -ne $info -and $info.Major -ge $MinimumJavaMajor) { return $info }
            return $null
        }
}

# --------------------------------------------------------------------------------------
# MAVEN_HOME
# --------------------------------------------------------------------------------------
if (-not [string]::IsNullOrWhiteSpace($MavenHome)) {
    $maven = Get-MavenHomeInfo $MavenHome
    if ($null -eq $maven) {
        Write-Host "setup.ps1: -MavenHome '$MavenHome' has no bin\mvn.cmd." -ForegroundColor Red
        exit 1
    }
}
else {
    $maven = Get-MavenHomeCandidate -Explicit $null
}

if ($null -eq $maven) {
    $maven = Read-ValidatedValue `
        -Prompt        'Path to an Apache Maven 3.9.x installation root' `
        -ParameterName 'MavenHome' `
        -Requirement   'It must contain bin\mvn.cmd.' `
        -Validator     { param($value) Get-MavenHomeInfo $value }
}

# --------------------------------------------------------------------------------------
# BIRT_RUNTIME_DIR (optional; the script never guesses it)
# --------------------------------------------------------------------------------------
$birt = $null
if (-not [string]::IsNullOrWhiteSpace($BirtRuntimeDir)) {
    $birt = Get-BirtRuntimeInfo $BirtRuntimeDir
    if ($null -eq $birt) {
        Write-Host "setup.ps1: -BirtRuntimeDir '$BirtRuntimeDir' does not contain ReportEngine\lib." -ForegroundColor Red
        exit 1
    }
}
elseif (-not [string]::IsNullOrWhiteSpace($env:BIRT_RUNTIME_DIR)) {
    $birt = Get-BirtRuntimeInfo $env:BIRT_RUNTIME_DIR
    if ($null -eq $birt) {
        Write-Warning "`$env:BIRT_RUNTIME_DIR does not contain ReportEngine\lib. The script leaves BIRT_RUNTIME_DIR empty."
    }
}

# --------------------------------------------------------------------------------------
# Write .env
# --------------------------------------------------------------------------------------
if ((Test-Path -LiteralPath $EnvFilePath -PathType Leaf) -and -not $Force) {
    if ($NonInteractive) {
        Write-Host ''
        Write-Host "setup.ps1: .env already exists. Run setup.ps1 again with -Force to overwrite it." -ForegroundColor Red
        exit 1
    }
    $answer = Read-Host '.env already exists. Overwrite it? [y/N]'
    if ($answer -notmatch '^(y|yes)$') {
        Write-Host 'Stopped. The script did not change .env.'
        exit 1
    }
}

$birtValue = if ($null -ne $birt) { $birt.Path } else { '' }

$lines = @(
    '# setup.ps1 generated this file. Git ignores it, and it must stay that way.',
    '# The file pins the toolchain for build.ps1 and overrides the process environment.',
    '# See .env.example for the meaning of the keys. Run .\setup.ps1 -Force to write it again.',
    "JAVA_HOME=$($jdk.Path)",
    "MAVEN_HOME=$($maven.Path)",
    "BIRT_RUNTIME_DIR=$birtValue"
)

$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllText($EnvFilePath, (($lines -join "`r`n") + "`r`n"), $utf8NoBom)

Write-Host ''
Write-Host "Wrote $EnvFilePath" -ForegroundColor Green
Write-Host "  JAVA_HOME        = $($jdk.Path)"
Write-Host "                     $($jdk.Banner)"
Write-Host "  MAVEN_HOME       = $($maven.Path)"
if ($birtValue -ne '') {
    Write-Host "  BIRT_RUNTIME_DIR = $birtValue"
    Write-Host '                     RuntimeSmokeIT runs. build.ps1 passes -Dbirt.runtime.dir by itself.'
}
else {
    Write-Host '  BIRT_RUNTIME_DIR = (empty). RuntimeSmokeIT stays skipped.'
    Write-Host '                     Pass -BirtRuntimeDir <unpacked birt-runtime> to enable that test.'
}
Write-Host ''
Write-Host 'Next: .\build.ps1 clean verify'
