param([Parameter(ValueFromRemainingArguments = $true)][string[]]$MvnArgs)

# This script holds no hard-coded toolchain path. The paths come from the .env file next
# to this script. Git ignores that file, and .\setup.ps1 writes it. If .env is missing,
# then this script reads the paths from the process environment. .env overrides the
# process environment: it is the pinned toolchain of the project, and a machine-wide
# JAVA_HOME often points at a different JDK.

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

    $source = if (Test-Path -LiteralPath $EnvFilePath -PathType Leaf) { $EnvFilePath } else { '(no .env; the script reads the values from the environment)' }
    Write-Host ''
    Write-Host "build.ps1: $Problem" -ForegroundColor Red
    Write-Host ''
    Write-Host 'This build needs two paths. It reads them from .env next to build.ps1, which'
    Write-Host 'overrides the process environment. It reads the process environment when there'
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
    Show-ToolchainHelp "Cannot read a version from '$javaExe -version'."
    exit 1
}
$javaVersion = $javaMatch.Groups[1].Value
# "21.0.12.1" -> 21, "25.0.3" -> 25, "1.8.0_501" -> 8
$javaNumbers = @([regex]::Matches($javaVersion, '\d+') | ForEach-Object { [int] $_.Value })
if ($javaNumbers.Count -eq 0) {
    Show-ToolchainHelp "Cannot parse the Java version '$javaVersion' that '$javaExe' reports."
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

# A configured runtime must run RuntimeSmokeIT, unless the caller passed its own value.
if ($birtRuntimeDir -ne '' -and -not ($mvnArguments | Where-Object { $_ -and $_.StartsWith('-Dbirt.runtime.dir=') })) {
    $mvnArguments += "-Dbirt.runtime.dir=$birtRuntimeDir"
}

& $mvn -B -ntp @mvnArguments
exit $LASTEXITCODE
