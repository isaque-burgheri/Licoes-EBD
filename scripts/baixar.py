#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Baixa o audio da licao EBD mais recente do canal Rede Brasil Oficial,
renomeia no padrao EBD AAAA-NT-LNN e deixa o arquivo pronto para upload.

Funciona assim:
  1. Le config.json (ano e trimestre, que voce edita 1x por trimestre).
  2. Usa o yt-dlp para listar os videos recentes do canal.
  3. Pega o video mais recente cujo titulo comeca com "EBD" e tem numero de licao.
  4. Verifica em baixados.txt se ja foi baixado (se sim, encerra sem fazer nada).
  5. Baixa so o audio no menor formato disponivel.
  6. Renomeia para EBD AAAA-NT-LNN.<ext> e registra no arquivo de controle.
"""

import json
import os
import re
import subprocess
import sys
from pathlib import Path

# --- Caminhos ---
RAIZ = Path(__file__).resolve().parent.parent
CONFIG = RAIZ / "config.json"
ARQUIVO_CONTROLE = RAIZ / "baixados.txt"
PASTA_SAIDA = RAIZ / "saida"

CANAL_URL = "https://www.youtube.com/@redebrasiloficial/videos"

# Quantos videos recentes do canal inspecionar para achar o EBD mais novo.
QTD_INSPECIONAR = 15

# Se existir um arquivo cookies.txt na raiz, usa para autenticar no YouTube.
COOKIES = RAIZ / "cookies.txt"


def opcoes_cookies():
    """Retorna a lista de args de cookie para o yt-dlp, se o arquivo existir."""
    if COOKIES.exists() and COOKIES.stat().st_size > 0:
        return ["--cookies", str(COOKIES)]
    return []


def ler_config():
    with open(CONFIG, encoding="utf-8") as f:
        cfg = json.load(f)
    ano = int(cfg["ano"])
    trimestre = int(cfg["trimestre"])
    if trimestre not in (1, 2, 3, 4):
        sys.exit(f"ERRO: trimestre invalido no config.json: {trimestre}")
    return ano, trimestre


def extrair_numero_licao(titulo):
    """
    Extrai o numero da licao do titulo do video.
    Cobre formatos como:
      'EBD | 1a LICAO: ...'  'EBD | 12a LICAO ...'
      'EBD - LICAO 03 ...'   'EBD 4a Licao ...'
    Retorna int ou None.
    """
    t = titulo.upper()
    if not t.lstrip().startswith("EBD"):
        return None
    # procura "<numero>a LICAO" ou "LICAO <numero>"
    m = re.search(r"(\d{1,2})\s*[ªAº]?\s*LI[ÇC][ÃA]O", t)
    if not m:
        m = re.search(r"LI[ÇC][ÃA]O\s*(\d{1,2})", t)
    if not m:
        return None
    n = int(m.group(1))
    if 1 <= n <= 13:
        return n
    return None


def listar_videos_canal():
    """Retorna lista de dicts {id, title} dos videos mais recentes do canal."""
    cmd = [
        "yt-dlp",
        "--flat-playlist",
        "--playlist-end", str(QTD_INSPECIONAR),
        "--dump-json",
        *opcoes_cookies(),
        CANAL_URL,
    ]
    saida = subprocess.run(cmd, capture_output=True, text=True)
    if saida.returncode != 0:
        print("Falha ao listar o canal:", saida.stderr, file=sys.stderr)
        sys.exit(1)
    videos = []
    for linha in saida.stdout.strip().splitlines():
        try:
            d = json.loads(linha)
            videos.append({"id": d.get("id"), "title": d.get("title", "")})
        except json.JSONDecodeError:
            continue
    return videos


def ja_baixado(video_id):
    if not ARQUIVO_CONTROLE.exists():
        return False
    return video_id in ARQUIVO_CONTROLE.read_text(encoding="utf-8").split()


def registrar_baixado(video_id):
    with open(ARQUIVO_CONTROLE, "a", encoding="utf-8") as f:
        f.write(video_id + "\n")


def baixar_audio(video_id, nome_base):
    """Baixa o menor audio disponivel e converte para m4a. Retorna o caminho final."""
    PASTA_SAIDA.mkdir(exist_ok=True)
    modelo_saida = str(PASTA_SAIDA / (nome_base + ".%(ext)s"))
    cmd = [
        "yt-dlp",
        "-f", "bestaudio[ext=m4a]/bestaudio/best",
        "-x",
        "--audio-format", "m4a",
        "--audio-quality", "5",   # 0=melhor, 9=menor arquivo; 5 = bom equilibrio
        *opcoes_cookies(),
        "-o", modelo_saida,
        f"https://www.youtube.com/watch?v={video_id}",
    ]
    r = subprocess.run(cmd)
    if r.returncode != 0:
        sys.exit("ERRO: download do audio falhou.")
    arquivos = list(PASTA_SAIDA.glob(nome_base + ".*"))
    if not arquivos:
        sys.exit("ERRO: arquivo de audio nao encontrado apos download.")
    return arquivos[0]


def main():
    ano, trimestre = ler_config()
    print(f"Config: {ano} - {trimestre}o trimestre")

    videos = listar_videos_canal()
    alvo = None
    for v in videos:  # ja vem do mais recente para o mais antigo
        n = extrair_numero_licao(v["title"])
        if n is not None:
            alvo = {"id": v["id"], "title": v["title"], "licao": n}
            break

    if alvo is None:
        print("Nenhum video EBD com numero de licao encontrado nos recentes. Encerrando.")
        return

    print(f"Video EBD mais recente: licao {alvo['licao']:02d} -> {alvo['title']}")

    if ja_baixado(alvo["id"]):
        print("Esse video ja foi baixado antes. Nada a fazer.")
        return

    nome_base = f"EBD {ano}-{trimestre}T-L{alvo['licao']:02d}"
    print(f"Nome do arquivo: {nome_base}")

    caminho = baixar_audio(alvo["id"], nome_base)
    registrar_baixado(alvo["id"])
    print(f"Pronto: {caminho.name} ({caminho.stat().st_size/1_048_576:.1f} MB)")

    # expoe o nome para o passo seguinte do workflow
    gh_out = os.environ.get("GITHUB_OUTPUT")
    if gh_out:
        with open(gh_out, "a") as f:
            f.write(f"arquivo={caminho}\n")
            f.write(f"nome={caminho.name}\n")
            f.write("baixou=sim\n")


if __name__ == "__main__":
    main()
