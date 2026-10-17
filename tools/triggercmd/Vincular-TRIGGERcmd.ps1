param([string]$AdbPath,[string]$Serial,[string]$ConfigDirectory=(Join-Path $env:USERPROFILE '.TRIGGERcmdData'))
$ErrorActionPreference='Stop'
if(-not $AdbPath){$AdbPath=(Get-Command adb -ErrorAction Stop).Source}
if(-not(Test-Path -LiteralPath $AdbPath)){throw 'ADB não encontrado.'}
function Invoke-PairAdb([string[]]$Arguments){$result=& $AdbPath @Arguments 2>&1;if($LASTEXITCODE -ne 0){throw 'Não foi possível acessar o celular. Confira o cabo e a autorização USB.'};return $result}
if(-not $Serial){$taskDevices=@(Invoke-PairAdb @('devices') | Where-Object {$_ -match '^\S+\s+device$'});if($taskDevices.Count -ne 1){throw 'Conecte apenas o celular desejado e autorize a depuração USB.'};$Serial=($taskDevices[0] -split '\s+')[0]}
$taskRemote='/sdcard/Android/data/br.com.casacesar.luz/files/'
$taskPublicText=(Invoke-PairAdb @('-s',$Serial,'shell','cat',($taskRemote+'triggercmd-public-key.json'))) -join "`n"
try{$taskPublic=$taskPublicText|ConvertFrom-Json}catch{throw 'No app, abra Configurações > TRIGGERcmd e toque em Preparar vínculo USB.'}
$taskCommands=@(Get-Content -LiteralPath (Join-Path $ConfigDirectory 'commands.json') -Raw|ConvertFrom-Json)
$taskShutdown=@($taskCommands|Where-Object {$_.command -match '^\s*shutdown(?:\.exe)?\s+/s(?:\s|$)'})
if($taskShutdown.Count -ne 1){throw 'Deixe um único comando shutdown /s cadastrado no TRIGGERcmd ou configure os dados manualmente no app.'}
$taskComputer=[IO.File]::ReadAllText((Join-Path $ConfigDirectory 'computername.cfg')).Trim()
$taskToken=[IO.File]::ReadAllText((Join-Path $ConfigDirectory 'token.tkn')).Trim()
$taskConfig=@{computer=$taskComputer;trigger=[string]$taskShutdown[0].trigger;token=$taskToken}|ConvertTo-Json -Compress
$taskBytes=[Text.Encoding]::UTF8.GetBytes($taskConfig)
$taskRsa=New-Object Security.Cryptography.RSACryptoServiceProvider(3072)
$taskTemp=[IO.Path]::GetTempFileName()
try{
 $taskModulus=[Convert]::FromBase64String([string]$taskPublic.modulus)
 if($taskModulus.Length -eq 385 -and $taskModulus[0] -eq 0){$taskModulus=[byte[]]$taskModulus[1..384]}
 if($taskModulus.Length -ne 384){throw 'Chave de vínculo inválida. Prepare novamente no celular.'}
 $taskParameters=New-Object Security.Cryptography.RSAParameters
 $taskParameters.Modulus=$taskModulus;$taskParameters.Exponent=[Convert]::FromBase64String([string]$taskPublic.exponent)
 $taskRsa.ImportParameters($taskParameters)
 # RSA-OAEP SHA-1 matches AndroidKeyStore. Only ciphertext is written or pushed.
 [IO.File]::WriteAllBytes($taskTemp,$taskRsa.Encrypt($taskBytes,$true))
 Invoke-PairAdb @('-s',$Serial,'push',$taskTemp,($taskRemote+'triggercmd-pair.enc'))|Out-Null
 Write-Output 'Vínculo criptografado enviado. No celular, toque em Importar vínculo. Nenhum comando foi executado.'
}finally{
 [Array]::Clear($taskBytes,0,$taskBytes.Length);$taskToken=$null;$taskConfig=$null;$taskRsa.PersistKeyInCsp=$false;$taskRsa.Dispose()
 if(Test-Path -LiteralPath $taskTemp){Remove-Item -LiteralPath $taskTemp -Force}
}
