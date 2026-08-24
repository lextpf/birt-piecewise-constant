#Requires -Version 7.0
<#
.SYNOPSIS
    Runs the test suite of this project and prints the results.

.DESCRIPTION
    The script runs "mvn -B -ntp test" with the pinned toolchain of this project.
    toolchain.ps1 holds the shared part: it reads .env, it validates JAVA_HOME and
    MAVEN_HOME, and it runs Maven.

    The script writes the complete Maven output to build\test.log. On the console it
    shows one line per test class while the run goes on. It then reads the surefire
    reports in build\surefire-reports and prints a results table, the detail of each
    failed test, and one RESULT line.

    The script deletes the old surefire reports before the run. A report of a class that
    the current run does not touch must not reach the table.

    The script does not pass -Dsurefire.failIfNoSpecifiedTests=false. A -Test pattern
    that matches no class must fail the run. If that happens, then the script says so and
    exits with a non-zero code.

    Exit code 0 means two things at once. Maven succeeded, and the reports hold no
    failure and no error.

.PARAMETER Test
    A surefire test pattern for -Dtest, for example RenderSmokeTest or *ExpanderTest.
    Without this parameter the script runs every test.

.PARAMETER Runtime
    An unpacked birt-runtime directory that contains ReportEngine\lib. The script passes
    it as -Dbirt.runtime.dir, and that value overrides BIRT_RUNTIME_DIR from .env.
    RuntimeSmokeIT runs only with such a directory.

.PARAMETER ShowLog
    The script streams the full Maven output to the console. The log file still gets
    every line.

.PARAMETER MvnArgs
    Further options for Maven. The script forwards them unchanged.

.EXAMPLE
    .\test.ps1

.EXAMPLE
    .\test.ps1 -Test PiecewiseConstantExpanderTest

.EXAMPLE
    .\test.ps1 -Test RuntimeSmokeIT -Runtime C:\birt-runtime-4.24.0
#>
param(
    [string] $Test,
    [string] $Runtime,
    [switch] $ShowLog,
    [Parameter(ValueFromRemainingArguments = $true)][string[]] $MvnArgs
)

. "$PSScriptRoot\toolchain.ps1"
# toolchain.ps1 prints the help screen when a path is missing or wrong. It never ends the
# process, so this script must stop by itself.
if (-not $ToolchainReady) { exit 1 }

$BuildDir = Join-Path $PSScriptRoot 'build'
$ReportDir = Join-Path $BuildDir 'surefire-reports'
$LogPath = Join-Path $BuildDir 'test.log'

if (-not (Test-Path -LiteralPath $BuildDir -PathType Container)) {
    New-Item -ItemType Directory -Path $BuildDir -Force | Out-Null
}

# Surefire keeps a report of an earlier run. The table must show the current run only.
if (Test-Path -LiteralPath $ReportDir -PathType Container) {
    Get-ChildItem -LiteralPath $ReportDir -File -Filter 'TEST-*.xml' -ErrorAction SilentlyContinue |
        Remove-Item -Force -ErrorAction SilentlyContinue
    Get-ChildItem -LiteralPath $ReportDir -File -Filter '*.txt' -ErrorAction SilentlyContinue |
        Remove-Item -Force -ErrorAction SilentlyContinue
}

# --------------------------------------------------------------------------------------
# Maven arguments
# --------------------------------------------------------------------------------------
$mavenArguments = [System.Collections.Generic.List[string]]::new()
$mavenArguments.Add('test')
if (-not [string]::IsNullOrWhiteSpace($Test)) { $mavenArguments.Add("-Dtest=$Test") }
if (-not [string]::IsNullOrWhiteSpace($Runtime)) { $mavenArguments.Add("-Dbirt.runtime.dir=$Runtime") }
if ($MvnArgs) { foreach ($extra in $MvnArgs) { if ($extra) { $mavenArguments.Add($extra) } } }

Write-Host ''
Write-Host "Running: mvn -B -ntp $($mavenArguments -join ' ')"
Write-Host "Log:     $LogPath"
Write-Host ''

# --------------------------------------------------------------------------------------
# Run Maven. Show one compact line per finished test class.
# --------------------------------------------------------------------------------------
# Surefire prints one such line per finished class, for example:
#   [INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.35 s -- in <class>
# A class with a failure gets an extra "<<< FAILURE!" marker before the "-- in" part.
$classResultPattern = '^\[(?:INFO|WARNING|ERROR)\]\s+Tests run:\s*(\d+),\s*Failures:\s*(\d+),\s*Errors:\s*(\d+),\s*Skipped:\s*(\d+),\s*Time elapsed:\s*([\d.,]+)\s*s(?:ec)?\s*(?:<<<\s*[A-Z!]+\s*)?-{1,2}\s+in\s+(\S+)'

