param([Parameter(Mandatory=$true)][string]$JavaHome,[Parameter(Mandatory=$true)][string]$JsonJar)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$taskTemp=Join-Path ([IO.Path]::GetTempPath()) ('positivo-tests-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $taskTemp | Out-Null
try {
  foreach($file in Get-ChildItem (Join-Path $root 'assets') -Filter '*.js') { & node --check $file.FullName; if($LASTEXITCODE -ne 0){throw 'Sintaxe JS inválida'} }
  $package=Join-Path $root 'src/br/com/casacesar/luz'
  [IO.File]::WriteAllText((Join-Path $taskTemp 'LocalProtocolConfig.java'),'package br.com.casacesar.luz; final class LocalProtocolConfig { static final String CLIENT_ID="test-only"; static final String APP_SECRET="test-only"; }')
  $files=@('CloudApi','CloudBackend','MobileCrypto','PresetStore','DeviceExtras','TriggerCmdProtocol') | ForEach-Object {Join-Path $package ($_+'.java')}
  & (Join-Path $JavaHome 'bin/javac.exe') --release 8 -encoding UTF-8 -cp $JsonJar -d $taskTemp @files (Join-Path $taskTemp 'LocalProtocolConfig.java') (Join-Path $PSScriptRoot 'SocketPowerChecks.java') (Join-Path $PSScriptRoot 'TriggerCmdChecks.java')
  if($LASTEXITCODE -ne 0){throw 'Falha na compilação dos testes'}
  foreach($test in @('SocketPowerChecks','TriggerCmdChecks')) {
    & (Join-Path $JavaHome 'bin/java.exe') -cp ($taskTemp+';'+$JsonJar) ('br.com.casacesar.luz.'+$test) (Join-Path $root 'assets/defaults.json') $taskTemp
    if($LASTEXITCODE -ne 0){throw 'Teste falhou'}
  }
} finally {
  # Delete only the freshly created temporary test directory, after containment checks.
  $resolved=[IO.Path]::GetFullPath($taskTemp)
  $tempRoot=[IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\')+'\'
  if($resolved.StartsWith($tempRoot,[StringComparison]::OrdinalIgnoreCase) -and [IO.Path]::GetFileName($resolved).StartsWith('positivo-tests-')) { Remove-Item -LiteralPath $resolved -Recurse -Force }
}
