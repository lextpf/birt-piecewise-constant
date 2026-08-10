param([Parameter(ValueFromRemainingArguments = $true)][string[]]$MvnArgs)
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
& 'C:\Users\Alex\source\repos\maven\bin\mvn.cmd' -B -ntp @MvnArgs
exit $LASTEXITCODE
