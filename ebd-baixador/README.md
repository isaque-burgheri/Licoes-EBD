# Baixador automático de áudio — EBD

Baixa toda semana, sozinho, o áudio da lição EBD mais recente do canal
**Rede Brasil Oficial** e envia para uma pasta do Google Drive, já com o
nome no padrão `EBD AAAA-NT-LNN.m4a` (ex: `EBD 2026-3T-L02.m4a`).

Roda de graça no GitHub Actions — não precisa deixar nenhum PC ligado.

---

## O que você precisa fazer (só uma vez)

### 1. Colocar estes arquivos no repositório
Copie a pasta inteira para o seu repo (pode ser na raiz). A estrutura é:

```
.github/workflows/baixar-ebd.yml   <- a automação
scripts/baixar.py                  <- acha e baixa o áudio
scripts/upload_drive.py            <- envia pro Drive
config.json                        <- ano e trimestre (você edita)
baixados.txt                       <- controle (não mexer)
```

### 2. Criar a Conta de Serviço do Google (para o upload)
1. Acesse https://console.cloud.google.com/ e crie um projeto (ou use um existente).
2. No menu, vá em **APIs e Serviços > Biblioteca**, procure **Google Drive API** e clique em **Ativar**.
3. Vá em **APIs e Serviços > Credenciais > Criar credenciais > Conta de serviço**. Dê um nome qualquer e finalize.
4. Clique na conta de serviço criada > aba **Chaves > Adicionar chave > Criar nova chave > JSON**. Vai baixar um arquivo `.json`. Guarde-o.
5. Copie o e-mail da conta de serviço (algo como `nome@projeto.iam.gserviceaccount.com`).

### 3. Compartilhar a pasta do Drive com a conta de serviço
1. Abra a [pasta do Drive](https://drive.google.com/drive/folders/11rhkYRHVdyeB7DfWuHkIi871Tabx2lHP).
2. Clique em **Compartilhar** e adicione o e-mail da conta de serviço como **Editor**.

### 4. Guardar a credencial no GitHub
1. No repositório, vá em **Settings > Secrets and variables > Actions > New repository secret**.
2. Nome: `GOOGLE_CREDENTIALS`
3. Valor: cole **todo o conteúdo** do arquivo `.json` que você baixou.
4. Salve.

### Pronto!
A automação já está ativa. Toda segunda às 8h (horário de Brasília) ela
verifica o canal e, se houver uma lição nova, baixa o áudio e joga no Drive.

---

## A cada trimestre (a única manutenção)
Quando virar o trimestre, edite o `config.json` e mude só duas linhas:

```json
"ano": 2026,
"trimestre": 3,
```

Faça commit. Só isso.

---

## Testar agora (sem esperar a segunda-feira)
Na aba **Actions** do GitHub, escolha **"Baixar audio EBD"** e clique em
**Run workflow**. Ele roda na hora.

## Ajustes possíveis
- **Horário:** edite a linha `cron: "0 11 * * 1"` no arquivo `.yml`. O `11`
  é a hora em UTC (Brasília = UTC−3) e o `1` é segunda-feira.
- **Formato/tamanho do áudio:** no `scripts/baixar.py`, o parâmetro
  `--audio-quality` vai de `0` (melhor qualidade, maior) a `9` (menor arquivo).
  Está em `5`.
