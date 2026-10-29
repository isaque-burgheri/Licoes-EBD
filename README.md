# Lições EBD — App Android

App nativo (Kotlin + Jetpack Compose) para leitura das revistas da Escola Bíblica Dominical da Assembleia de Deus diretamente da pasta pública do Google Drive.

> ✦ **Sem login.** Sem OAuth. Sem SHA-1. Só uma chave de API gratuita.

---

## Capas mais leves (versão 1.6)

A biblioteca usa primeiro as miniaturas fornecidas pelo Google Drive, sem precisar baixar cada PDF inteiro.
As capas ficam em cache no disco e as imagens recentes também em memória (até 8 MB).
O carregamento acompanha as linhas visíveis da biblioteca, com até quatro miniaturas simultâneas.
Quando não houver miniatura válida, o app usa a primeira página do PDF como alternativa, uma revista por vez.
Essa alternativa tem uma fila separada para não bloquear as demais miniaturas.
Mudanças na data de modificação da revista invalidam sua capa em cache.

O ganho depende da rede e da disponibilidade de miniaturas no Drive; não há tempo fixo garantido.
O fluxo de APK executa testes automáticos de cache, concorrência, alternativa por PDF e cancelamento.

## Gerar APK pelo GitHub Actions

1. Abra **Settings → Secrets and variables → Actions → New repository secret**.
2. Cadastre **DRIVE_API_KEY** com sua chave da Google Drive API.
3. Opcional: cadastre **AUDIO_FOLDER_ID** como Secret ou Variable para habilitar os áudios.
4. Abra **Actions → Gerar APK → Run workflow**, mantendo a branch **master**.
5. Quando concluir, abra a execução e baixe **Artifacts → Licoes-EBD-APK**.
6. Extraia o ZIP e instale o arquivo **app-debug.apk** no Android 7 ou superior.

Mudanças no código Android ou na configuração de compilação em master geram um novo APK automaticamente.
Alterações apenas no README ou nos registros de downloads não iniciam outra compilação.
O fluxo usa Java 17 e Gradle 8.7; não é preciso instalar Android Studio no seu computador.

Sem DRIVE_API_KEY, a compilação funciona, mas o APK não consegue carregar a biblioteca do Drive.
A chave é lida dos Secrets durante a compilação e não é gravada no código do repositório.
Como o app acessa o Drive diretamente, a chave fica incorporada no APK e pode ser extraída:
restrinja sua utilização no Google Cloud conforme as necessidades do app.

Este fluxo gera um APK **debug**, para testes e instalação manual.
A assinatura de teste pode mudar entre execuções; não equivale a uma versão release com chave de assinatura fixa.
Para substituir um app já instalado, a assinatura precisa ser compatível.

---

## ⚡ Como abrir e gerar o APK

### 1. Pré-requisitos
- Android Studio **Iguana ou superior** (2023.2+)
- JDK **17** (já vem com o Android Studio)

### 2. Importar
1. Descompacte o ZIP.
2. Android Studio → **File → Open** → selecione a pasta `LicoesEBD`.
3. Aguarde o Gradle sync (3 a 5 min na primeira vez).

### 3. Pegar uma API key do Google (5 min, grátis, sem cartão)

A pasta do Drive é pública — então o app só precisa de uma chave para chamar a API. **Não é login, não é OAuth**, é só um token grátis para identificar o app.

1. Acesse: https://console.cloud.google.com/
2. Crie um projeto (qualquer nome, ex: "LicoesEBD").
3. **APIs & Services → Library** → procure **"Google Drive API"** → **Enable**.
4. **APIs & Services → Credentials → Create Credentials → API key**.
5. Copie a chave (formato `AIzaSy...`).
6. *(Opcional, recomendado)* Clique na chave para restringir:
   - **Application restrictions**: Android apps → adicione package `br.com.licoesebd.app` + SHA-1 do debug (rode `./gradlew signingReport`).
   - **API restrictions**: Restrict key → marque só **Google Drive API**.

### 4. Colar a chave no projeto

Abra (ou crie) o arquivo `local.properties` na **raiz do projeto** (mesma pasta do `settings.gradle.kts`) e adicione:

```properties
DRIVE_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXX
```

> ⚠️ Esse arquivo já está no `.gitignore` — não vai pro git.

### 5. Rodar
- Sync Gradle (botão "elefante" no topo direito do Android Studio).
- Plug o celular com depuração USB ativa → **Run ▶️**.
- Ou crie um emulador (Tools → Device Manager) com Android 7+ (API 24).

