#Requires -Version 7.0

# Shared toolchain part of build.ps1 and test.ps1. Dot-source this file from a script in
# this directory, and stop at once when the toolchain is not valid:
#
#     . "$PSScriptRoot\toolchain.ps1"
#     if (-not $ToolchainReady) { exit 1 }
#
# This file holds no hard-coded toolchain path. The paths come from the .env file next to
# this file. Git ignores that file, and .\setup.ps1 writes it. If .env is missing, then
# this file reads the paths from the process environment. .env overrides the process
# environment: it is the pinned toolchain of the project, and a machine-wide JAVA_HOME
# often points at a different JDK.
#
# This file does two things on its own, and nothing else. It resolves the three settings,
# and it validates JAVA_HOME and MAVEN_HOME. It starts no build. The caller must call
# Invoke-Maven to run Maven.
#
# If a setting is missing or wrong, then this file prints the help screen and sets
# $ToolchainReady to $false. It never ends the process. PowerShell runs a dot-sourced file
# in the scope of the caller, so a process exit here would also kill an interactive shell.
# The guard line above is therefore the duty of each caller.
#
# The file gives these members to the calling script:
#
#   $ToolchainReady           $true only when every check passed
#   $ToolchainJavaHome        validated JDK root
#   $ToolchainMavenHome       validated Apache Maven root
#   $ToolchainBirtRuntimeDir  unpacked BIRT runtime, or '' when nobody configured one
#   $ToolchainMvn             full path of <MAVEN_HOME>\bin\mvn.cmd
#   Invoke-Maven              runs Maven and returns the exit code
#   Show-ToolchainHelp        prints the help screen

$ToolchainMinimumJavaMajor = 21
$ToolchainEnvFilePath = Join-Path $PSScriptRoot '.env'

# The name of the script that dot-sources this file. The help screen names it, so the
# reader knows which command failed.
$ToolchainCallerName = 'toolchain.ps1'
if ($MyInvocation.PSCommandPath) {
    $ToolchainCallerName = Split-Path -Leaf $MyInvocation.PSCommandPath
}

# --------------------------------------------------------------------------------------
# Settings
# --------------------------------------------------------------------------------------

