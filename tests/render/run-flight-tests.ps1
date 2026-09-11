param(
    [Parameter(Mandatory = $true)][string]$ClasspathFile,
    [string]$JavaBin = '',
    [string]$OutputDirectory = (Join-Path ([IO.Path]::GetTempPath()) ('magicaland-flight-tests-' + [guid]::NewGuid().ToString('N')))
)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$flightRepo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$flightDependencies = (Get-Content -Raw -LiteralPath $ClasspathFile).Trim()
$flightJavac = if ($JavaBin) { Join-Path $JavaBin 'javac.exe' } else { 'javac' }
$flightJava = if ($JavaBin) { Join-Path $JavaBin 'java.exe' } else { 'java' }
if (Test-Path -LiteralPath $OutputDirectory) { throw 'Choose a fresh test output directory.' }
$flightRun = (New-Item -ItemType Directory -Path $OutputDirectory).FullName
$flightSources = @(
    'src/client/java/top/csituka/magicaland/client/animation/PonyFlightAnimations.java',
    'src/client/java/top/csituka/magicaland/client/animation/PonyFlightMotion.java',
    'tests/render/PonyFlightAnimationsTest.java',
    'tests/render/PonyFlightMotionTest.java',
    'src/client/java/top/csituka/magicaland/client/render/UnicornFlightRimMath.java',
    'tests/render/UnicornFlightRimTest.java',
    'tests/render/UnicornFlightRimStructureTest.java'
) | ForEach-Object { Join-Path $flightRepo $_ }
function Run-FlightJava([string]$Command, [string[]]$Arguments, [string]$Name) {
    $flightArgumentFile = Join-Path $flightRun ($Name + '.args')
    [IO.File]::WriteAllLines($flightArgumentFile, @($Arguments | ForEach-Object {
        '"' + $_.Replace('\', '/').Replace('"', '\"') + '"'
    }), [Text.UTF8Encoding]::new($false))
    $flightOutput = @(& $Command ('@' + $flightArgumentFile) 2>&1 | ForEach-Object { $_.ToString() })
    $flightExit = $LASTEXITCODE
    $flightOutput | Tee-Object -FilePath (Join-Path $flightRun ($Name + '.log'))
    if ($flightExit -ne 0 -or ($flightOutput -match 'An exception has occurred in the compiler')) { throw "$Name failed" }
}
Run-FlightJava $flightJavac (@('--release', '17', '-proc:none', '-encoding', 'UTF-8', '-cp', $flightDependencies,
    '-d', $flightRun) + $flightSources) 'compile'
foreach ($flightTest in @('PonyFlightMotionTest', 'PonyFlightAnimationsTest')) {
    Run-FlightJava $flightJava @('-ea', '-cp', ($flightRun + [IO.Path]::PathSeparator + $flightDependencies),
        ('top.csituka.magicaland.client.animation.' + $flightTest), $flightRepo) $flightTest
}
$flightMinecraft = $flightDependencies.Split([IO.Path]::PathSeparator) | Where-Object { $_ -match 'minecraft-client.*\.jar$' } | Select-Object -First 1
if (!$flightMinecraft) { throw 'Missing named Minecraft client JAR for GUI layer inspection.' }
foreach ($flightTest in @('UnicornFlightRimTest', 'UnicornFlightRimStructureTest')) {
    Run-FlightJava $flightJava @('-ea', '-cp', ($flightRun + [IO.Path]::PathSeparator + $flightDependencies),
        ('top.csituka.magicaland.client.render.' + $flightTest), $flightRepo, $flightMinecraft) $flightTest
}
Write-Output "Evidence: $flightRun"
