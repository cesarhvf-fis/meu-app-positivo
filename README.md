# Meu App Positivo v1.0 — cliente Android independente

Projeto idealizado e testado por [cesarhvf-fis](https://github.com/cesarhvf-fis),
desenvolvido em colaboração com o **Codex (OpenAI)**, que auxiliou na programação,
interface e documentação. As funcionalidades e os ajustes foram definidos e
validados ao longo dessa colaboração.

**Projeto comunitário independente e não oficial. Não é desenvolvido, mantido,
patrocinado ou endossado pela Positivo, Tuya ou TRIGGERcmd.**

Este aplicativo oferece uma interface alternativa e simplificada para recursos
de dispositivos compatíveis. Não é uma versão oficial nem uma modificação do
APK oficial da fabricante. Os nomes dos produtos e serviços são mencionados
para identificar compatibilidade; suas marcas pertencem aos respectivos titulares.
Veja o [aviso de independência, licenças e direitos](docs/DIREITOS-E-INDEPENDENCIA.md).

Controle de lâmpadas e tomadas compatíveis com a conta Positivo Casa Inteligente,
com interface em português, widget, ajustes rápidos e integração opcional ao TRIGGERcmd.

Veja o [guia ilustrado do aplicativo — esboço](docs/GUIA-DO-APLICATIVO.md),
com explicações das abas e submenus e espaços reservados para os prints.

## Instalar

O APK v1.0 assinado fica em **Releases**, separado dos fontes. Android 8 ou posterior.
Cada pessoa entra com a própria conta. Não há conta de demonstração, senha,
sessão ou configuração TRIGGERcmd do autor incorporadas no aplicativo.
Compatibilidade foi verificada em um conjunto limitado de dispositivos; não é universal.

## Recursos

- Lâmpadas: liga/desliga, brilho, temperatura, cores, efeitos e presets.
- Tomadas: corrente, tensão, potência, consumo acumulado e histórico mensal.
- Comandos de energia com confirmação e reinício por interrupção temporária da tomada.
- Desligamento normal do Windows pelo TRIGGERcmd, configurado por cada usuário.
- Widget e botões dos ajustes rápidos; botão Voltar fecha primeiro os submenus.

A consulta de medições ocorre a cada segundo com o painel aberto e ativo.
O acumulado usa cache e consulta menos frequente. Valores dependem do dispositivo
e da nuvem. Fechar o painel suspende a consulta por segundo; o widget pode
receber atualizações periódicas agendadas pelo Android.

## Desenvolver

Leia [COMPILAR.md](docs/COMPILAR.md), [ARQUITETURA.md](docs/ARQUITETURA.md)
e [CONTRIBUTING.md](CONTRIBUTING.md). Interface em HTML/CSS/JS; integração Android
em Java. O projeto usa um script PowerShell, sem Gradle ou dependências Android externas.

**Configuração do protocolo:** os fontes públicos não incluem a identificação e
o segredo técnico do aplicativo da fabricante. Não são senha ou token pessoal.
A compilação exige um arquivo local externo com valores compatíveis, obtidos por
meios autorizados. O exemplo contém somente placeholders. Sem essa configuração
é possível estudar o código e executar testes simulados, mas não autenticar na nuvem.
O APK funcional contém os valores técnicos compilados: eles são extraíveis e
não podem ser tratados como um cofre de segredos. Contas pessoais continuam separadas.

## Privacidade e licença

Veja [PRIVACIDADE.md](docs/PRIVACIDADE.md) e [SECURITY.md](SECURITY.md).
Código sob licença MIT; atribuições em [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
Essa licença abrange o código disponibilizado por seus respectivos titulares;
não concede direitos sobre marcas, serviços, credenciais técnicas ou materiais
proprietários de terceiros. O acesso às plataformas continua sujeito às condições
dos respectivos provedores.
Não envie senhas, tokens, backups ou capturas com informações pessoais em issues.
