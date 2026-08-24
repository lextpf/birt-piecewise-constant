#Requires -Version 7.0
param([Parameter(ValueFromRemainingArguments = $true)][string[]]$MvnArgs)

# Builds the plug-in jar with the pinned toolchain of this project. This script runs no
# test. Use .\test.ps1 to run the tests and to read the results.
#
# toolchain.ps1 holds the shared part: it reads .env, it validates JAVA_HOME and
# MAVEN_HOME, and it runs Maven.
#
# Behaviour:
#   .\build.ps1                       runs "mvn -B -ntp -DskipTests clean package"
#   .\build.ps1 <goals and options>   runs "mvn -B -ntp -DskipTests <goals and options>"
#
# The script always prepends -DskipTests. Maven reads the command line from left to
# right, and the last value of a property wins. A caller can therefore switch the tests
# back on:
#
#   .\build.ps1 clean install -DskipTests=false
#
# That command runs "mvn -B -ntp -DskipTests clean install -DskipTests=false", so Maven
# runs the tests. The script exits with the exit code of Maven.

. "$PSScriptRoot\toolchain.ps1"
# toolchain.ps1 prints the help screen when a path is missing or wrong. It never ends the
# process, so this script must stop by itself.
if (-not $ToolchainReady) { exit 1 }

# The default goals build the jar and the sources jar into build\.
$goals = [string[]]@('clean', 'package')
if ($MvnArgs -and @($MvnArgs).Count -gt 0) { $goals = @($MvnArgs) }

$exitCode = Invoke-Maven (@('-DskipTests') + $goals)
exit $exitCode
