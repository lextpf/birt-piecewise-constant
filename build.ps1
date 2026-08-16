param([Parameter(ValueFromRemainingArguments = $true)][string[]]$MvnArgs)

# Toolchain paths are never hard-coded here. They come from .env next to this script
# (git-ignored, written by .\setup.ps1) and, if that file is missing, from the process
# environment. .env wins: it is the project's pinned toolchain, while a machine-wide
# JAVA_HOME may well point at a different JDK.

$MinimumJavaMajor = 21
$EnvFilePath = Join-Path $PSScriptRoot '.env'

$fileSettings = @{}
if (Test-Path -LiteralPath $EnvFilePath -PathType Leaf) {
    foreach ($line in (Get-Content -LiteralPath $EnvFilePath -Encoding utf8)) {
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
        $fileSettings[$key] = $value
    }
}

function Get-Setting {
    param([string] $Name)
    if ($fileSettings.ContainsKey($Name)) { return $fileSettings[$Name] }
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

    $source = if (Test-Path -LiteralPath $EnvFilePath -PathType Leaf) { $EnvFilePath } else { '(no .env - values read from the environment)' }
    Write-Host ''
    Write-Host "build.ps1: $Problem" -ForegroundColor Red
    Write-Host ''
    Write-Host 'This build needs two paths, and takes them from .env next to build.ps1 (which wins)'
    Write-Host 'or, when there is no .env, from the process environment:'
    Write-Host ''
    Write-Host '  JAVA_HOME         JDK 21 root, must contain bin\java.exe (BIRT 4.24 needs Java 21+)'
    Write-Host '  MAVEN_HOME        Apache Maven root, must contain bin\mvn.cmd'
    Write-Host '  BIRT_RUNTIME_DIR  optional; unpacked birt-runtime with ReportEngine\lib, enables RuntimeSmokeIT'
    Write-Host ''
    Write-Host "  settings source:  $source"
    Write-Host ''
    Write-Host 'Fix it either way:'
    Write-Host '  .\setup.ps1                       auto-detects a JDK 21 and Maven and writes the git-ignored .env'
    Write-Host '  .\setup.ps1 -JavaHome <path> -MavenHome <path> [-BirtRuntimeDir <path>]'
    Write-Host '  $env:JAVA_HOME = <path>; $env:MAVEN_HOME = <path>     (no .env needed)'
    Write-Host ''
    Write-Host 'See .env.example for the keys and README.md > Build for the full story.'
    Write-Host ''
}

$javaHome = ConvertTo-CleanPath (Get-Setting 'JAVA_HOME')
$mavenHome = ConvertTo-CleanPath (Get-Setting 'MAVEN_HOME')
$birtRuntimeDir = ConvertTo-CleanPath (Get-Setting 'BIRT_RUNTIME_DIR')

if ($javaHome -eq '') {
    Show-ToolchainHelp 'JAVA_HOME is not set.'
    exit 1
}

$javaExe = Join-Path $javaHome 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javaExe -PathType Leaf)) {
    Show-ToolchainHelp "JAVA_HOME = '$javaHome' has no bin\java.exe."
    exit 1
}

$javaOutput = @((& $javaExe -version 2>&1) | ForEach-Object { $_.ToString() })
$javaMatch = [regex]::Match(($javaOutput -join "`n"), 'version\s+"([^"]+)"')
if (-not $javaMatch.Success) {
    Show-ToolchainHelp "Could not read a version from '$javaExe -version'."
    exit 1
}
$javaVersion = $javaMatch.Groups[1].Value
# "21.0.12.1" -> 21, "25.0.3" -> 25, "1.8.0_501" -> 8
$javaNumbers = @([regex]::Matches($javaVersion, '\d+') | ForEach-Object { [int] $_.Value })
if ($javaNumbers.Count -eq 0) {
    Show-ToolchainHelp "Could not parse the Java version '$javaVersion' reported by '$javaExe'."
    exit 1
}
$javaMajor = if ($javaNumbers[0] -eq 1 -and $javaNumbers.Count -gt 1) { $javaNumbers[1] } else { $javaNumbers[0] }

if ($javaMajor -lt $MinimumJavaMajor) {
    Show-ToolchainHelp "JAVA_HOME = '$javaHome' is Java $javaMajor ($javaVersion); this build needs Java $MinimumJavaMajor or newer."
    exit 1
}

if ($mavenHome -eq '') {
    Show-ToolchainHelp 'MAVEN_HOME is not set.'
    exit 1
}

$mvn = Join-Path $mavenHome 'bin\mvn.cmd'
if (-not (Test-Path -LiteralPath $mvn -PathType Leaf)) {
    Show-ToolchainHelp "MAVEN_HOME = '$mavenHome' has no bin\mvn.cmd."
    exit 1
}

$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"

$mvnArguments = [string[]]@()
if ($MvnArgs) { $mvnArguments = @($MvnArgs) }

# A configured runtime should just run RuntimeSmokeIT - unless the caller passed its own.
if ($birtRuntimeDir -ne '' -and -not ($mvnArguments | Where-Object { $_ -and $_.StartsWith('-Dbirt.runtime.dir=') })) {
    $mvnArguments += "-Dbirt.runtime.dir=$birtRuntimeDir"
}

& $mvn -B -ntp @mvnArguments
exit $LASTEXITCODE
