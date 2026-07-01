#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Faz upload de um arquivo para uma pasta do Google Drive usando uma
Conta de Servico. A credencial JSON vem da variavel de ambiente
GOOGLE_CREDENTIALS (configurada como secret no GitHub).

Uso:
    python scripts/upload_drive.py "caminho/do/arquivo.m4a"

Se ja existir um arquivo com o mesmo nome na pasta, ele e substituido,
para evitar duplicatas caso o workflow rode mais de uma vez.
"""

import json
import os
import sys
from pathlib import Path

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

# ID da pasta de destino no Drive (da URL que voce compartilhou).
PASTA_DRIVE_ID = "11rhkYRHVdyeB7DfWuHkIi871Tabx2lHP"

ESCOPOS = ["https://www.googleapis.com/auth/drive"]


def autenticar():
    cred_json = os.environ.get("GOOGLE_CREDENTIALS")
    if not cred_json:
        sys.exit("ERRO: secret GOOGLE_CREDENTIALS nao definido.")
    info = json.loads(cred_json)
    creds = service_account.Credentials.from_service_account_info(
        info, scopes=ESCOPOS
    )
    return build("drive", "v3", credentials=creds)


def achar_existente(service, nome):
    q = (
        f"name = '{nome}' and "
        f"'{PASTA_DRIVE_ID}' in parents and trashed = false"
    )
    resp = service.files().list(
        q=q, fields="files(id, name)", supportsAllDrives=True,
        includeItemsFromAllDrives=True,
    ).execute()
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
    media = MediaFileUpload(str(caminho), resumable=True)

    existente = achar_existente(service, nome)
    if existente:
        service.files().update(
            fileId=existente, media_body=media, supportsAllDrives=True
        ).execute()
        print(f"Atualizado no Drive (substituido): {nome}")
    else:
        meta = {"name": nome, "parents": [PASTA_DRIVE_ID]}
        service.files().create(
            body=meta, media_body=media, fields="id",
            supportsAllDrives=True,
        ).execute()
        print(f"Enviado ao Drive: {nome}")


if __name__ == "__main__":
    main()
