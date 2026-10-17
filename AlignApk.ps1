param([string]$InputApk,[string]$OutputApk,[switch]$CheckOnly)
$ErrorActionPreference='Stop'
# Preserve compressed entry bytes and CRCs; align each local ZIP payload to 4 bytes.
# No native libraries are packaged, so ELF/page alignment is not needed.
$zipBytes=[IO.File]::ReadAllBytes($InputApk)
$endOffset=-1
for($offset=$zipBytes.Length-22;$offset -ge [Math]::Max(0,$zipBytes.Length-65557);$offset--){if([BitConverter]::ToUInt32($zipBytes,$offset) -eq 0x06054b50){$endOffset=$offset;break}}
if($endOffset -lt 0){throw 'Arquivo ZIP incompleto.'}
$entryCount=[BitConverter]::ToUInt16($zipBytes,$endOffset+10)
$directoryOffset=[BitConverter]::ToUInt32($zipBytes,$endOffset+16)
$entries=@();$cursor=[int]$directoryOffset
for($i=0;$i -lt $entryCount;$i++){
  if([BitConverter]::ToUInt32($zipBytes,$cursor) -ne 0x02014b50){throw 'Índice ZIP inválido.'}
  $nameLength=[BitConverter]::ToUInt16($zipBytes,$cursor+28);$extraLength=[BitConverter]::ToUInt16($zipBytes,$cursor+30);$commentLength=[BitConverter]::ToUInt16($zipBytes,$cursor+32)
  $localOffset=[BitConverter]::ToUInt32($zipBytes,$cursor+42)
  $localNameLength=[BitConverter]::ToUInt16($zipBytes,$localOffset+26);$localExtraLength=[BitConverter]::ToUInt16($zipBytes,$localOffset+28)
  $dataOffset=$localOffset+30+$localNameLength+$localExtraLength
  $nameBytes=New-Object byte[] $nameLength;[Array]::Copy($zipBytes,$cursor+46,$nameBytes,0,$nameLength)
  $name=[Text.Encoding]::UTF8.GetString($nameBytes)
  $compressedLength=[BitConverter]::ToUInt32($zipBytes,$cursor+20)
  if($name -eq 'resources.arsc' -and ([BitConverter]::ToUInt16($zipBytes,$cursor+10) -ne 0 -or $dataOffset%4 -ne 0) -and $CheckOnly){throw 'resources.arsc comprimido ou desalinhado.'}
  if($CheckOnly -and [BitConverter]::ToUInt16($zipBytes,$cursor+10) -eq 0 -and $dataOffset%4 -ne 0){throw ('Entrada desalinhada: '+$name)}
  $central=New-Object byte[] (46+$nameLength+$extraLength+$commentLength);[Array]::Copy($zipBytes,$cursor,$central,0,$central.Length)
  $local=New-Object byte[] 30;[Array]::Copy($zipBytes,$localOffset,$local,0,30)
  $entries+=@{Name=$nameBytes;Central=$central;Local=$local;DataOffset=$dataOffset;Length=$compressedLength}
  $cursor+=$central.Length
}
if($CheckOnly){Write-Output 'PASS: entradas armazenadas alinhadas; resources.arsc sem compressão e alinhado.';return}
$stream=New-Object IO.MemoryStream;$writer=New-Object IO.BinaryWriter $stream
foreach($entry in $entries){
  $localOffset=[uint32]$stream.Position
  # Clear the data-descriptor flag and copy the central CRC/sizes into the local header.
  $flags=[BitConverter]::ToUInt16($entry.Central,8) -band 0xfff7
  [Array]::Copy([BitConverter]::GetBytes([uint16]$flags),0,$entry.Local,6,2)
  [Array]::Copy([BitConverter]::GetBytes([uint16]$flags),0,$entry.Central,8,2)
  [Array]::Copy($entry.Central,16,$entry.Local,14,12)
  $padding=(4-(($stream.Position+30+$entry.Name.Length+4)%4))%4
  [Array]::Copy([BitConverter]::GetBytes([uint16](4+$padding)),0,$entry.Local,28,2)
  [Array]::Copy([BitConverter]::GetBytes($localOffset),0,$entry.Central,42,4)
  $writer.Write([byte[]]$entry.Local);$writer.Write([byte[]]$entry.Name);$writer.Write([uint16]0xd935);$writer.Write([uint16]$padding)
  if($padding){$writer.Write([byte[]](New-Object byte[] $padding))}
  $writer.Write($zipBytes,[int]$entry.DataOffset,[int]$entry.Length)
}
$centralStart=[uint32]$stream.Position
foreach($entry in $entries){$writer.Write([byte[]]$entry.Central)}
$centralSize=[uint32]($stream.Position-$centralStart)
$writer.Write([uint32]0x06054b50);$writer.Write([uint16]0);$writer.Write([uint16]0);$writer.Write([uint16]$entryCount);$writer.Write([uint16]$entryCount);$writer.Write($centralSize);$writer.Write($centralStart);$writer.Write([uint16]0)
[IO.File]::WriteAllBytes($OutputApk,$stream.ToArray());$writer.Dispose();$stream.Dispose()
