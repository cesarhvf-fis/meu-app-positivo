# Guia do Meu App Positivo v1.0

Projeto idealizado e testado por **cesarhvf-fis**, desenvolvido em colaboração
com o **Codex (OpenAI)**, que auxiliou na programação, interface e documentação.

**Esboço em construção:** as explicações abaixo descrevem a interface implementada.
Os prints ainda serão adicionados e conferidos pelo autor. Recursos disponíveis
dependem do dispositivo, das permissões da conta e da resposta da nuvem.
Este é um cliente independente e não oficial; veja o [aviso de independência](DIREITOS-E-INDEPENDENCIA.md).

## Navegação rápida

| Quero… | Onde encontrar |
| --- | --- |
| Ajustar brilho e temperatura | Aba **Branco** |
| Escolher uma cor | Aba **Cores** |
| Aplicar ou editar um preset | Aba **Efeitos** |
| Controlar outra lâmpada | Seletor no topo |
| Acessar tomada e outros dispositivos | Aba **Outros** |
| Consultar consumo por dia | Tomada → **Histórico de consumo** |
| Criar um horário para a lâmpada | Ícone de agendamento no topo |
| Compartilhar uma lâmpada | Engrenagem → **Compartilhamento** |
| Definir comportamento após retorno da energia | Engrenagem → **Estado Inicial** |
| Configurar desligamento do Windows | Engrenagem → **TRIGGERcmd** |
| Adicionar botões rápidos e widget | Engrenagem → **Complementos** |

## 1. Entrar na conta

Na primeira abertura, informe o e-mail e a senha da sua própria conta do aplicativo
Positivo. Após o login, o cliente consulta os dispositivos disponíveis nessa conta.
É necessário acesso à internet. O APK distribuído não contém a conta do autor.
A senha não é salva; uma sessão protegida permite continuar usando o app depois.

**Print pendente:** `01-login.png` — tela de entrada, com os campos vazios.

## 2. Tela principal e escolha da lâmpada

O seletor no topo escolhe a lâmpada controlada pelas abas principais e pelos
atalhos associados. O menu distingue dispositivos próprios e compartilhados,
conforme os dados da conta. O status indica a situação disponível do dispositivo.
Escolher outra lâmpada muda o alvo dos controles; não transfere sua propriedade.

**Print pendente:** `02-seletor-lampada.png` — menu aberto com nomes fictícios.

### Renomear dispositivo

A opção de renomear permite escolher um nome mais fácil de reconhecer. É possível
salvar o nome somente neste aplicativo. Dispositivos compartilhados podem ter
restrições para alterações na nuvem; um apelido local não muda o nome para o proprietário.

**Print pendente:** `03-renomear.png` — formulário com um nome de demonstração.

## 3. Aba Branco

Controla a iluminação branca da lâmpada selecionada. Ajuste o **brilho** para
variar a intensidade e a **temperatura** para escolher entre branco quente e frio.
O controle de energia permite ligar ou desligar a lâmpada.
Esses ajustes dependem dos recursos que a lâmpada disponibiliza.

**Print pendente:** `04-branco.png` — indicar os controles de brilho, temperatura e energia.

## 4. Aba Cores

Use o seletor visual para escolher a tonalidade e a saturação. O controle de
brilho ajusta a intensidade da cor. Essa aba exige uma lâmpada com suporte a cores.

**Print pendente:** `05-cores.png` — seletor visual e controle de intensidade.

## 5. Aba Efeitos

Aplica presets como **Noite, Leitura, Trabalho, Lazer, Suave, Festa, Diversão e
Multicolorido**. Toque no preset para usar sua configuração na lâmpada selecionada.
O botão de edição abre a personalização daquele efeito.

**Print pendente:** `06-efeitos.png` — grade de presets.

### Editar um efeito

É possível ajustar nome, ícone, imagem e etapas de iluminação. Cada etapa pode
usar branco ou cor, com os valores correspondentes. A transição define luz fixa,
alternância ou mudança suave; a velocidade se aplica às transições compatíveis.
Use **Adicionar etapa**, a seta para reordenar e **Remover** para montar a sequência.

