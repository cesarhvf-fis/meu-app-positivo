# Contribuir

Crie uma cópia do repositório na sua conta (fork), abra uma branch, faça a mudança
e envie um pull request explicando o comportamento e os testes realizados.
Use dados artificiais. Não anexe logs de autenticação ou configurações locais.
Interface: assets/. Integrações: src/. Ícones e layouts: res/.
Leia docs/COMPILAR.md para ferramentas, configuração privada e assinatura própria.

Execute a auditoria e os testes simulados. Mudanças na interface devem incluir
uma conferência visual com nomes e medições fictícios. Valide Voltar, submenus,
suspensão da consulta em segundo plano e confirmações dos comandos de energia.
Nunca execute desligamentos ou cortes reais nos testes automatizados.

Ao publicar nova versão, incremente versionCode e versionName no manifest,
atualize CHANGELOG.md, gere o APK e confira assinatura e SHA-256.
Código é versionado no repositório; o APK fica anexado à Release correspondente.
