# Conversor de Gênios

Projeto independente para Android: importar HTML ou ZIP, abrir a prévia e preparar um APK Android que abre o projeto web empacotado.

## Estado atual

- O aplicativo Android importa arquivos `.html`, `.htm` e `.zip` pelo seletor de arquivos do próprio celular.
- A prévia abre no WebView do aplicativo.
- O app consegue exportar o projeto importado como `input-project.zip`.
- O workflow `Build APK from HTML ZIP` compila o ZIP que estiver na raiz do repositório e publica o APK como artefato do GitHub Actions.
- O workflow não usa mais uma demonstração automaticamente: em execução manual, a opção de demo começa desativada. A demonstração precisa ser habilitada de propósito.

## Gerar APK de um projeto real

1. No Conversor de Gênios, importe o HTML ou ZIP completo.
2. Toque em **Preparar projeto para gerar APK** e salve com o nome `input-project.zip`.
3. Abra a página Code do repositório GitHub e envie esse arquivo à raiz do repositório, confirmando o commit.
4. O envio de `input-project.zip` inicia automaticamente o workflow **Build APK from HTML ZIP**.
5. Quando a execução terminar, abra a execução em Actions e baixe o artefato **APK-do-projeto-HTML**.

Workflow: https://github.com/regggui/Conversor-de-G-nios-/actions/workflows/html-to-apk.yml

## Limitação de integração que ainda existe

O app ainda **não envia automaticamente** o ZIP para o GitHub e não baixa o APK final por conta própria. Hoje ele prepara o ZIP e abre a página do workflow; o envio e o download final ainda são feitos no GitHub pelo usuário.

Para automatizar isso com segurança, é necessário configurar uma autorização OAuth/App para a conta GitHub ou um serviço intermediário protegido. Não coloque um token pessoal do GitHub dentro do APK nem dentro dos arquivos HTML: qualquer pessoa que extraia o app poderia recuperar o segredo. Essa autorização não pode ser criada apenas alterando o código do repositório; ela precisa ser registrada/configurada na conta GitHub (e, se usado, no serviço de backend).

## Formato e limitações do projeto

- O ZIP deve conter `index.html` e os recursos necessários (CSS, JavaScript, imagens etc.). Se o index estiver numa subpasta, o fluxo tenta mover o conteúdo dessa pasta para a raiz do site empacotado.
- O APK abre o site local via WebView. Conteúdo que dependa de servidores externos, APIs ou funcionalidades nativas Android não será automaticamente convertido em funcionalidade nativa.
- O resultado atual é um APK de depuração para testes, não uma versão assinada para publicação na Play Store.
- Não coloque senhas, chaves privadas ou tokens no HTML/ZIP.