**Salvar** guarda a configuração do preset neste aplicativo. **Restaurar padrão**
recupera a configuração inicial. Uma imagem escolhida pode ser reutilizada em
outros presets. Os presets locais não são apresentados como sincronizados entre celulares.

**Print pendente:** `07-editar-efeito.png` — nome, ícone, transição e uma etapa de demonstração.

## 6. Agendamentos da lâmpada

O ícone de agendamento abre a lista de horários da lâmpada selecionada. É possível
criar, editar, pausar, ativar ou excluir um horário, conforme as permissões da conta.
Os agendamentos são enviados à nuvem; os horários da interface usam Brasília.

### Criar ou editar horário

Escolha nome, horário, dias e ação: ligar, desligar, branco, cores ou efeito.
Sem dias selecionados, o horário é configurado para uma única execução.
Ao agendar um efeito, os valores atuais do preset são usados ao salvar o horário.
Editar depois o preset não deve ser confundido com atualizar automaticamente um horário já salvo.

**Print pendente:** `08-agendamentos.png` — lista de horários fictícios.

**Print pendente:** `09-editar-agendamento.png` — formulário com horário e dias.

## 7. Aba Outros

Reúne os outros dispositivos próprios e os compartilhados. Os controles exibidos
dependem do tipo e das capacidades de cada dispositivo. Dispositivos offline ou
sem permissão podem ter ações indisponíveis. A tomada com medição possui um painel próprio.

**Print pendente:** `10-outros.png` — exemplos de cartões com nomes fictícios.

## 8. Tomada e controle do PC

Uma tomada compatível pode mostrar **corrente, tensão, potência e consumo acumulado**.
Com o painel aberto e ativo, as medições são consultadas a cada segundo; isso
não significa que o próprio dispositivo ou a nuvem gere um valor novo a cada segundo.
O total acumulado tem consulta menos frequente e pode manter o último valor válido.

| Ação | O que faz |
| --- | --- |
| Ligar/desligar a tomada | Fornece ou corta a energia elétrica, após confirmação |
| Reiniciar o pc | Interrompe a energia e programa o religamento da tomada, após confirmação |
| Desligar Windows | Envia o comando configurado no TRIGGERcmd, após confirmação |

**Desligar Windows** permite ao sistema fazer o desligamento normal.
**Desligar a tomada** corta a alimentação; se o PC estiver trabalhando, pode haver
perda de trabalho não salvo. O botão de reinício também atua na energia da tomada.
Fornecer energia novamente só inicia o PC se sua configuração de hardware permitir isso.
Essas ações não são equivalentes, por isso os diálogos explicam o comando antes de confirmar.

**Print pendente:** `11-tomada.png` — medições de demonstração e botões de controle.

**Print pendente:** `12-confirmacoes-energia.png` — diálogo de confirmação, sem executar o comando.

## 9. Histórico de consumo

Na tomada, toque em **Histórico de consumo**. O seletor de mês abre um menu que
segue o visual do app. Os dias aparecem em uma lista vertical, cada um com sua
barra horizontal e o consumo correspondente em kWh.

O histórico depende dos dados que a nuvem disponibiliza para aquela tomada.
Ausência de informação não deve ser interpretada automaticamente como consumo zero.
O consumo acumulado e a potência instantânea representam grandezas diferentes.

**Print pendente:** `13-historico.png` — lista de dias e barras com valores fictícios.

**Print pendente:** `14-seletor-mes.png` — menu de meses aberto.

## 10. Configurações

A engrenagem abre **Compartilhamento**, **Estado Inicial**, **TRIGGERcmd**,
**Complementos** e a opção de sair da conta. A tela pode mostrar o e-mail da
sessão; oculte-o nos prints públicos.

**Print pendente:** `15-configuracoes.png` — menu com os dados da conta ocultos.

### Compartilhamento

