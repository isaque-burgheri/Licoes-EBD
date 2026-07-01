#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RODE ESTE SCRIPT UMA VEZ NO SEU PC para gerar o token OAuth do Google Drive.

Passos:
  1. Baixe o arquivo de credenciais OAuth (client_secret ...json) do Google Cloud
     e salve na mesma pasta com o nome: client_secret.json
  2. Instale as dependencias:
       pip install google-auth-oauthlib google-api-python-client
  3. Rode:
       python scripts/gerar_token.py
  4. O navegador vai abrir para voce autorizar com a sua conta Google.
  5. No fim, sera criado o arquivo token.json.

Depois, coloque no GitHub (Settings > Secrets):
  - GOOGLE_OAUTH_CLIENT = conteudo do client_secret.json
  - GOOGLE_OAUTH_TOKEN  = conteudo do token.json
"""

from google_auth_oauthlib.flow import InstalledAppFlow

ESCOPOS = ["https://www.googleapis.com/auth/drive"]


def main():
    flow = InstalledAppFlow.from_client_secrets_file("client_secret.json", ESCOPOS)
    creds = flow.run_local_server(port=0)
    with open("token.json", "w") as f:
        f.write(creds.to_json())
    print("\nOK! Arquivo token.json criado.")
    print("Agora copie o conteudo de client_secret.json e token.json")
    print("para os secrets GOOGLE_OAUTH_CLIENT e GOOGLE_OAUTH_TOKEN no GitHub.")


if __name__ == "__main__":
    main()
