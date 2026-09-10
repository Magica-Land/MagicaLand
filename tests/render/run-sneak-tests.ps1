param(
    [Parameter(Mandatory=$true)][string]$DependencyClasspathFile,
    [Parameter(Mandatory=$true)][string]$OutputDirectory,
    [string]$AppearanceClasspath = '',
    [string]$JdkBin = 'C:/Program Files/Microsoft/jdk-25.0.4.7-hotspot/bin'
)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$sneakRepo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$sneakOutput = [IO.Path]::GetFullPath($OutputDirectory)
if ($sneakOutput -eq $sneakRepo -or $sneakOutput.StartsWith($sneakRepo + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Use an isolated output directory outside the repository.' }
$sneakRun = Join-Path $sneakOutput ('run-' + [guid]::NewGuid().ToString('N'))
$sneakClasses = Join-Path $sneakRun 'classes'
New-Item -ItemType Directory -Path $sneakClasses -Force | Out-Null
$sneakDependencies = (Get-Content -Raw -LiteralPath $DependencyClasspathFile).Trim()
$sneakSource = Join-Path $sneakRepo 'src/client/java/top/csituka/magicaland/client/animation/PonySneakController.java'
$sneakTest = Join-Path $PSScriptRoot 'PonySneakControllerTest.java'
$sneakAnimatable = Join-Path $sneakRepo 'src/client/java/top/csituka/magicaland/client/model/GeckoPlayerAnimatable.java'
$sneakSources = @($sneakSource,$sneakTest)
if ($AppearanceClasspath) {
    $sneakDependencies = $AppearanceClasspath + [IO.Path]::PathSeparator + $sneakDependencies
    $sneakSources += $sneakAnimatable
}
$sneakUtf8 = [Text.UTF8Encoding]::new($false)
function Invoke-SneakJava([string]$Tool, [string]$Name, [string[]]$Arguments) {
    $sneakArgs = Join-Path $sneakRun ($Name + '.args')
    [IO.File]::WriteAllLines($sneakArgs, @($Arguments | ForEach-Object { '"' + $_.Replace('\','/').Replace('"','\"') + '"' }), $sneakUtf8)
    $sneakLines = @(& (Join-Path $JdkBin ($Tool + '.exe')) ('@' + $sneakArgs) 2>&1 | ForEach-Object { $_.ToString() })
    $sneakCode = $LASTEXITCODE
    [IO.File]::WriteAllLines((Join-Path $sneakRun ($Name + '.log')), $sneakLines, $sneakUtf8)
    $sneakLines | Write-Output
    if ($sneakCode -ne 0) { throw "$Name failed; see $sneakRun" }
}
Invoke-SneakJava 'javac' 'typecheck' (@('--release','17','-proc:none','-implicit:none','-encoding','UTF-8','-cp',$sneakDependencies,'-d',$sneakClasses) + $sneakSources)
Invoke-SneakJava 'java' 'PonySneakControllerTest' @('-ea','-Xmx256M','-Djava.awt.headless=true','-Dfile.encoding=UTF-8','-cp',($sneakClasses + [IO.Path]::PathSeparator + $sneakDependencies),'top.csituka.magicaland.client.animation.PonySneakControllerTest',$sneakRepo)
$sneakHashes = @($sneakSource,$sneakTest,$sneakAnimatable | ForEach-Object { Get-FileHash -LiteralPath $_ -Algorithm SHA256 })
[IO.File]::WriteAllText((Join-Path $sneakRun 'result.json'), ([ordered]@{Passed=$true;RealGeckoController=$true;RealAnimatableTypecheck=[bool]$AppearanceClasspath;Sources=$sneakHashes;Gradle=$false;GameLoop=$false} | ConvertTo-Json -Depth 5), $sneakUtf8)
Write-Output "Saved sneak animation verification: $sneakRun"
