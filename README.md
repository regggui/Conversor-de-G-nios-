# Conversor de Gênios

Projeto independente para Android: importar HTML ou ZIP, abrir a prévia e preparar um APK Android que abre o projeto web empacotado.

## Fluxos de compilação

### 1. APK do próprio Conversor
O fluxo **Build Android APK** compila o aplicativo Conversor de Gênios.

### 2. APK a partir de um projeto HTML/ZIP
O fluxo **Build APK from HTML ZIP** empacota um projeto web em um APK Android usando WebView.

**Formato de entrada:** um arquivo chamado `input-project.zip` na raiz do repositório, com um `index.html` e os recursos do projeto (CSS, JavaScript, imagens etc.). Se o index estiver numa subpasta, o fluxo tenta colocar essa pasta na raiz do projeto empacotado.

**Como usar no GitHub pelo celular:**
1. Abra a página Code do repositório.
2. Use o menu de adicionar arquivo para enviar `input-project.zip` à raiz do repositório e faça commit.
3. Abra **Actions** → **Build APK from HTML ZIP** → **Run workflow**.
4. Quando a execução terminar com sucesso, abra a execução e baixe o artefato **APK-do-projeto-HTML**.

## Limitações importantes
- O fluxo de APK HTML exige enviar o ZIP ao repositório e iniciar a compilação no GitHub; o aplicativo Android ainda não compila APKs localmente nem envia arquivos para o GitHub automaticamente.
- O APK gerado abre o HTML empacotado por WebView; ele não converte automaticamente recursos web que dependam de servidores externos, APIs ou funcionalidades nativas Android.
- O resultado é um APK de depuração para testes, não uma versão assinada para publicação na Play Store.
- Não coloque senhas, chaves privadas ou tokens no HTML/ZIP.
