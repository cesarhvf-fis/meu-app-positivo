# Segurança

Não publique credenciais, tokens, dumps de conta, identificadores reais de dispositivos,
chaves de assinatura ou capturas contendo informações pessoais em issues e pull requests.
Relate problemas com dados artificiais. Para informação sensível, use o canal privado
de relato de vulnerabilidade do GitHub se habilitado pelo mantenedor.

Antes de publicar, execute tools/audit_public.py, revise git diff --cached e
confirme que só os arquivos da cópia pública serão enviados. O scanner é complementar
à revisão humana, não uma garantia. .gitignore não remove arquivos já rastreados.
Se uma credencial pessoal vazar, revogue-a imediatamente no serviço correspondente;
apagar o arquivo ou tornar o repositório privado não resolve cópias já feitas.

A assinatura oficial fica fora dos fontes e não é disponibilizada em automações.
Não forneça segredos a código de contribuições ainda não revisadas.
