#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Faz upload de um arquivo para uma pasta do Google Drive usando OAuth
(credenciais do proprio usuario). Assim o arquivo vai para o seu Drive
pessoal, usando sua cota de 15GB.

Autenticacao:
  As credenciais OAuth vem de duas variaveis de ambiente (secrets no GitHub):
    - GOOGLE_OAUTH_CLIENT : o JSON do "OAuth client" (tipo Desktop app)
    - GOOGLE_OAUTH_TOKEN  : o JSON do token gerado uma vez no seu PC
                            (contem o refresh_token que renova o acesso sozinho)

Uso:
    python scripts/upload_drive.py "caminho/do/arquivo.mp3"

Se ja existir um arquivo com o mesmo nome na pasta, ele e substituido.
"""

import json
import os
import sys
from pathlib import Path

from google.oauth2.credentials import Credentials
from google.auth.transport.requests import Request
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

# ID da pasta de destino no Drive (da URL que voce compartilhou).
PASTA_DRIVE_ID = "11rhkYRHVdyeB7DfWuHkIi871Tabx2lHP"

ESCOPOS = ["https://www.googleapis.com/auth/drive"]

# Mapa de extensao -> mimetype de audio. Garante que o arquivo suba com o
# Content-Type correto no Drive (nunca application/octet-stream), o que mantem
# o Drive reconhecendo o arquivo como audio.
MIMETYPES_AUDIO = {
    ".mp3": "audio/mpeg",
    ".m4a": "audio/mp4",
    ".aac": "audio/aac",
    ".ogg": "audio/ogg",
    ".opus": "audio/opus",
    ".wav": "audio/wav",
    ".flac": "audio/flac",
}


def mimetype_do_arquivo(caminho: Path) -> str:
    return MIMETYPES_AUDIO.get(caminho.suffix.lower(), "application/octet-stream")


def autenticar():
    token_json = os.environ.get("GOOGLE_OAUTH_TOKEN")
    client_json = os.environ.get("GOOGLE_OAUTH_CLIENT")
    if not token_json:
        sys.exit("ERRO: secret GOOGLE_OAUTH_TOKEN nao definido.")
    if not client_json:
        sys.exit("ERRO: secret GOOGLE_OAUTH_CLIENT nao definido.")

    info = json.loads(token_json)
    client = json.loads(client_json)
    # o token precisa saber o client_id/secret para poder renovar
    dados = client.get("installed") or client.get("web") or {}
    info.setdefault("client_id", dados.get("client_id"))
    info.setdefault("client_secret", dados.get("client_secret"))
    info.setdefault("token_uri", dados.get("token_uri", "https://oauth2.googleapis.com/token"))

    creds = Credentials.from_authorized_user_info(info, ESCOPOS)
    if not creds.valid and creds.refresh_token:
        creds.refresh(Request())
    return build("drive", "v3", credentials=creds)


def achar_existente(service, nome):
    q = (
        f"name = '{nome}' and "
        f"'{PASTA_DRIVE_ID}' in parents and trashed = false"
    )
    resp = service.files().list(q=q, fields="files(id, name)").execute()
    arquivos = resp.get("files", [])
    return arquivos[0]["id"] if arquivos else None


def main():
    if len(sys.argv) < 2:
        sys.exit("Uso: upload_drive.py <caminho_do_arquivo>")
    caminho = Path(sys.argv[1])
    if not caminho.exists():
        sys.exit(f"ERRO: arquivo nao encontrado: {caminho}")

    service = autenticar()
    nome = caminho.name
    mime = mimetype_do_arquivo(caminho)
    media = MediaFileUpload(str(caminho), mimetype=mime, resumable=True)

    existente = achar_existente(service, nome)
    if existente:
        # Atualiza tambem o mimeType nos metadados, caso o arquivo tenha sido
        # enviado antes com um tipo generico.
        service.files().update(
            fileId=existente,
            body={"mimeType": mime},
            media_body=media,
        ).execute()
        print(f"Atualizado no Drive (substituido): {nome} [{mime}]")
    else:
        meta = {"name": nome, "parents": [PASTA_DRIVE_ID], "mimeType": mime}
        service.files().create(body=meta, media_body=media, fields="id").execute()
        print(f"Enviado ao Drive: {nome} [{mime}]")


if __name__ == "__main__":
    main()