$onLine = {
    param([string] $line)

    if ($ShowLog) {
        Write-Host $line
        return
    }

    $match = [regex]::Match($line, $classResultPattern)
    if (-not $match.Success) { return }

    $className = $match.Groups[6].Value
    $shortName = $className.Substring($className.LastIndexOf('.') + 1)
    $bad = ([int] $match.Groups[2].Value) + ([int] $match.Groups[3].Value)
    $mark = if ($bad -gt 0) { 'FAIL' } else { 'ok  ' }
    $colour = if ($bad -gt 0) { 'Red' } else { 'DarkGray' }
    Write-Host ("  {0}  {1,-34} {2,3} tests {3,8} s" -f $mark, $shortName, $match.Groups[1].Value, $match.Groups[5].Value) -ForegroundColor $colour
}

$mavenExit = Invoke-Maven $mavenArguments.ToArray() -LogPath $LogPath -OnLine $onLine

# --------------------------------------------------------------------------------------
# Read the surefire reports
# --------------------------------------------------------------------------------------
function Get-FirstMessageLine {
    param([object] $Node)

    if ($null -eq $Node) { return '' }

    $text = ''
    if ($Node -is [string]) {
        $text = $Node
    }
    else {
        if ($Node.PSObject.Properties.Name -contains 'message' -and $Node.message) { $text = [string] $Node.message }
        elseif ($Node.InnerText) { $text = [string] $Node.InnerText }
    }

    if ([string]::IsNullOrWhiteSpace($text)) { return '(no message)' }
    $first = ($text -split "`r?`n" | Where-Object { $_.Trim() -ne '' } | Select-Object -First 1)
    if ($null -eq $first) { return '(no message)' }
    $first = $first.Trim()
    if ($first.Length -gt 160) { $first = $first.Substring(0, 157) + '...' }
    return $first
}

function ConvertTo-Seconds {
    param([string] $Value)
    if ([string]::IsNullOrWhiteSpace($Value)) { return 0.0 }
    $number = 0.0
    if ([double]::TryParse($Value, [Globalization.NumberStyles]::Float, [Globalization.CultureInfo]::InvariantCulture, [ref] $number)) { return $number }
    if ([double]::TryParse($Value.Replace(',', '.'), [Globalization.NumberStyles]::Float, [Globalization.CultureInfo]::InvariantCulture, [ref] $number)) { return $number }
    return 0.0
}

$reportFiles = @()
if (Test-Path -LiteralPath $ReportDir -PathType Container) {
    $reportFiles = @(Get-ChildItem -LiteralPath $ReportDir -File -Filter 'TEST-*.xml' -ErrorAction SilentlyContinue | Sort-Object Name)
}

# No report means Maven stopped before the tests ran.
if ($reportFiles.Count -eq 0) {
    Write-Host ''
    if (-not [string]::IsNullOrWhiteSpace($Test)) {
        Write-Host "No test matches the pattern '$Test'. Maven wrote no report." -ForegroundColor Red
        Write-Host 'Check the spelling of the pattern. A pattern is a class name, for example RenderSmokeTest or *ExpanderTest.'
    }
    else {
        Write-Host 'Maven wrote no test report. The run stopped before the tests.' -ForegroundColor Red
    }
    Write-Host ''
    Write-Host "Last 30 lines of ${LogPath}:"
    if (Test-Path -LiteralPath $LogPath -PathType Leaf) {
        Get-Content -LiteralPath $LogPath -Tail 30 | ForEach-Object { Write-Host "  $_" }
    }
    Write-Host ''
    Write-Host "Log: $LogPath"
    Write-Host 'RESULT: FAIL (no tests ran)' -ForegroundColor Red
    if ($mavenExit -ne 0) { exit $mavenExit }
    exit 1
}

$rows = [System.Collections.Generic.List[object]]::new()
$problems = [System.Collections.Generic.List[object]]::new()

