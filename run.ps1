$ErrorActionPreference = 'Stop'
$source = Get-ChildItem (Join-Path $PSScriptRoot 'src\main\java') -Filter '*.java' -Recurse | ForEach-Object { $_.FullName }
$classes = Join-Path $PSScriptRoot 'build\classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
javac --add-modules jdk.httpserver -encoding UTF-8 -d $classes $source
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$staticAssets = Join-Path $PSScriptRoot 'src\main\resources\static'
if (Test-Path $staticAssets) { Copy-Item -Path $staticAssets -Destination $classes -Recurse -Force }
java --add-modules jdk.httpserver -cp $classes classroom.app.ClassroomRentalApp