# Reads .env: one KEY=value per line. A line that starts with # is a comment. The reader
# removes a pair of surrounding quotes from the value.
$ToolchainFileSettings = @{}
if (Test-Path -LiteralPath $ToolchainEnvFilePath -PathType Leaf) {
    foreach ($line in (Get-Content -LiteralPath $ToolchainEnvFilePath -Encoding utf8)) {
        $text = $line.Trim()
        if ($text -eq '' -or $text.StartsWith('#')) { continue }
        $split = $text.IndexOf('=')
        if ($split -lt 1) { continue }
        $key = $text.Substring(0, $split).Trim()
        $value = $text.Substring($split + 1).Trim()
        if ($value.Length -ge 2 -and
            (($value[0] -eq '"' -and $value[-1] -eq '"') -or ($value[0] -eq "'" -and $value[-1] -eq "'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $ToolchainFileSettings[$key] = $value
    }
}

# Returns the value of one setting. .env wins over the process environment.
function Get-Setting {
    param([string] $Name)
    if ($ToolchainFileSettings.ContainsKey($Name)) { return $ToolchainFileSettings[$Name] }
    return [System.Environment]::GetEnvironmentVariable($Name)
}

function ConvertTo-CleanPath {
    param([string] $Path)
    if ([string]::IsNullOrWhiteSpace($Path)) { return '' }
    $trimmed = $Path.Trim().Trim('"').Trim("'").Trim()
    if ($trimmed.Length -gt 3) { $trimmed = $trimmed.TrimEnd('\', '/') }
    return $trimmed
}

function Show-ToolchainHelp {
    param([string] $Problem)

    $source = if (Test-Path -LiteralPath $ToolchainEnvFilePath -PathType Leaf) { $ToolchainEnvFilePath } else { '(no .env; the script reads the values from the environment)' }
    Write-Host ''
    Write-Host "$($ToolchainCallerName): $Problem" -ForegroundColor Red
    Write-Host ''
    Write-Host 'These scripts need two paths. They read them from .env next to toolchain.ps1, which'
    Write-Host 'overrides the process environment. They read the process environment when there'
    Write-Host 'is no .env:'
    Write-Host ''
    Write-Host '  JAVA_HOME         JDK 21 root. It must contain bin\java.exe. BIRT 4.24 needs Java 21 or newer.'
    Write-Host '  MAVEN_HOME        Apache Maven root. It must contain bin\mvn.cmd.'
    Write-Host '  BIRT_RUNTIME_DIR  Optional. An unpacked birt-runtime with ReportEngine\lib. It enables RuntimeSmokeIT.'
    Write-Host ''
    Write-Host "  settings source:  $source"
    Write-Host ''
    Write-Host 'Use one of these three ways to set the paths:'
    Write-Host '  .\setup.ps1                       detects a JDK 21 and Maven, then writes the git-ignored .env'
    Write-Host '  .\setup.ps1 -JavaHome <path> -MavenHome <path> [-BirtRuntimeDir <path>]'
    Write-Host '  $env:JAVA_HOME = <path>; $env:MAVEN_HOME = <path>     (no .env needed)'
    Write-Host ''
    Write-Host 'See .env.example for the keys, and README.md > Building from source for the details.'
    Write-Host ''
}

# --------------------------------------------------------------------------------------
# Resolution and validation
# --------------------------------------------------------------------------------------

# Resolves the three settings, and validates JAVA_HOME and MAVEN_HOME. Returns $true when
# every check passed. The function prints the help screen and returns $false at the first
# problem. It ends no process, so an interactive shell survives a broken .env.
function Test-Toolchain {
    $script:ToolchainJavaHome = ConvertTo-CleanPath (Get-Setting 'JAVA_HOME')
    $script:ToolchainMavenHome = ConvertTo-CleanPath (Get-Setting 'MAVEN_HOME')
    $script:ToolchainBirtRuntimeDir = ConvertTo-CleanPath (Get-Setting 'BIRT_RUNTIME_DIR')
    $script:ToolchainMvn = ''

    if ($script:ToolchainJavaHome -eq '') {
        Show-ToolchainHelp 'JAVA_HOME is not set.'
        return $false
    }

    $javaExe = Join-Path $script:ToolchainJavaHome 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $javaExe -PathType Leaf)) {
        Show-ToolchainHelp "JAVA_HOME = '$($script:ToolchainJavaHome)' has no bin\java.exe."
        return $false
    }

    $javaOutput = @((& $javaExe -version 2>&1) | ForEach-Object { $_.ToString() })
    $javaMatch = [regex]::Match(($javaOutput -join "`n"), 'version\s+"([^"]+)"')
    if (-not $javaMatch.Success) {
        Show-ToolchainHelp "Cannot read a version from '$javaExe -version'."
        return $false
    }

    $javaVersion = $javaMatch.Groups[1].Value
    # "21.0.12.1" -> 21, "25.0.3" -> 25, "1.8.0_501" -> 8
    $javaNumbers = @([regex]::Matches($javaVersion, '\d+') | ForEach-Object { [int] $_.Value })
    if ($javaNumbers.Count -eq 0) {
        Show-ToolchainHelp "Cannot parse the Java version '$javaVersion' that '$javaExe' reports."
        return $false
    }
    $javaMajor = if ($javaNumbers[0] -eq 1 -and $javaNumbers.Count -gt 1) { $javaNumbers[1] } else { $javaNumbers[0] }

    if ($javaMajor -lt $ToolchainMinimumJavaMajor) {
        Show-ToolchainHelp "JAVA_HOME = '$($script:ToolchainJavaHome)' is Java $javaMajor ($javaVersion); this build needs Java $ToolchainMinimumJavaMajor or newer."
        return $false
    }

    if ($script:ToolchainMavenHome -eq '') {
        Show-ToolchainHelp 'MAVEN_HOME is not set.'
        return $false
    }

    $mvn = Join-Path $script:ToolchainMavenHome 'bin\mvn.cmd'
    if (-not (Test-Path -LiteralPath $mvn -PathType Leaf)) {
        Show-ToolchainHelp "MAVEN_HOME = '$($script:ToolchainMavenHome)' has no bin\mvn.cmd."
        return $false
    }

    $script:ToolchainMvn = $mvn
    return $true
}

# --------------------------------------------------------------------------------------
# Invoke-Maven
# --------------------------------------------------------------------------------------

<#
.SYNOPSIS
    Runs Maven with the pinned toolchain and returns the exit code.

.DESCRIPTION
    The function points Maven at the pinned JDK. It then runs
    <MAVEN_HOME>\bin\mvn.cmd -B -ntp with the arguments of the caller.

    If BIRT_RUNTIME_DIR is configured, then the function appends
    -Dbirt.runtime.dir=<dir>. It appends nothing when the caller passes an own
    -Dbirt.runtime.dir value. An explicit value therefore always wins.

    The function writes the Maven output to the console. If the caller passes -LogPath,
    then the function writes every output line to that file instead. The caller can pass
    -OnLine to print a own progress line for each output line.

    The function throws when the toolchain is not valid. The caller must check
    $ToolchainReady right after the dot-source, and must stop on $false.

.PARAMETER Arguments
    The goals and options for Maven, in the order of the command line.

.PARAMETER LogPath
    Optional. The file that takes the complete Maven output. The parent directory must
    exist.

.PARAMETER OnLine
    Optional. A script block that the function calls with each output line. Use it
    together with -LogPath to show a compact progress report.

.OUTPUTS
    The exit code of Maven.
#>
function Invoke-Maven {
    param(
        [Parameter(Position = 0)][AllowEmptyCollection()][string[]] $Arguments = @(),
        [string] $LogPath = '',
        [scriptblock] $OnLine = $null
    )

    if (-not $script:ToolchainReady) {
        throw 'Invoke-Maven: the toolchain is not valid. The calling script must stop when $ToolchainReady is $false.'
    }

    # A native command writes to the error stream. That output must not stop this script.
    $ErrorActionPreference = 'Continue'

    $env:JAVA_HOME = $script:ToolchainJavaHome
    $env:Path = "$($script:ToolchainJavaHome)\bin;$env:Path"

    $mavenArguments = [string[]]@()
    if ($Arguments) { $mavenArguments = @($Arguments | Where-Object { $null -ne $_ }) }

    # A configured runtime must run RuntimeSmokeIT, unless the caller passed an own value.
    if ($script:ToolchainBirtRuntimeDir -ne '' -and -not ($mavenArguments | Where-Object { $_ -and $_.StartsWith('-Dbirt.runtime.dir=') })) {
        $mavenArguments += "-Dbirt.runtime.dir=$($script:ToolchainBirtRuntimeDir)"
    }

    if ($LogPath -eq '') {
        & $script:ToolchainMvn -B -ntp @mavenArguments | Out-Host
        return $LASTEXITCODE
    }

    $writer = [System.IO.StreamWriter]::new($LogPath, $false, [System.Text.UTF8Encoding]::new($false))
    try {
        & $script:ToolchainMvn -B -ntp @mavenArguments 2>&1 | ForEach-Object {
            $line = if ($_ -is [System.Management.Automation.ErrorRecord]) { $_.ToString() } else { [string] $_ }
            $writer.WriteLine($line)
            if ($OnLine) { & $OnLine $line }
        }
    }
    finally {
        $writer.Flush()
        $writer.Dispose()
    }
    return $LASTEXITCODE
}

# The caller must read this flag right after the dot-source, and must stop on $false.
$ToolchainReady = Test-Toolchain