### 6. Gerar o APK
**Build → Build Bundle(s) / APK(s) → Build APK(s)**.
APK fica em: `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📂 Trocar a pasta do Drive

A pasta padrão é a do link que você passou.
Para usar outra **pasta pública** ("Qualquer pessoa com o link"), edite:

`app/src/main/java/br/com/licoesebd/app/data/repository/PublicDriveRepository.kt`
```kotlin
private val rootFolderId = "SEU_FOLDER_ID_AQUI"
```

O ID está sempre na URL: `drive.google.com/drive/folders/<ID>`.

---

## 🎙 Aba de Podcasts (áudios)

O app tem uma segunda aba ("Áudios") que lista podcasts da EBD a partir de **outra pasta do Drive**, separada da dos PDFs.

### 1. Criar pasta no Drive
- Crie uma pasta no Drive (ex: "Áudios EBD").
- Compartilhe como **"Qualquer pessoa com o link"**.
- Copie o ID da URL.
- Cole no `local.properties` em `AUDIO_FOLDER_ID=...`.

### 2. Padrão de nomes (automático, zero manutenção)

Coloque os arquivos com nomes assim:

```
EBD 2020-2T-L01.mp3        → Lição 01 do 2º trim de 2020
EBD 2020-2T-L02.mp3        → Lição 02
...
EBD 2020-2T-L13.mp3        → Lição 13
EBD 2020-2T-Revista.mp3    → Áudio cobrindo a revista inteira
```

O app **agrupa automaticamente por ano + trimestre**. Cada grupo vira um "álbum" na lista, com a "Revista completa" no topo e as 13 lições em ordem.

Aceita variações: `L3` ou `L03`, `Lição 3`, maiúsculas/minúsculas. Funciona com `.mp3`, `.m4a`, `.aac`, `.ogg`, `.opus`, qualquer formato `audio/*`.

### 3. Player
- Toque numa faixa para tocar (streaming, sem download).
- Barra inferior com play/pause, seek e fechar.
- Continua tocando enquanto você navega entre as abas.

> Se `AUDIO_FOLDER_ID` ficar vazio no `local.properties`, a aba de Áudios fica em branco mostrando uma mensagem explicativa — sem quebrar o app.

---

## 📱 O que o app faz

- Abre direto na biblioteca (sem login).
- Duas abas: **Lições** (PDFs) e **Áudios** (podcasts).
- Varre recursivamente as pastas públicas do Drive.
- Mostra miniaturas do Drive como capas, com renderização da primeira página do PDF como alternativa.
- Toca numa capa → baixa o PDF para cache local → abre no visualizador de PDF padrão do celular.
- Toca numa faixa de áudio → toca via streaming.
- Tema claro e escuro automático.

---

## 🗂 Estrutura

```
LicoesEBD/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/br/com/licoesebd/app/
│           ├── MainActivity.kt
│           ├── LicoesEbdApp.kt
│           ├── data/
│           │   ├── model/Magazine.kt
│           │   └── repository/PublicDriveRepository.kt   ← REST direto, com API key
│           ├── viewmodel/LibraryViewModel.kt
│           └── ui/
│               ├── AppRoot.kt
│               ├── theme/Theme.kt
│               ├── components/
│               └── screens/
│                   ├── HomeScreen.kt
│                   └── ReaderScreen.kt
├── build.gradle.kts
├── settings.gradle.kts
└── local.properties      ← VOCÊ CRIA, com a DRIVE_API_KEY
```

---

## ❓ Problemas comuns

**"BuildConfig.DRIVE_API_KEY is empty" ou erro 400 da Drive API**
→ você não criou ou colou a chave no `local.properties`. Veja passo 3 e 4.

**Erro 403 "API key not valid"**
→ a chave foi restringida com SHA-1 errado, ou a Drive API não foi habilitada no projeto. Volte ao Cloud Console.

**Lista volta vazia**
→ a pasta não está realmente como "Qualquer pessoa com o link pode visualizar". Confirme nas permissões do Drive.

**PDF não baixa**
→ algum PDF dentro da pasta pode estar com permissão diferente. Cheque arquivo a arquivo se necessário.

**Gradle sync demora muito**
→ normal na primeira vez (~250MB de dependências).

---

Feito com carinho. Que sirva ao seu propósito. ✦

> "Bem-aventurado aquele que lê, e os que ouvem as palavras desta profecia." — Apocalipse 1:3
