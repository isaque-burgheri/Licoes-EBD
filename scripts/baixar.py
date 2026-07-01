#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Baixa o audio da licao EBD mais recente do PODCAST da Rede Brasil (via RSS),
renomeia no padrao EBD AAAA-NT-LNN.mp3 e deixa pronto para upload no Drive.

Por que RSS e nao YouTube:
  O YouTube passou a bloquear downloads de servidores (bot check, PO token, 403).
  O audio do podcast e um MP3 comum, livre, sem nenhum desses bloqueios.

Funciona assim:
  1. Le config.json (ano, trimestre e a URL do feed RSS).
  2. Le o feed RSS e lista os episodios (mais recente primeiro).
  3. Pega o episodio mais recente cujo titulo tem numero de licao ("EBD ... Na LICAO").
  4. Verifica em baixados.txt se ja foi baixado (se sim, encerra sem fazer nada).
  5. Baixa o MP3 do episodio.
  6. Renomeia para EBD AAAA-NT-LNN.mp3 e registra no arquivo de controle.
"""

import json
import os
import re
import sys
from pathlib import Path
from urllib.request import urlopen, Request

import feedparser

# --- Caminhos ---
RAIZ = Path(__file__).resolve().parent.parent
CONFIG = RAIZ / "config.json"
ARQUIVO_CONTROLE = RAIZ / "baixados.txt"
PASTA_SAIDA = RAIZ / "saida"


def ler_config():
    with open(CONFIG, encoding="utf-8") as f:
        cfg = json.load(f)
    ano = int(cfg["ano"])
    trimestre = int(cfg["trimestre"])
    if trimestre not in (1, 2, 3, 4):
        sys.exit(f"ERRO: trimestre invalido no config.json: {trimestre}")
    feed_url = cfg.get("feed_rss", "").strip()
    if not feed_url:
        sys.exit("ERRO: 'feed_rss' nao definido no config.json.")
    return ano, trimestre, feed_url


def extrair_numero_licao(titulo):
    """
    Extrai o numero da licao do titulo do episodio.
    Cobre formatos como:
      'EBD | 1a LICAO: ...'   'EBD | 12a LICAO ...'
      '01a LICAO: ... | EBD'  'EBD - LICAO 03 ...'   'EBD 4a Licao ...'
    Retorna int (1-13) ou None.
    """
    t = titulo.upper()
    if "EBD" not in t:
        return None
    m = re.search(r"(\d{1,2})\s*[ªAº]?\s*LI[ÇC][ÃA]O", t)
    if not m:
        m = re.search(r"LI[ÇC][ÃA]O\s*(\d{1,2})", t)
    if not m:
        return None
    n = int(m.group(1))
    if 1 <= n <= 13:
        return n
    return None


def achar_url_audio(entry):
    """Extrai a URL do arquivo de audio (enclosure) de um episodio do RSS."""
    for enc in entry.get("enclosures", []):
        href = enc.get("href") or enc.get("url")
        if href:
            return href
    for link in entry.get("links", []):
        if link.get("rel") == "enclosure" and link.get("href"):
            return link["href"]
    return None


def listar_episodios(feed_url):
    """Retorna lista de dicts {id, title, audio} dos episodios (mais recente primeiro)."""
    feed = feedparser.parse(feed_url)
    if feed.bozo and not feed.entries:
        sys.exit(f"ERRO: nao foi possivel ler o feed RSS: {feed_url}")
    episodios = []
    for e in feed.entries:
        episodios.append({
            "id": e.get("id") or e.get("guid") or e.get("link") or e.get("title"),
            "title": e.get("title", ""),
            "audio": achar_url_audio(e),
        })
    return episodios


def ja_baixado(ep_id):
    if not ARQUIVO_CONTROLE.exists():
        return False
    marcados = ARQUIVO_CONTROLE.read_text(encoding="utf-8").splitlines()
    return ep_id in [m.strip() for m in marcados]


def registrar_baixado(ep_id):
    with open(ARQUIVO_CONTROLE, "a", encoding="utf-8") as f:
        f.write(ep_id + "\n")


def baixar_mp3(url, nome_base):
    """Baixa o MP3 do episodio. Retorna o caminho final."""
    PASTA_SAIDA.mkdir(exist_ok=True)
    destino = PASTA_SAIDA / (nome_base + ".mp3")
    req = Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urlopen(req) as resp, open(destino, "wb") as out:
        while True:
            bloco = resp.read(65536)
            if not bloco:
                break
            out.write(bloco)
    if destino.stat().st_size == 0:
        sys.exit("ERRO: arquivo baixado esta vazio.")
    return destino


def main():
    ano, trimestre, feed_url = ler_config()
    print(f"Config: {ano} - {trimestre}o trimestre")

    episodios = listar_episodios(feed_url)
    alvo = None
    for ep in episodios:  # mais recente primeiro
        n = extrair_numero_licao(ep["title"])
        if n is not None:
            alvo = {**ep, "licao": n}
            break

    if alvo is None:
        print("Nenhum episodio EBD com numero de licao encontrado. Encerrando.")
        return

    print(f"Episodio EBD mais recente: licao {alvo['licao']:02d} -> {alvo['title']}")

    if not alvo["audio"]:
        sys.exit("ERRO: episodio encontrado mas sem URL de audio no feed.")

    if ja_baixado(alvo["id"]):
        print("Esse episodio ja foi baixado antes. Nada a fazer.")
        return

    nome_base = f"EBD {ano}-{trimestre}T-L{alvo['licao']:02d}"
    print(f"Nome do arquivo: {nome_base}")

    caminho = baixar_mp3(alvo["audio"], nome_base)
    registrar_baixado(alvo["id"])
    print(f"Pronto: {caminho.name} ({caminho.stat().st_size/1_048_576:.1f} MB)")

    gh_out = os.environ.get("GITHUB_OUTPUT")
    if gh_out:
        with open(gh_out, "a") as f:
            f.write(f"arquivo={caminho}\n")
            f.write(f"nome={caminho.name}\n")
            f.write("baixou=sim\n")


if __name__ == "__main__":
    main()
