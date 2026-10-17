# Organização

| Arquivo/pasta | Responsabilidade |
| --- | --- |
| assets/app.js, features.js | Interface, presets, tomada, histórico, confirmações e navegação |
| assets/style.css, panel-glass.css | Aparência do app e painel |
| assets/defaults.json | Presets iniciais genéricos |
| MainActivity, PanelActivity | WebView local e ponte assíncrona para Java |
| CasaApp | Coordenação e executor serial de operações |
| CloudApi, MobileCrypto | Transporte HTTPS e assinatura do protocolo |
| CloudBackend, DeviceExtras | Dispositivos, comandos e histórico de consumo |
| SessionStore | Sessão e configuração TRIGGERcmd cifradas via AndroidKeyStore |
| TriggerCmd, TriggerCmdProtocol | Vínculo, validação e comando de desligamento |
| CycleFlow, PowerCycle, PowerCycleJob | Reinício da tomada e recuperação agendada |
| LampWidgetProvider, WidgetRefreshJob | Widget e atualização periódica |
| LampTileService, PanelTileService | Ajustes rápidos do Android |
| res/ | Ícones vetoriais, temas e layouts nativos |
| tests/ | Testes JVM com dados artificiais |

A interface conversa com a camada nativa por rotas limitadas. O conteúdo web
é servido localmente em https://app.local; não é um site público independente.
As chamadas à nuvem usam HTTPS e não seguem redirecionamentos automaticamente.
O identificador br.com.casacesar.luz é público e preservado para compatibilidade
de atualização; não é uma credencial de conta.

A consulta por segundo depende de painel ativo. O consumo acumulado mantém
o último valor válido para evitar oscilação quando a nuvem omite a informação.
Login e comandos devem ser testados manualmente pelo dono de cada conta.
Não automatize testes reais de corte de energia ou desligamento em contribuições.
