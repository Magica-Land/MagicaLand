param(
 [string]$JavaBin='',
 [string]$OutputRoot=(Join-Path ([IO.Path]::GetTempPath()) 'magicaland-hoof-cadence'),
 [string]$Node='node'
)
$ErrorActionPreference='Stop'
$hoofRepo=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$hoofRun=Join-Path $OutputRoot ('run-'+[guid]::NewGuid().ToString('N'))
$hoofClasses=Join-Path $hoofRun 'classes'
New-Item -ItemType Directory -Path $hoofClasses -Force | Out-Null
$hoofJavac=if($JavaBin){Join-Path $JavaBin 'javac.exe'}else{'javac'}
$hoofJava=if($JavaBin){Join-Path $JavaBin 'java.exe'}else{'java'}
& $hoofJavac --release 17 -proc:none -encoding UTF-8 -d $hoofClasses (Join-Path $hoofRepo 'src/client/java/top/csituka/magicaland/client/sound/PonyHoofCadence.java') (Join-Path $PSScriptRoot 'PonyHoofCadenceTest.java')
if($LASTEXITCODE -ne 0){throw 'Cadence pure Java compile failed'}
& $hoofJava -ea -Xmx128M -cp $hoofClasses top.csituka.magicaland.client.sound.PonyHoofCadenceTest 2>&1 | Tee-Object -FilePath (Join-Path $hoofRun 'tests.log')
if($LASTEXITCODE -ne 0){throw 'Cadence regression failed'}
& $Node (Join-Path $PSScriptRoot 'PonyHoofAssetTest.mjs') 2>&1 | Tee-Object -FilePath (Join-Path $hoofRun 'assets.log')
if($LASTEXITCODE -ne 0){throw 'Cadence asset regression failed'}
Write-Output "Output: $hoofRun"
