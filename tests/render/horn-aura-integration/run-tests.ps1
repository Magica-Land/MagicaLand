param(
    [Parameter(Mandatory)][string]$ClasspathFile,
    [Parameter(Mandatory)][string]$MinecraftJar,
    [Parameter(Mandatory)][string]$WidenedMinecraftJar,
    [Parameter(Mandatory)][string]$AppearanceClasses,
    [string]$JavaBin = '',
    [string]$OutputDirectory = (Join-Path ([IO.Path]::GetTempPath()) ('magicaland-horn-blend-' + [guid]::NewGuid().ToString('N')))
)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$hornRepo = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
if (Test-Path -LiteralPath $OutputDirectory) { throw 'Use a fresh output directory.' }
$hornOutput = (New-Item -ItemType Directory -Path $OutputDirectory).FullName
$hornDependencies = $WidenedMinecraftJar + [IO.Path]::PathSeparator + $AppearanceClasses + [IO.Path]::PathSeparator + (Get-Content -Raw -LiteralPath $ClasspathFile).Trim()
function Invoke-HornJava([string]$Command, [string[]]$Arguments, [string]$Name) {
    $hornArgFile = Join-Path $hornOutput ($Name + '.args')
    [IO.File]::WriteAllLines($hornArgFile, @($Arguments | ForEach-Object { '"' + $_.Replace('\', '/').Replace('"', '\"') + '"' }), [Text.UTF8Encoding]::new($false))
    $hornExecutable = if ($JavaBin) { Join-Path $JavaBin $Command } else { $Command }
    & $hornExecutable ('@' + $hornArgFile) 2>&1 | Tee-Object (Join-Path $hornOutput ($Name + '.log'))
    if ($LASTEXITCODE -ne 0) { throw "$Name failed" }
}
$hornSources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'stubs') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName)
$hornSources += Join-Path $PSScriptRoot 'HornAuraBlendIntegrationTest.java'
$hornSources += Join-Path $hornRepo 'src/client/java/top/csituka/magicaland/client/render/MagicGlow.java'
Invoke-HornJava 'javac' (@('--release', '17', '-proc:none', '-encoding', 'UTF-8', '-cp', $hornDependencies, '-d', $hornOutput) + $hornSources) 'compile'
Invoke-HornJava 'java' @('--enable-native-access=ALL-UNNAMED', '-ea', '-cp', ($hornOutput + [IO.Path]::PathSeparator + $hornDependencies),
    'HornAuraBlendIntegrationTest', $MinecraftJar, (Join-Path $hornRepo 'src/main/resources/assets/magicaland/shaders/core')) 'integration'
Write-Output "Evidence: $hornOutput"