Escolha uma lâmpada da qual você é proprietário, consulte seus compartilhamentos
e informe a conta da pessoa com quem deseja compartilhá-la. Também há opção de
remover o acesso. O menu depende das permissões e informações retornadas pela nuvem.
Uma lâmpada recebida de outra pessoa não passa a ser sua ao selecioná-la no app.

**Print pendente:** `16-compartilhamento.png` — sem nomes ou e-mails reais.

### Estado Inicial

Define o comportamento compatível após a lâmpada voltar a receber energia:
modo inicial, restaurar os últimos valores ou usar uma configuração personalizada.
No personalizado, escolha branco ou cores e seus ajustes.
A opção **Não perturbe** aparece conforme o suporte do dispositivo.

**Print pendente:** `17-estado-inicial.png` — seletor de comportamento e controles personalizados.

## 11. Integração TRIGGERcmd

O menu configura o computador e o nome do comando de desligamento já cadastrado
no agente TRIGGERcmd. Cada usuário utiliza seu próprio vínculo. O recurso exige
o agente no computador e conectividade com o serviço; não liga sozinho um PC desligado.

Há configuração manual com token e vínculo opcional por USB. A opção
**Validar conexão · sem desligar** verifica a presença do comando sem executá-lo.
**Remover vínculo deste celular** apaga a configuração local da integração.
O aplicativo não fornece um token compartilhado com quem baixa o APK.

### Vínculo por USB

1. No celular, toque em **Preparar vínculo USB**.
2. Conecte-o ao computador e autorize a depuração USB necessária ao auxiliar.
3. Execute o auxiliar de vínculo com o agente TRIGGERcmd já configurado.
4. No aplicativo, toque em **Importar vínculo**.
5. Use a validação sem desligamento para conferir a configuração.

O auxiliar transfere a configuração cifrada; ele não executa o desligamento nessa etapa.
Veja também as instruções de [compilação e auxiliar](COMPILAR.md).

**Print pendente:** `18-triggercmd.png` — configuração com nomes fictícios e token vazio/oculto.

**Print pendente:** `19-vinculo-usb.png` — instruções e resultado da validação sem dados pessoais.

## 12. Complementos: botões rápidos e widget

O menu **Complementos** orienta a adicionar dois botões aos ajustes rápidos do Android:

- **Lâmpada:** um toque alterna a energia da lâmpada selecionada; segurar abre o painel,
  conforme as regras do sistema.
- **Painel da luz:** abre diretamente os controles, sem alternar a energia ao tocar.

O widget **Minha luz** pode ser adicionado à tela inicial pelo seletor de widgets
do Android. Mostra o estado disponível e oferece consulta pela seta.
A instalação na tela bloqueada por Good Lock/LockStar e a abertura sem desbloqueio
dependem do aparelho e da versão da One UI; não são recursos universais do Android.

**Print pendente:** `20-complementos.png` — instruções de adição.

**Print pendente:** `21-ajustes-rapidos.png` — botões na cortina do Android, com notificações ocultas.

**Print pendente:** `22-widget.png` — widget sobre uma tela de demonstração.

## 13. Voltar, fechar e segundo plano

Use o botão ou gesto **Voltar** do Android para fechar primeiro o submenu ou diálogo
atual. O **×** fecha o diálogo visível. Na tela principal, voltar pode colocar o app
em segundo plano. A consulta por segundo é suspensa quando o painel deixa de estar ativo.
Atualizações agendadas do widget e tarefas de religamento já solicitadas são independentes.

## Como completar este guia

Os nomes acima são sugestões para organizar as futuras capturas em `docs/imagens/`.
Não há imagens anexadas nesta primeira revisão; os marcadores não são links quebrados.
Após revisar um print, substitua o marcador por uma imagem e uma legenda curta.
O arquivo [imagens/README.md](imagens/README.md) descreve o padrão sugerido.
Os bloqueios de imagens na auditoria devem ser revistos conscientemente antes de
publicar capturas: não basta adicionar arquivos de uma conta real ao repositório.

**Pendências para o autor:** inserir as capturas, conferir os textos no aparelho e
complementar exemplos de uso. Prefira dados demonstrativos em todas as imagens públicas.
