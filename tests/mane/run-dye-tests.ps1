param([Parameter(Mandatory = $true)][string]$GsonJar)
$ErrorActionPreference = 'Stop'
$testRepo = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '../..')).Path
$testGson = (Resolve-Path -LiteralPath $GsonJar).Path
$testOutput = Join-Path ([IO.Path]::GetTempPath()) ('magicaland-dye-test-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testOutput | Out-Null
$testPrefix = Join-Path $testRepo 'src/client/java/top/csituka/magicaland/client'
$testSources = @(
    'config/ModelConfig.java', 'config/style/PonyStyleRegistry.java',
    'config/style/PonyStylePart.java', 'config/style/PonyStyleDefinition.java',
    'render/BodyColorRamp.java', 'render/BodyPalette.java', 'render/ManePalette.java',
    'render/ManeDye.java', 'render/ManeDyeMask.java'
) | ForEach-Object { Join-Path $testPrefix $_ }
& javac --release 17 -proc:none -encoding UTF-8 -cp $testGson -d $testOutput @testSources (Join-Path $PSScriptRoot 'ManeDyeTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Java test compilation failed' }
& java -cp ($testOutput + [IO.Path]::PathSeparator + $testGson) ManeDyeTest (Join-Path $testRepo 'src/main/resources/assets/magicaland/mane_dyes/stripe01.json')
if ($LASTEXITCODE -ne 0) { throw 'Mane dye tests failed' }
Write-Output ('Test classes: ' + $testOutput)
