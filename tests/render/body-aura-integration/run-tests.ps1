param(
    [Parameter(Mandatory)][string]$ClasspathFile,
    [string]$MinecraftJar = '',
    [string]$JavaBin = '',
    [string]$OutputDirectory = (Join-Path ([IO.Path]::GetTempPath()) ('magicaland-aura-test-' + [guid]::NewGuid().ToString('N')))
)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$dependencies = (Get-Content -Raw -LiteralPath $ClasspathFile).Trim()
$entries = $dependencies.Split([IO.Path]::PathSeparator)
if (!$MinecraftJar) {
    $candidates = @($entries | Where-Object { [IO.Path]::GetFileName($_) -match 'minecraft-client(Only|-widened).*\.jar$' })
    if ($candidates.Count -ne 1) { throw 'Specify -MinecraftJar for the named Minecraft client jar in the classpath.' }
    $MinecraftJar = $candidates[0]
}
$MinecraftJar = (Resolve-Path -LiteralPath $MinecraftJar).Path
if (Test-Path -LiteralPath $OutputDirectory) { throw 'Use a new output directory.' }
$output = (New-Item -ItemType Directory -Path $OutputDirectory).FullName
function Invoke-TestJava([string]$Command, [string[]]$Arguments, [string]$Name) {
    $argFile = Join-Path $output ($Name + '.args')
    [IO.File]::WriteAllLines($argFile, @($Arguments | ForEach-Object { '"' + $_.Replace('\', '/').Replace('"', '\"') + '"' }), [Text.UTF8Encoding]::new($false))
    $executable = if ($JavaBin) { Join-Path $JavaBin $Command } else { $Command }
    & $executable ('@' + $argFile) 2>&1 | Tee-Object (Join-Path $output ($Name + '.log'))
    if ($LASTEXITCODE -ne 0) { throw "$Name failed" }
}
Invoke-TestJava 'javac' @('--release', '17', '-proc:none', '-encoding', 'UTF-8', '-cp', $dependencies, '-d', $output, (Join-Path $PSScriptRoot 'PrepareTestMinecraft.java')) 'prepare-compile'
$widenedJar = Join-Path $output 'minecraft-client-test.jar'
Invoke-TestJava 'java' @('-cp', ($output + [IO.Path]::PathSeparator + $dependencies), 'PrepareTestMinecraft', $MinecraftJar, (Join-Path $repo 'src/main/resources/magicaland.accesswidener'), $widenedJar) 'prepare'
$testDependencies = (@($widenedJar) + @($entries | Where-Object { [IO.Path]::GetFullPath($_) -ne $MinecraftJar })) -join [IO.Path]::PathSeparator
$sources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'stubs') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName)
$sources += Join-Path $PSScriptRoot 'BodyAuraIntegrationTest.java'
$sources += Join-Path $repo 'src/client/java/top/csituka/magicaland/client/render/BodyFlightAura.java'
Invoke-TestJava 'javac' (@('--release', '17', '-proc:none', '-encoding', 'UTF-8', '-cp', $testDependencies, '-d', $output) + $sources) 'compile'
Invoke-TestJava 'java' @('--enable-native-access=ALL-UNNAMED', '-ea', '-cp', ($output + [IO.Path]::PathSeparator + $testDependencies), 'BodyAuraIntegrationTest', $MinecraftJar, (Join-Path $repo 'src/main/resources/assets/magicaland/shaders/core')) 'integration'
Write-Output "Evidence: $output"
