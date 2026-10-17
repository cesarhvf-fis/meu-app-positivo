# Dados e limites da revisão

A senha é usada para autenticar na nuvem; não é salva pelo app. A sessão lembrada
e o token TRIGGERcmd são cifrados com AES-GCM e uma chave do AndroidKeyStore.
Backups automáticos do aplicativo estão desativados. Cada instalação exige
sua própria configuração. O APK não leva os dados armazenados no celular do autor.

O aplicativo acessa serviços externos necessários ao controle: a nuvem Tuya
usada pela conta compatível e, se configurado, www.triggercmd.com. Essas empresas
recebem as informações necessárias à autenticação e aos comandos.
Não é um aplicativo offline e não há garantia de continuidade dessas APIs.

Nomes de dispositivos, identificadores, estado, presets e caches podem permanecer
no armazenamento privado local. Nem todo cache usa a cifra de SessionStore.
Não compartilhe backups, relatórios completos de depuração ou a pasta de dados do app.
Isso não protege contra um aparelho comprometido ou uma versão maliciosamente alterada.

A credencial técnica de interoperabilidade da fabricante não é uma credencial
pessoal e está excluída dos fontes públicos. Ela existe no APK funcional e pode
ser extraída, como qualquer constante compilada. Não coloque senhas pessoais no código.

A revisão de publicação procura arquivos privados e padrões conhecidos de credenciais.
Nenhuma revisão, .gitignore ou ferramenta automática prova risco zero.
Faça nova revisão a cada release; o relatório vale somente para os arquivos examinados.
