param(
  [string]$AndroidJar,
  [string]$BuildTools,
  [string]$JavaHome,
  [string]$SigningDirectory,
  [string]$WorkDirectory,
  [string]$ApkOutput,
  [string]$NodeExecutable,
  [Parameter(Mandatory=$true)][string]$ProtocolConfig
)
$ErrorActionPreference = 'Stop'
$projectDirectory = $PSScriptRoot
foreach($inputPath in @($AndroidJar,$BuildTools,$JavaHome)){if(-not(Test-Path -LiteralPath $inputPath)){throw 'Informe as ferramentas Android e o JDK existentes.'}}
if(-not $SigningDirectory -or -not $WorkDirectory -or -not $ApkOutput){throw 'Informe as pastas de compilação, assinatura e o arquivo APK de saída.'}
# Never create credentials, generated code, or signed binaries inside the source tree.
$sourceRoot = [IO.Path]::GetFullPath($projectDirectory).TrimEnd('\') + '\'
foreach($externalPath in @($SigningDirectory,$WorkDirectory,$ApkOutput,$ProtocolConfig)) {
  $resolved = [IO.Path]::GetFullPath($externalPath)
  if($resolved.StartsWith($sourceRoot,[StringComparison]::OrdinalIgnoreCase) -or $resolved.TrimEnd('\') -eq $sourceRoot.TrimEnd('\')) { throw 'Assinatura, configuração privada, trabalho e APK precisam ficar fora da pasta dos fontes.' }
}
$protocol = Get-Content -LiteralPath $ProtocolConfig -Raw | ConvertFrom-Json
foreach($value in @($protocol.clientId,$protocol.appSecret)) {
  if(-not $value -or $value -notmatch '^[A-Za-z0-9_-]+$' -or $value -match '^PREENCHER') { throw 'Preencha a configuração local do protocolo. Nunca use sua senha ou token de conta nesses campos.' }
}
New-Item -ItemType Directory -Path $SigningDirectory,$WorkDirectory -Force | Out-Null
$buildDirectory = Join-Path $WorkDirectory ('build-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $buildDirectory,(Join-Path $buildDirectory 'classes'),(Join-Path $buildDirectory 'dex'),(Join-Path $buildDirectory 'gen'),(Join-Path $buildDirectory 'flat'),(Join-Path $buildDirectory 'package') | Out-Null
$protocolSource = Join-Path $buildDirectory 'gen/LocalProtocolConfig.java'
$protocolJava = 'package br.com.casacesar.luz; final class LocalProtocolConfig { static final String CLIENT_ID = "' + $protocol.clientId + '"; static final String APP_SECRET = "' + $protocol.appSecret + '"; }'
[IO.File]::WriteAllText($protocolSource,$protocolJava,[Text.UTF8Encoding]::new($false))
$protocolJava=$null; $protocol=$null
function Invoke-BuildTool([string]$Executable,[string[]]$Arguments){ & $Executable @Arguments; if($LASTEXITCODE -ne 0){throw ('Falha na ferramenta: '+[IO.Path]::GetFileName($Executable))}}
if(-not $NodeExecutable){$NodeExecutable=(Get-Command node -ErrorAction Stop).Source}
foreach($script in Get-ChildItem -LiteralPath (Join-Path $projectDirectory 'assets') -Filter '*.js'){Invoke-BuildTool -Executable $NodeExecutable -Arguments @('--check',$script.FullName)}
$aapt2 = Join-Path $BuildTools 'aapt2.exe'
foreach($resource in Get-ChildItem -LiteralPath (Join-Path $projectDirectory 'res') -File -Recurse){Invoke-BuildTool -Executable $aapt2 -Arguments @('compile','--no-crunch',$resource.FullName,'-o',(Join-Path $buildDirectory 'flat'))}
$baseApk = Join-Path $buildDirectory 'base.apk'
$packageDirectory = Join-Path $buildDirectory 'package'
foreach($resourceDirectory in Get-ChildItem -LiteralPath (Join-Path $projectDirectory 'res') -Directory -Recurse){$relativeDirectory=$resourceDirectory.FullName.Substring($projectDirectory.Length+1);New-Item -ItemType Directory -Path (Join-Path $packageDirectory $relativeDirectory) -Force | Out-Null}
New-Item -ItemType Directory -Path (Join-Path $packageDirectory 'assets') -Force | Out-Null
$linkArguments = @('link','--output-to-dir','-o',$packageDirectory,'-I',$AndroidJar,'--manifest',(Join-Path $projectDirectory 'AndroidManifest.xml'),'-A',(Join-Path $projectDirectory 'assets'),'--java',(Join-Path $buildDirectory 'gen')) + @(Get-ChildItem -LiteralPath (Join-Path $buildDirectory 'flat') -File | ForEach-Object {$_.FullName})
Invoke-BuildTool -Executable $aapt2 -Arguments $linkArguments
$javac = Join-Path $JavaHome 'bin/javac.exe'
$sourcePaths = @(Get-ChildItem -LiteralPath (Join-Path $projectDirectory 'src'),(Join-Path $buildDirectory 'gen') -Filter '*.java' -Recurse | ForEach-Object {$_.FullName})
$javaArguments = @('--release','8','-encoding','UTF-8','-classpath',$AndroidJar,'-d',(Join-Path $buildDirectory 'classes')) + $sourcePaths
Invoke-BuildTool -Executable $javac -Arguments $javaArguments
$classesJar = Join-Path $buildDirectory 'classes.jar'
Add-Type -AssemblyName System.IO.Compression.FileSystem
[IO.Compression.ZipFile]::CreateFromDirectory((Join-Path $buildDirectory 'classes'),$classesJar,[IO.Compression.CompressionLevel]::Optimal,$false)
Invoke-BuildTool -Executable (Join-Path $JavaHome 'bin/java.exe') -Arguments @(('-Djava.io.tmpdir='+$buildDirectory),'-cp',(Join-Path $BuildTools 'lib/d8.jar'),'com.android.tools.r8.D8','--lib',$AndroidJar,'--min-api','26','--output',(Join-Path $buildDirectory 'dex'),$classesJar)
$archive = [IO.Compression.ZipFile]::Open($baseApk,[IO.Compression.ZipArchiveMode]::Create)
try {
  Get-ChildItem -LiteralPath $packageDirectory -Recurse -File | ForEach-Object { $entryName=$_.FullName.Substring($packageDirectory.Length+1).Replace('\','/'); $compression=if($entryName -eq 'resources.arsc'){[IO.Compression.CompressionLevel]::NoCompression}else{[IO.Compression.CompressionLevel]::Optimal}; [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,$_.FullName,$entryName,$compression) | Out-Null }
  Get-ChildItem -LiteralPath (Join-Path $buildDirectory 'dex') -Filter '*.dex' | ForEach-Object {[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,$_.FullName,$_.Name,[IO.Compression.CompressionLevel]::Optimal) | Out-Null}
} finally {$archive.Dispose()}
$alignedApk = Join-Path $buildDirectory 'aligned.apk'
& (Join-Path $projectDirectory 'AlignApk.ps1') -InputApk $baseApk -OutputApk $alignedApk
$keyFile = Join-Path $SigningDirectory 'casa-lampada.jks'; $passwordFile = Join-Path $SigningDirectory 'casa-lampada.password'
if(-not(Test-Path -LiteralPath $keyFile)){
  if(Test-Path -LiteralPath $passwordFile){throw 'Arquivo de senha existente sem chave; preserve e confira sua pasta de assinatura.'}
  $random = New-Object byte[] 32; $generator = [Security.Cryptography.RandomNumberGenerator]::Create(); $generator.GetBytes($random); $generator.Dispose()
  $keyPassword = [Convert]::ToBase64String($random); [IO.File]::WriteAllText($passwordFile,$keyPassword,[Text.UTF8Encoding]::new($false)); [Array]::Clear($random,0,$random.Length)
  Invoke-BuildTool -Executable (Join-Path $JavaHome 'bin/keytool.exe') -Arguments @('-genkeypair','-keystore',$keyFile,'-alias','casa','-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=Casa Lampada Pessoal','-storepass:file',$passwordFile,'-keypass:file',$passwordFile)
  $keyPassword = $null
}
$signer = Join-Path $BuildTools 'lib/apksigner.jar'
Invoke-BuildTool -Executable (Join-Path $JavaHome 'bin/java.exe') -Arguments @(('-Djava.io.tmpdir='+$buildDirectory),'-jar',$signer,'sign','--ks',$keyFile,'--ks-key-alias','casa','--ks-pass',('file:'+$passwordFile),'--out',$ApkOutput,$alignedApk)
Invoke-BuildTool -Executable (Join-Path $JavaHome 'bin/java.exe') -Arguments @('-jar',$signer,'verify','--verbose','--print-certs',$ApkOutput)
& (Join-Path $projectDirectory 'AlignApk.ps1') -InputApk $ApkOutput -CheckOnly
Write-Output ('APK gerado e verificado: '+$ApkOutput)
