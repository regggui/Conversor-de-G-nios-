# Conversor de Gênios

Projeto independente para Android: importar arquivos HTML ou projetos ZIP e abrir a prévia dentro de uma WebView.

## O que esta primeira versão faz
- Importa um arquivo HTML isolado.
- Importa ZIP e extrai os arquivos com proteção contra caminhos fora da pasta.
- Procura `index.html` dentro do projeto.
- Abre a prévia do projeto com suporte a JavaScript e armazenamento local.
- Tem workflow GitHub Actions para compilar o aplicativo Android de teste e guardar o APK como artefato.

## Limitação importante
Esta versão **não transforma ainda o projeto importado em um APK próprio**. Ela é um aplicativo Android que abre a prévia do HTML importado. O próximo passo é implementar um empacotador que gere um projeto Android para cada HTML/ZIP e automatizar sua compilação. Não exponha tokens do GitHub no aplicativo.

## Como obter o APK de teste
1. Abra a aba **Actions** deste repositório.
2. Selecione **Build Android APK** e toque em **Run workflow**.
3. Aguarde a execução ficar verde.
4. Abra a execução concluída e baixe o artefato `Conversor-de-Genios-debug-apk`.

Este é um APK de depuração para teste, não uma versão pronta para publicação na Play Store.
