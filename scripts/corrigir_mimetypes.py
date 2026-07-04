#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Corrige o mimeType de arquivos de audio JA existentes na pasta do Drive.

Use isso uma unica vez para consertar os audios que foram enviados antes da
correcao (que podem ter subido como 'application/octet-stream'). Nao baixa nem
re-envia nada: apenas ajusta os metadados (mimeType) via API, o que e rapido.

Rode localmente, com as mesmas credenciais OAuth do upload:
    GOOGLE_OAUTH_CLIENT=... GOOGLE_OAUTH_TOKEN=... python scripts/corrigir_mimetypes.py

Ou, se preferir, exporte os dois JSONs para variaveis de ambiente antes de rodar.
"""

import json
import os
import sys

from google.oauth2.credentials import Credentials
from google.auth.transport.requests import Request
from googleapiclient.discovery import build

PASTA_DRIVE_ID = "11rhkYRHVdyeB7DfWuHkIi871Tabx2lHP"
ESCOPOS = ["https://www.googleapis.com/auth/drive"]

MIMETYPES_AUDIO = {
    ".mp3": "audio/mpeg",
    ".m4a": "audio/mp4",
    ".aac": "audio/aac",
    ".ogg": "audio/ogg",
    ".opus": "audio/opus",
    ".wav": "audio/wav",
    ".flac": "audio/flac",
}


def mimetype_por_nome(nome):
    for ext, mime in MIMETYPES_AUDIO.items():
        if nome.lower().endswith(ext):
            return mime
    return None


def autenticar():
    token_json = os.environ.get("GOOGLE_OAUTH_TOKEN")
    client_json = os.environ.get("GOOGLE_OAUTH_CLIENT")
    if not token_json or not client_json:
        sys.exit("ERRO: defina GOOGLE_OAUTH_TOKEN e GOOGLE_OAUTH_CLIENT no ambiente.")
    info = json.loads(token_json)
    client = json.loads(client_json)
    dados = client.get("installed") or client.get("web") or {}
    info.setdefault("client_id", dados.get("client_id"))
    info.setdefault("client_secret", dados.get("client_secret"))
    info.setdefault("token_uri", dados.get("token_uri", "https://oauth2.googleapis.com/token"))
    creds = Credentials.from_authorized_user_info(info, ESCOPOS)
    if not creds.valid and creds.refresh_token:
        creds.refresh(Request())
    return build("drive", "v3", credentials=creds)


def main():
    service = autenticar()
    page_token = None
    total = 0
    corrigidos = 0
    while True:
        resp = service.files().list(
            q=f"'{PASTA_DRIVE_ID}' in parents and trashed = false",
            fields="nextPageToken, files(id, name, mimeType)",
            pageSize=200,
            pageToken=page_token,
        ).execute()
        for f in resp.get("files", []):
            total += 1
            esperado = mimetype_por_nome(f["name"])
            if esperado is None:
                continue  # nao e audio conhecido, ignora
            if f.get("mimeType") == esperado:
                continue  # ja esta correto
            service.files().update(
                fileId=f["id"],
                body={"mimeType": esperado},
            ).execute()
            corrigidos += 1
            print(f"Corrigido: {f['name']}  {f.get('mimeType')} -> {esperado}")
        page_token = resp.get("nextPageToken")
        if not page_token:
            break
    print(f"\nConcluido. {total} arquivos verificados, {corrigidos} corrigidos.")


if __name__ == "__main__":
    main()