foreach ($file in $reportFiles) {
    try {
        $document = [xml](Get-Content -LiteralPath $file.FullName -Raw -Encoding utf8)
    }
    catch {
        Write-Host "Cannot read $($file.Name): $($_.Exception.Message)" -ForegroundColor Yellow
        continue
    }

    $suite = $document.testsuite
    if ($null -eq $suite) { continue }

    $className = [string] $suite.name
    $shortName = $className.Substring($className.LastIndexOf('.') + 1)

    $rows.Add([pscustomobject]@{
            Name     = $shortName
            Full     = $className
            Tests    = [int] $suite.tests
            Failures = [int] $suite.failures
            Errors   = [int] $suite.errors
            Skipped  = [int] $suite.skipped
            Seconds  = ConvertTo-Seconds ([string] $suite.time)
        })

    foreach ($case in @($suite.testcase)) {
        if ($null -eq $case) { continue }
        $kind = ''
        $node = $null
        if ($case.PSObject.Properties.Name -contains 'failure' -and $null -ne $case.failure) { $kind = 'FAILED'; $node = $case.failure }
        elseif ($case.PSObject.Properties.Name -contains 'error' -and $null -ne $case.error) { $kind = 'ERROR'; $node = $case.error }
        if ($kind -eq '') { continue }

        $problems.Add([pscustomobject]@{
                Kind    = $kind
                Class   = $shortName
                Method  = [string] $case.name
                Message = Get-FirstMessageLine $node
            })
    }
}

$totalTests = ($rows | Measure-Object -Property Tests -Sum).Sum
$totalFailures = ($rows | Measure-Object -Property Failures -Sum).Sum
$totalErrors = ($rows | Measure-Object -Property Errors -Sum).Sum
$totalSkipped = ($rows | Measure-Object -Property Skipped -Sum).Sum
$totalSeconds = ($rows | Measure-Object -Property Seconds -Sum).Sum
if ($null -eq $totalTests) { $totalTests = 0 }
if ($null -eq $totalFailures) { $totalFailures = 0 }
if ($null -eq $totalErrors) { $totalErrors = 0 }
if ($null -eq $totalSkipped) { $totalSkipped = 0 }
if ($null -eq $totalSeconds) { $totalSeconds = 0.0 }

# --------------------------------------------------------------------------------------
# Results table
# --------------------------------------------------------------------------------------
$classWord = if ($rows.Count -eq 1) { 'class' } else { 'classes' }
$totalsLabel = "TOTAL ($($rows.Count) $classWord)"
$nameWidth = 20
foreach ($row in $rows) { if ($row.Name.Length -gt $nameWidth) { $nameWidth = $row.Name.Length } }
if ($totalsLabel.Length -gt $nameWidth) { $nameWidth = $totalsLabel.Length }

$format = "{0,-$nameWidth}  {1,5}  {2,8}  {3,6}  {4,7}  {5,8}"
$rule = '-' * ($nameWidth + 42)

Write-Host ''
Write-Host ($format -f 'Test class', 'Tests', 'Failures', 'Errors', 'Skipped', 'Seconds')
Write-Host $rule
foreach ($row in ($rows | Sort-Object Name)) {
    $colour = if (($row.Failures + $row.Errors) -gt 0) { 'Red' } else { 'Gray' }
    Write-Host ($format -f $row.Name, $row.Tests, $row.Failures, $row.Errors, $row.Skipped, $row.Seconds.ToString('0.000', [Globalization.CultureInfo]::InvariantCulture)) -ForegroundColor $colour
}
Write-Host $rule
Write-Host ($format -f $totalsLabel, $totalTests, $totalFailures, $totalErrors, $totalSkipped, ([double] $totalSeconds).ToString('0.000', [Globalization.CultureInfo]::InvariantCulture))

if ($problems.Count -gt 0) {
    Write-Host ''
    Write-Host 'Failed tests:' -ForegroundColor Red
    foreach ($problem in $problems) {
        Write-Host ("  {0}  {1}.{2}" -f $problem.Kind, $problem.Class, $problem.Method) -ForegroundColor Red
        Write-Host ("           {0}" -f $problem.Message)
    }
}

# --------------------------------------------------------------------------------------
# Result line and exit code
# --------------------------------------------------------------------------------------
Write-Host ''
Write-Host "Log: $LogPath"

if (($totalFailures + $totalErrors) -gt 0) {
    Write-Host "RESULT: FAIL ($totalFailures failures, $totalErrors errors)" -ForegroundColor Red
    exit 1
}

if ($mavenExit -ne 0) {
    Write-Host "The tests passed, but Maven exited with code $mavenExit." -ForegroundColor Yellow
    Write-Host "Read $LogPath for the reason."
    Write-Host "RESULT: FAIL (Maven exit code $mavenExit)" -ForegroundColor Red
    exit $mavenExit
}

Write-Host "RESULT: PASS ($totalTests tests)" -ForegroundColor Green
exit 0
