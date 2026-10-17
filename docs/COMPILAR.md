# Compilar no Windows

Requisitos: PowerShell 5.1+, JDK 21, Node.js, Android SDK Platform 36 e Build Tools
compatíveis com API 36 contendo aapt2.exe, lib/d8.jar e lib/apksigner.jar.
Obtenha as ferramentas por seus distribuidores oficiais; não estão incluídas aqui.

Mantenha fontes, segredos, trabalho e saídas em pastas separadas:

```text
projetos/meu-app-positivo/       fontes deste repositório
privado/positivo/               protocol.local.json e assinatura/
compilacoes/positivo/           arquivos intermediários
releases/                      APKs finais
```

Copie config/protocol.example.json para a pasta privada e preencha a configuração
técnica do protocolo. NÃO use senha, e-mail ou token de usuário nesses campos.
Não salve a configuração real dentro dos fontes, nem mesmo como arquivo ignorado.

Exemplo (ajuste os caminhos para suas ferramentas):

```powershell
.\Build.ps1 `
  -AndroidJar 'D:\Android\Sdk\platforms\android-36\android.jar' `
  -BuildTools 'D:\Android\Sdk\build-tools\36.0.0' `
  -JavaHome 'D:\Ferramentas\jdk-21' `
  -ProtocolConfig 'D:\privado\positivo\protocol.local.json' `
  -SigningDirectory 'D:\privado\positivo\assinatura' `
  -WorkDirectory 'D:\compilacoes\positivo' `
  -ApkOutput 'D:\releases\Meu-App-Positivo-v1.0.apk'
```

Se a pasta de assinatura estiver vazia, o script gera sua própria chave e senha.
Nunca as coloque no GitHub. Use a mesma chave em futuras versões suas para
permitir atualização. Uma chave diferente exige desinstalar a versão anterior,
perdendo seus dados locais. Apenas o mantenedor possui a chave da distribuição oficial.
A senha é passada às ferramentas por arquivo, sem aparecer na linha de comando.
Guarde uma cópia protegida da pasta de assinatura fora do computador principal.

A configuração gera LocalProtocolConfig.java somente na pasta externa de trabalho.
Essa pasta contém o segredo técnico compilado; também não deve ser compartilhada.
Não há promessa de APK bit a bit reproduzível: assinatura, ZIP e ferramentas variam.

## Testes sem conta e sem dispositivos

```powershell
python tools/audit_public.py .
    python -B -m unittest discover -s tests -p "test_*.py"
.\tests\Run-Tests.ps1 -JavaHome 'D:\Ferramentas\jdk-21' -JsonJar 'D:\bibliotecas\json.jar'
```

Use uma versão JVM da biblioteca org.json (por exemplo org.json:json:20240303),
obtida separadamente. Os testes usam transportes simulados e não desligam nada.
O script também verifica a sintaxe JavaScript com Node.js disponível no PATH.

## Auxiliar TRIGGERcmd

tools/triggercmd contém o código do vínculo USB opcional. Execute o .ps1 com
ADB no PATH e o celular autorizado, depois de preparar o vínculo no app.
Ele lê o token existente do agente Windows e envia somente conteúdo cifrado ao celular.
Nunca envie a pasta de configuração do agente ou arquivos de vínculo ao repositório.
O lançador PairLauncher.cs pode ser compilado com csc /target:winexe e referências
System.Windows.Forms.dll; coloque o executável ao lado do script.
