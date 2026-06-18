"""
deploy_panel.py — Painel de Deploy Profissional · Tô Contando
================================================================
Arquitetura:
  • Um terminal independente por serviço (aba no notebook)
  • Terminal global para "Deploy Tudo"
  • Sidebar com cards de status ao vivo
  • Barra de progresso animada durante deploy
  • Histórico de deploys por serviço (persistido em JSON)
  • Sistema de erros inteligente: classifica, destaca e sugere fix
  • Cancelamento de processo em andamento
  • Atalhos de teclado
"""

import tkinter as tk
from tkinter import ttk
import subprocess
import threading
import os
import json
import re
import signal
import time
from datetime import datetime
from pathlib import Path

import ctypes

# Ocultar a janela do CMD se for executado com python.exe
try:
    kernel32 = ctypes.WinDLL('kernel32')
    user32 = ctypes.WinDLL('user32')
    hWnd = kernel32.GetConsoleWindow()
    if hWnd:
        user32.ShowWindow(hWnd, 0)
except Exception:
    pass

# ══════════════════════════════════════════════════════════════════════════════
#  CONFIGURAÇÃO DE CAMINHOS
# ══════════════════════════════════════════════════════════════════════════════
ROOT_DIR      = Path(__file__).parent.resolve()
BACKEND_DIR   = ROOT_DIR / "quanto-falta-server" / "backend"
DASHBOARD_DIR = ROOT_DIR / "quanto-falta-server" / "dashboard"
FRONTEND_DIR  = ROOT_DIR / "quanto-falta-web"
NEW_SITE_DIR  = ROOT_DIR / "novositequantofalta"
HISTORY_FILE  = ROOT_DIR / ".deploy_history.json"

# ══════════════════════════════════════════════════════════════════════════════
#  DESIGN TOKENS
# ══════════════════════════════════════════════════════════════════════════════
C = {
    # fundos
    "bg":         "#0d0f14",
    "surface":    "#13161f",
    "surface2":   "#1a1e2e",
    "surface3":   "#1f2438",
    "panel":      "#111420",

    # bordas
    "border":     "#252a42",
    "border2":    "#2e3558",

    # accents
    "accent":     "#4f7cff",
    "accent_dim": "#2d4baa",
    "accent_glow":"#4f7cff22",

    # semânticas
    "success":    "#22c55e",
    "success_dim":"#14532d",
    "error":      "#ef4444",
    "error_dim":  "#7f1d1d",
    "warning":    "#f59e0b",
    "warning_dim":"#78350f",
    "info":       "#38bdf8",

    # texto
    "text":       "#e2e8f0",
    "text_dim":   "#64748b",
    "text_faint": "#334155",
    "text_code":  "#a5f3fc",

    # terminal
    "term_bg":    "#090b10",
    "term_sel":   "#1e3a5f",
}

FONT_MONO  = None   # resolvido em runtime
FONT_UI    = "Segoe UI"
FONT_BOLD  = ("Segoe UI", 9, "bold")
FONT_SMALL = ("Segoe UI", 8)
FONT_TINY  = ("Segoe UI", 7)

# ══════════════════════════════════════════════════════════════════════════════
#  DEFINIÇÃO DE SERVIÇOS
# ══════════════════════════════════════════════════════════════════════════════
SERVICES = [
    {
        "key":    "backend",
        "label":  "API Backend",
        "badge":  "Workers",
        "icon":   "⚙",
        "color":  "#818cf8",   # indigo
        "cwd":    BACKEND_DIR,
        "cmd":    "npx wrangler deploy",
        "desc":   "Cloudflare Workers",
        "tips": {
            "wrangler":      "Verifique se o wrangler está instalado: npm i -g wrangler",
            "authentication":"Execute: npx wrangler login",
            "not found":     "Confirme que o wrangler.toml existe no diretório do backend",
            "enoent":        "Diretório não encontrado. Verifique ROOT_DIR no topo do script",
            "timeout":       "Timeout de rede — verifique conexão ou proxy corporativo",
            "unauthorized":  "Token expirado — execute: npx wrangler login",
        },
    },
    {
        "key":    "dashboard",
        "label":  "Dashboard",
        "badge":  "Next.js",
        "icon":   "▦",
        "color":  "#34d399",   # emerald
        "cwd":    DASHBOARD_DIR,
        "cmd":    r"npm run build ; node_modules\.bin\wrangler pages deploy out --project-name quanto-falta-dashboard",
        "desc":   "Cloudflare Pages · build + deploy",
        "tips": {
            "build failed":  "Erro no build Next.js — verifique erros de TypeScript ou imports inválidos",
            "out of memory": "Aumente memória: set NODE_OPTIONS=--max-old-space-size=4096",
            "wrangler":      "Instale localmente: npm install wrangler --save-dev",
            "enoent":        "Pasta 'out' não gerada — rode 'npm run build' manualmente para ver o erro",
            "pages":         "Projeto não existe no Cloudflare Pages — crie via dashboard primeiro",
        },
    },
    {
        "key":    "frontend_velho",
        "label":  "Site Antigo",
        "badge":  "Pages",
        "icon":   "◈",
        "color":  "#fb923c",   # orange
        "cwd":    FRONTEND_DIR,
        "cmd":    "npx wrangler pages deploy frontend --project-name quanto-falta-web",
        "desc":   "Cloudflare Pages · site legado",
        "tips": {
            "wrangler":      "Verifique se o wrangler está instalado: npm i -g wrangler",
            "not found":     "Pasta 'frontend' não existe no diretório",
            "enoent":        "Diretório quanto-falta-web não encontrado. Verifique o caminho",
            "unauthorized":  "Token expirado — execute: npx wrangler login",
        },
    },
    {
        "key":    "frontend_novo",
        "label":  "Site Novo",
        "badge":  "Pages",
        "icon":   "✨",
        "color":  "#f43f5e",   # rose
        "cwd":    NEW_SITE_DIR,
        "cmd":    "npx wrangler pages deploy . --project-name novositequantofalta",
        "desc":   "Cloudflare Pages · novo site",
        "tips": {
            "wrangler":      "Verifique se o wrangler está instalado: npm i -g wrangler",
            "not found":     "Diretório novositequantofalta não encontrado",
            "unauthorized":  "Token expirado — execute: npx wrangler login",
        },
    },
]

SVC_BY_KEY = {s["key"]: s for s in SERVICES}

# ══════════════════════════════════════════════════════════════════════════════
#  HISTÓRICO DE DEPLOYS
# ══════════════════════════════════════════════════════════════════════════════
class DeployHistory:
    def __init__(self, path: Path):
        self.path = path
        self._data: dict = self._load()

    def _load(self) -> dict:
        if self.path.exists():
            try:
                return json.loads(self.path.read_text(encoding="utf-8"))
            except Exception:
                pass
        return {s["key"]: [] for s in SERVICES}

    def _save(self):
        try:
            self.path.write_text(json.dumps(self._data, ensure_ascii=False, indent=2),
                                 encoding="utf-8")
        except Exception:
            pass

    def record(self, key: str, ok: bool, duration_s: float, error_snippet: str = ""):
        entry = {
            "ts":       datetime.now().isoformat(timespec="seconds"),
            "ok":       ok,
            "duration": round(duration_s, 1),
            "error":    error_snippet[:300] if error_snippet else "",
        }
        if key not in self._data:
            self._data[key] = []
        self._data[key].insert(0, entry)
        self._data[key] = self._data[key][:30]   # mantém últimos 30
        self._save()

    def last(self, key: str) -> dict | None:
        entries = self._data.get(key, [])
        return entries[0] if entries else None

    def all(self, key: str) -> list:
        return self._data.get(key, [])


# ══════════════════════════════════════════════════════════════════════════════
#  ANÁLISE DE ERROS
# ══════════════════════════════════════════════════════════════════════════════
def classify_error(output: str, svc: dict) -> tuple[str, str]:
    """
    Retorna (categoria, sugestão) baseado no output e nas tips do serviço.
    """
    low = output.lower()
    for pattern, tip in svc.get("tips", {}).items():
        if pattern in low:
            return (pattern, tip)
    # Genéricos
    if "enoent" in low or "no such file" in low:
        return ("arquivo não encontrado", "Verifique se os diretórios do projeto existem.")
    if "network" in low or "econnrefused" in low or "fetch failed" in low:
        return ("erro de rede", "Verifique conexão com a internet e configurações de proxy.")
    if "permission" in low or "eacces" in low:
        return ("permissão negada", "Tente executar como administrador ou verifique permissões.")
    if "syntax" in low or "syntaxerror" in low:
        return ("erro de sintaxe", "Verifique o código-fonte — há erros de JavaScript/TypeScript.")
    return ("erro desconhecido", "Consulte o log completo acima para mais detalhes.")


# ══════════════════════════════════════════════════════════════════════════════
#  PROCESSO DE DEPLOY
# ══════════════════════════════════════════════════════════════════════════════
class DeployProcess:
    """
    Encapsula um subprocess de deploy com suporte a cancelamento,
    callback de saída linha a linha, e coleta de saída para análise.
    """
    def __init__(self, svc: dict):
        self.svc      = svc
        self._proc    = None
        self._output  = []
        self.cancelled = False

    def run(self, on_line, on_done):
        start = time.monotonic()
        try:
            self._proc = subprocess.Popen(
                ["powershell", "-Command", self.svc["cmd"]],
                cwd=str(self.svc["cwd"]),
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                bufsize=1,
                encoding="utf-8",
                errors="replace",
                creationflags=0x08000000, # CREATE_NO_WINDOW
            )
            for line in self._proc.stdout:
                self._output.append(line)
                on_line(line)
                if self.cancelled:
                    break
            self._proc.wait()
            ok = (not self.cancelled) and (self._proc.returncode == 0)

        except FileNotFoundError:
            msg = (
                f"ERRO: PowerShell não encontrado.\n"
                f"  Este script requer Windows com PowerShell disponível no PATH.\n"
            )
            on_line(msg)
            self._output.append(msg)
            ok = False

        except Exception as exc:
            msg = f"EXCEÇÃO INESPERADA: {exc}\n"
            on_line(msg)
            self._output.append(msg)
            ok = False

        duration  = time.monotonic() - start
        full_out  = "".join(self._output)
        error_cat, error_tip = ("", "") if ok else classify_error(full_out, self.svc)
        on_done(ok, duration, full_out, error_cat, error_tip)

    def cancel(self):
        self.cancelled = True
        if self._proc:
            try:
                self._proc.terminate()
            except Exception:
                pass

    @property
    def full_output(self):
        return "".join(self._output)


# ══════════════════════════════════════════════════════════════════════════════
#  WIDGET: TERMINAL
# ══════════════════════════════════════════════════════════════════════════════
class Terminal(tk.Frame):
    """
    Widget de terminal com:
    - Output colorido (tags semânticas)
    - Barra de status inline
    - Spinner animado
    - Botão de copiar output
    """
    SPINNER = ["⠋","⠙","⠹","⠸","⠼","⠴","⠦","⠧","⠇","⠏"]

    def __init__(self, parent, svc: dict, **kw):
        super().__init__(parent, bg=C["term_bg"], **kw)
        self.svc = svc
        self._spinner_i  = 0
        self._spin_after = None
        self._build()

    def _build(self):
        # ── barra de título do terminal ──────────────────────────────────────
        bar = tk.Frame(self, bg=C["surface2"], pady=4)
        bar.pack(fill=tk.X)

        self._icon_lbl = tk.Label(bar, text=self.svc["icon"], width=2,
                                   font=(FONT_UI, 11), fg=self.svc["color"],
                                   bg=C["surface2"])
        self._icon_lbl.pack(side=tk.LEFT, padx=(10,2))

        tk.Label(bar, text=self.svc["label"], font=(FONT_UI, 9, "bold"),
                 fg=C["text"], bg=C["surface2"]).pack(side=tk.LEFT)

        tk.Label(bar, text=self.svc["badge"],
                 font=FONT_TINY, fg=self.svc["color"],
                 bg=C["surface3"], padx=6, pady=1).pack(side=tk.LEFT, padx=8)

        self._status_badge = tk.Label(bar, text="● idle",
                                       font=FONT_TINY, fg=C["text_dim"],
                                       bg=C["surface2"], padx=6)
        self._status_badge.pack(side=tk.RIGHT, padx=6)

        self._spinner_lbl = tk.Label(bar, text="", font=(FONT_UI, 10),
                                      fg=C["accent"], bg=C["surface2"], width=2)
        self._spinner_lbl.pack(side=tk.RIGHT)

        # ── área de texto ─────────────────────────────────────────────────────
        txt_frame = tk.Frame(self, bg=C["term_bg"])
        txt_frame.pack(fill=tk.BOTH, expand=True)
        txt_frame.rowconfigure(0, weight=1)
        txt_frame.columnconfigure(0, weight=1)

        self.text = tk.Text(
            txt_frame,
            bg=C["term_bg"], fg=C["text"],
            font=(FONT_MONO, 9),
            wrap=tk.WORD,
            relief=tk.FLAT,
            padx=12, pady=8,
            insertbackground=C["text"],
            selectbackground=C["term_sel"],
            state=tk.DISABLED,
        )
        self.text.grid(row=0, column=0, sticky="nsew")

        sb = ttk.Scrollbar(txt_frame, orient=tk.VERTICAL, command=self.text.yview)
        sb.grid(row=0, column=1, sticky="ns")
        self.text["yscrollcommand"] = sb.set

        # Tags semânticas
        self.text.tag_configure("ts",      foreground=C["text_faint"])
        self.text.tag_configure("cmd",     foreground=C["accent"],    font=(FONT_MONO, 9, "bold"))
        self.text.tag_configure("out",     foreground=C["text"])
        self.text.tag_configure("ok",      foreground=C["success"],   font=(FONT_MONO, 9, "bold"))
        self.text.tag_configure("err",     foreground=C["error"])
        self.text.tag_configure("err_hl",  foreground=C["error"],     font=(FONT_MONO, 9, "bold"),
                                background=C["error_dim"])
        self.text.tag_configure("warn",    foreground=C["warning"])
        self.text.tag_configure("hdr",     foreground=C["text_dim"],  font=(FONT_MONO, 8))
        self.text.tag_configure("divider", foreground=C["border2"])
        self.text.tag_configure("tip",     foreground=C["info"],      font=(FONT_MONO, 9, "italic"))
        self.text.tag_configure("tip_hdr", foreground=C["warning"],   font=(FONT_MONO, 9, "bold"))

        # ── rodapé ────────────────────────────────────────────────────────────
        foot = tk.Frame(self, bg=C["surface"], pady=3)
        foot.pack(fill=tk.X)

        self._foot_lbl = tk.Label(foot, text="Aguardando.", font=FONT_TINY,
                                   fg=C["text_dim"], bg=C["surface"])
        self._foot_lbl.pack(side=tk.LEFT, padx=10)

        tk.Button(foot, text="Copiar log", font=FONT_TINY,
                  fg=C["text_dim"], bg=C["surface"], relief=tk.FLAT,
                  cursor="hand2", activeforeground=C["text"],
                  activebackground=C["surface2"],
                  command=self._copy_log).pack(side=tk.RIGHT, padx=6)

        tk.Button(foot, text="Limpar", font=FONT_TINY,
                  fg=C["text_dim"], bg=C["surface"], relief=tk.FLAT,
                  cursor="hand2", activeforeground=C["text"],
                  activebackground=C["surface2"],
                  command=self.clear).pack(side=tk.RIGHT, padx=2)

    # ── API pública ───────────────────────────────────────────────────────────
    def write(self, text: str, tag: str = "out"):
        """Thread-safe: agenda inserção no loop Tk."""
        self.after(0, self._insert, text, tag)

    def _insert(self, text: str, tag: str):
        self.text.config(state=tk.NORMAL)
        # Auto-classifica linhas de erro/aviso (palavras-chave)
        low = text.lower()
        if tag == "out":
            if any(k in low for k in ("error", "erro", "failed", "falhou", "exception")):
                tag = "err"
            elif any(k in low for k in ("warn", "warning", "aviso")):
                tag = "warn"
        self.text.insert(tk.END, text, tag)
        self.text.see(tk.END)
        self.text.config(state=tk.DISABLED)

    def write_divider(self, label: str = ""):
        line = "─" * 58
        if label:
            self.write(f"\n  {line}\n  {label}\n  {line}\n\n", "divider")
        else:
            self.write(f"  {line}\n", "divider")

    def write_error_card(self, category: str, tip: str, snippet: str):
        self.write("\n", "out")
        self.write(f"  ╔══ FALHA DETECTADA ══╗\n", "err_hl")
        self.write(f"  Categoria : {category}\n", "err")
        self.write(f"  Sugestão  : {tip}\n", "tip")
        if snippet:
            # Últimas 5 linhas do output como contexto
            lines = [l for l in snippet.splitlines() if l.strip()]
            excerpt = "\n".join(lines[-5:])
            self.write(f"\n  Trecho relevante:\n", "tip_hdr")
            for ln in excerpt.splitlines():
                self.write(f"    {ln}\n", "err")
        self.write("\n", "out")

    def clear(self):
        self.text.config(state=tk.NORMAL)
        self.text.delete("1.0", tk.END)
        self.text.config(state=tk.DISABLED)
        self._foot_lbl.config(text="Log limpo.")

    def set_status(self, state: str, extra: str = ""):
        """state: idle | running | ok | error"""
        configs = {
            "idle":    ("● idle",    C["text_dim"]),
            "running": ("● running", C["warning"]),
            "ok":      ("● ok",      C["success"]),
            "error":   ("● error",   C["error"]),
        }
        text, color = configs.get(state, ("●", C["text_dim"]))
        self.after(0, lambda: self._status_badge.config(text=text, fg=color))
        if extra:
            self.after(0, lambda: self._foot_lbl.config(text=extra))

    def start_spinner(self):
        self._spin()

    def stop_spinner(self):
        if self._spin_after:
            self.after_cancel(self._spin_after)
            self._spin_after = None
        self._spinner_lbl.config(text="")

    def _spin(self):
        self._spinner_lbl.config(text=self.SPINNER[self._spinner_i % len(self.SPINNER)])
        self._spinner_i += 1
        self._spin_after = self.after(80, self._spin)

    def _copy_log(self):
        content = self.text.get("1.0", tk.END)
        self.clipboard_clear()
        self.clipboard_append(content)
        self._foot_lbl.config(text="Log copiado para área de transferência.")
        self.after(2500, lambda: self._foot_lbl.config(text=""))

    def set_footer(self, text: str):
        self.after(0, lambda: self._foot_lbl.config(text=text))


# ══════════════════════════════════════════════════════════════════════════════
#  WIDGET: CARD DE SERVIÇO (sidebar)
# ══════════════════════════════════════════════════════════════════════════════
class ServiceCard(tk.Frame):
    def __init__(self, parent, svc: dict, on_deploy, on_cancel, on_select, history: DeployHistory, **kw):
        super().__init__(parent, bg=C["surface2"],
                         highlightbackground=C["border"], highlightthickness=1, **kw)
        self.svc        = svc
        self._on_deploy = on_deploy
        self._on_cancel = on_cancel
        self._on_select = on_select
        self._history   = history
        self._state     = "idle"
        self._process   = None
        self._build()
        self._refresh_last()

    def _build(self):
        # ── cabeçalho ─────────────────────────────────────────────────────────
        hdr = tk.Frame(self, bg=C["surface2"])
        hdr.pack(fill=tk.X, padx=10, pady=(10, 4))
        hdr.bind("<Button-1>", lambda e: self._on_select(self.svc["key"]))

        # Faixinha de cor lateral
        accent_bar = tk.Frame(self, bg=self.svc["color"], width=3)
        accent_bar.place(x=0, y=0, relheight=1.0)

        tk.Label(hdr, text=self.svc["icon"], font=(FONT_UI, 13),
                 fg=self.svc["color"], bg=C["surface2"]).pack(side=tk.LEFT, padx=(4,4))

        name_frame = tk.Frame(hdr, bg=C["surface2"])
        name_frame.pack(side=tk.LEFT, fill=tk.X, expand=True)

        tk.Label(name_frame, text=self.svc["label"], font=(FONT_UI, 10, "bold"),
                 fg=C["text"], bg=C["surface2"]).pack(anchor="w")
        tk.Label(name_frame, text=self.svc["desc"], font=FONT_TINY,
                 fg=C["text_dim"], bg=C["surface2"]).pack(anchor="w")

        self._dot = tk.Label(hdr, text="●", font=(FONT_UI, 14),
                              fg=C["text_faint"], bg=C["surface2"])
        self._dot.pack(side=tk.RIGHT)

        # ── status + última execução ──────────────────────────────────────────
        mid = tk.Frame(self, bg=C["surface2"])
        mid.pack(fill=tk.X, padx=14, pady=(0, 4))

        self._status_lbl = tk.Label(mid, text="Aguardando", font=FONT_SMALL,
                                     fg=C["text_dim"], bg=C["surface2"])
        self._status_lbl.pack(side=tk.LEFT)

        self._last_lbl = tk.Label(mid, text="", font=FONT_TINY,
                                   fg=C["text_faint"], bg=C["surface2"])
        self._last_lbl.pack(side=tk.RIGHT)

        # ── barra de progresso ────────────────────────────────────────────────
        self._progress = ttk.Progressbar(self, mode="indeterminate",
                                          style="Deploy.Horizontal.TProgressbar")
        self._progress.pack(fill=tk.X, padx=10, pady=(0, 6))

        # ── botões ────────────────────────────────────────────────────────────
        btn_row = tk.Frame(self, bg=C["surface2"])
        btn_row.pack(fill=tk.X, padx=10, pady=(0, 10))

        self._deploy_btn = tk.Button(
            btn_row, text="Deploy",
            font=(FONT_UI, 9, "bold"),
            fg=C["text"], bg=self.svc["color"],
            activeforeground=C["text"],
            activebackground=self.svc["color"],
            relief=tk.FLAT, cursor="hand2", pady=5,
            command=lambda: self._on_deploy(self.svc["key"]),
        )
        self._deploy_btn.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 3))

        self._cancel_btn = tk.Button(
            btn_row, text="✕",
            font=(FONT_UI, 9, "bold"),
            fg=C["text_dim"], bg=C["surface3"],
            activeforeground=C["error"],
            activebackground=C["surface3"],
            relief=tk.FLAT, cursor="hand2", pady=5, width=3,
            command=lambda: self._on_cancel(self.svc["key"]),
            state=tk.DISABLED,
        )
        self._cancel_btn.pack(side=tk.LEFT)

        # Clique no card → seleciona aba
        for widget in (self, hdr, name_frame, mid):
            widget.bind("<Button-1>", lambda e: self._on_select(self.svc["key"]))

    def set_state(self, state: str):
        self._state = state
        colors = {
            "idle":    C["text_faint"],
            "running": C["warning"],
            "ok":      C["success"],
            "error":   C["error"],
        }
        labels = {
            "idle":    "Aguardando",
            "running": "Em andamento…",
            "ok":      "Concluído",
            "error":   "Falhou",
        }
        self._dot.config(fg=colors.get(state, C["text_faint"]))
        self._status_lbl.config(text=labels.get(state, ""), fg=colors.get(state, C["text_dim"]))

        running = state == "running"
        self._deploy_btn.config(state=tk.DISABLED if running else tk.NORMAL,
                                 bg=C["text_faint"] if running else self.svc["color"])
        self._cancel_btn.config(state=tk.NORMAL if running else tk.DISABLED)

        if running:
            self._progress.start(12)
        else:
            self._progress.stop()
            self._progress["value"] = 0

    def refresh_last(self):
        last = self._history.last(self.svc["key"])
        if last:
            ts  = last["ts"][11:16]   # HH:MM
            ok  = last["ok"]
            dur = last["duration"]
            sym = "✓" if ok else "✗"
            col = C["success"] if ok else C["error"]
            self._last_lbl.config(text=f"{sym} {ts}  {dur}s", fg=col)

    def _refresh_last(self):
        self.refresh_last()


# ══════════════════════════════════════════════════════════════════════════════
#  WIDGET: PAINEL DE HISTÓRICO
# ══════════════════════════════════════════════════════════════════════════════
class HistoryPanel(tk.Frame):
    def __init__(self, parent, history: DeployHistory, **kw):
        super().__init__(parent, bg=C["surface"], **kw)
        self._history = history
        self._build()

    def _build(self):
        tk.Label(self, text="HISTÓRICO DE DEPLOYS", font=(FONT_UI, 8, "bold"),
                 fg=C["text_dim"], bg=C["surface"]).pack(anchor="w", padx=10, pady=(8,4))

        self._inner = tk.Frame(self, bg=C["surface"])
        self._inner.pack(fill=tk.BOTH, expand=True, padx=6)

    def refresh(self):
        for w in self._inner.winfo_children():
            w.destroy()

        for svc in SERVICES:
            entries = self._history.all(svc["key"])
            if not entries:
                continue

            tk.Label(self._inner, text=f"{svc['icon']} {svc['label']}",
                     font=(FONT_UI, 8, "bold"), fg=svc["color"],
                     bg=C["surface"]).pack(anchor="w", pady=(6,1))

            for e in entries[:5]:
                sym = "✓" if e["ok"] else "✗"
                col = C["success"] if e["ok"] else C["error"]
                ts  = e["ts"][5:16].replace("T", " ")
                row = tk.Frame(self._inner, bg=C["surface"])
                row.pack(fill=tk.X)
                tk.Label(row, text=sym, fg=col, bg=C["surface"],
                         font=(FONT_UI, 9, "bold"), width=2).pack(side=tk.LEFT)
                tk.Label(row, text=ts, fg=C["text_dim"], bg=C["surface"],
                         font=FONT_TINY).pack(side=tk.LEFT)
                tk.Label(row, text=f"{e['duration']}s", fg=C["text_faint"],
                         bg=C["surface"], font=FONT_TINY).pack(side=tk.RIGHT)

            tk.Frame(self._inner, bg=C["border"], height=1).pack(fill=tk.X, pady=4)


# ══════════════════════════════════════════════════════════════════════════════
#  APLICAÇÃO PRINCIPAL
# ══════════════════════════════════════════════════════════════════════════════
class DeployApp:
    def __init__(self, root: tk.Tk):
        self.root     = root
        self.history  = DeployHistory(HISTORY_FILE)
        self._procs: dict[str, DeployProcess | None] = {s["key"]: None for s in SERVICES}

        self._resolve_font()
        self._apply_styles()
        self._build_ui()
        self._setup_keybinds()

    # ── setup ──────────────────────────────────────────────────────────────
    def _resolve_font(self):
        global FONT_MONO
        import tkinter.font as tkf
        families = tkf.families()
        for candidate in ("Cascadia Code", "JetBrains Mono", "Fira Code", "Consolas", "Courier New"):
            if candidate in families:
                FONT_MONO = candidate
                break
        if not FONT_MONO:
            FONT_MONO = "Courier New"

    def _apply_styles(self):
        style = ttk.Style()
        style.theme_use("clam")

        style.configure("TNotebook", background=C["bg"], borderwidth=0)
        style.configure("TNotebook.Tab",
                         background=C["surface2"], foreground=C["text_dim"],
                         padding=[14, 6], font=(FONT_UI, 9),
                         borderwidth=0)
        style.map("TNotebook.Tab",
                  background=[("selected", C["surface3"]), ("active", C["surface3"])],
                  foreground=[("selected", C["text"]),     ("active", C["text"])])

        style.configure("Deploy.Horizontal.TProgressbar",
                         troughcolor=C["surface3"],
                         background=C["accent"],
                         borderwidth=0, thickness=3)

        style.configure("TScrollbar",
                         background=C["surface2"],
                         troughcolor=C["surface"],
                         borderwidth=0, arrowsize=0)
        style.map("TScrollbar", background=[("active", C["border2"])])

    def _build_ui(self):
        self.root.title("Deploy · Tô Contando")
        self.root.configure(bg=C["bg"])
        self.root.geometry("1100x700")
        self.root.minsize(860, 540)

        # ── titlebar personalizada ─────────────────────────────────────────
        titlebar = tk.Frame(self.root, bg=C["panel"], pady=0)
        titlebar.pack(fill=tk.X)

        # Indicador de projeto
        tk.Label(titlebar, text="  ⬡  QUANTO FALTA?",
                 font=(FONT_UI, 10, "bold"), fg=C["accent"],
                 bg=C["panel"]).pack(side=tk.LEFT, padx=8, pady=8)

        tk.Label(titlebar, text="Deploy Console",
                 font=(FONT_UI, 9), fg=C["text_dim"],
                 bg=C["panel"]).pack(side=tk.LEFT)

        # Status geral
        self._global_status = tk.Label(titlebar, text="",
                                        font=FONT_TINY, fg=C["text_dim"],
                                        bg=C["panel"])
        self._global_status.pack(side=tk.RIGHT, padx=12)

        # Separador
        tk.Frame(self.root, bg=C["border"], height=1).pack(fill=tk.X)

        # ── layout principal ───────────────────────────────────────────────
        main = tk.Frame(self.root, bg=C["bg"])
        main.pack(fill=tk.BOTH, expand=True)
        main.columnconfigure(1, weight=1)
        main.rowconfigure(0, weight=1)

        # ── sidebar ────────────────────────────────────────────────────────
        sidebar = tk.Frame(main, bg=C["panel"], width=240)
        sidebar.grid(row=0, column=0, sticky="ns")
        sidebar.pack_propagate(False)

        tk.Label(sidebar, text="SERVIÇOS", font=(FONT_UI, 7, "bold"),
                 fg=C["text_dim"], bg=C["panel"]).pack(anchor="w", padx=12, pady=(12,4))

        self._cards: dict[str, ServiceCard] = {}
        for svc in SERVICES:
            card = ServiceCard(
                sidebar, svc,
                on_deploy=self._deploy_one,
                on_cancel=self._cancel,
                on_select=self._select_tab,
                history=self.history,
            )
            card.pack(fill=tk.X, padx=8, pady=4)
            self._cards[svc["key"]] = card

        # Deploy tudo
        tk.Frame(sidebar, bg=C["border"], height=1).pack(fill=tk.X, padx=8, pady=8)
        self._btn_all = tk.Button(
            sidebar, text="⚡  Deploy Tudo",
            font=(FONT_UI, 10, "bold"),
            fg=C["text"], bg=C["accent"],
            activeforeground=C["text"], activebackground=C["accent_dim"],
            relief=tk.FLAT, cursor="hand2", pady=8,
            command=self.deploy_all,
        )
        self._btn_all.pack(fill=tk.X, padx=8)

        # ── histórico ──────────────────────────────────────────────────────
        self._hist_panel = HistoryPanel(sidebar, self.history)
        self._hist_panel.pack(fill=tk.X, padx=0, pady=(12,0))
        self._hist_panel.refresh()

        # Separador vertical
        tk.Frame(main, bg=C["border"], width=1).grid(row=0, column=0,
                                                       sticky="ns", padx=(240,0))

        # ── área de terminais (notebook) ───────────────────────────────────
        right = tk.Frame(main, bg=C["bg"])
        right.grid(row=0, column=1, sticky="nsew")
        right.rowconfigure(0, weight=1)
        right.columnconfigure(0, weight=1)

        self._nb = ttk.Notebook(right)
        self._nb.pack(fill=tk.BOTH, expand=True)

        self._terminals: dict[str, Terminal] = {}
        for svc in SERVICES:
            term = Terminal(self._nb, svc)
            self._nb.add(term, text=f"  {svc['icon']}  {svc['label']}  ")
            self._terminals[svc["key"]] = term
            term.write(
                f"Terminal pronto para {svc['label']}.\n"
                f"Diretório: {svc['cwd']}\n"
                f"Comando:   {svc['cmd']}\n\n",
                "hdr"
            )

        # Aba global "Deploy Tudo"
        self._global_term = Terminal(self._nb, {
            "key": "_all", "icon": "⚡", "label": "Deploy Tudo",
            "badge": "all", "color": C["accent"], "cwd": ROOT_DIR,
            "cmd": "", "desc": "Sequência completa de deploy",
        })
        self._nb.add(self._global_term, text="  ⚡  Deploy Tudo  ")
        self._global_term.write("Terminal de orquestração.\nAqui será exibida a sequência completa.\n\n", "hdr")

        # ── barra de status inferior ───────────────────────────────────────
        statusbar = tk.Frame(self.root, bg=C["surface"], pady=3)
        statusbar.pack(fill=tk.X, side=tk.BOTTOM)
        tk.Frame(self.root, bg=C["border"], height=1).pack(fill=tk.X, side=tk.BOTTOM)

        self._statusbar_lbl = tk.Label(statusbar, text="Pronto.",
                                        font=FONT_TINY, fg=C["text_dim"], bg=C["surface"])
        self._statusbar_lbl.pack(side=tk.LEFT, padx=10)

        font_lbl = tk.Label(statusbar, text=f"font: {FONT_MONO}",
                             font=FONT_TINY, fg=C["text_faint"], bg=C["surface"])
        font_lbl.pack(side=tk.RIGHT, padx=10)

    def _setup_keybinds(self):
        self.root.bind("<F5>", lambda e: self._deploy_focused())
        self.root.bind("<Control-Shift-A>", lambda e: self.deploy_all())
        self.root.bind("<Control-l>", lambda e: self._clear_focused())
        self.root.bind("<Escape>", lambda e: self._cancel_focused())

    # ── seleção de abas ────────────────────────────────────────────────────
    def _select_tab(self, key: str):
        idx = list(self._terminals.keys()).index(key)
        self._nb.select(idx)

    def _focused_key(self) -> str | None:
        idx = self._nb.index(self._nb.select())
        keys = list(self._terminals.keys())
        if idx < len(keys):
            return keys[idx]
        return None

    def _deploy_focused(self):
        key = self._focused_key()
        if key:
            self._deploy_one(key)

    def _clear_focused(self):
        idx = self._nb.index(self._nb.select())
        keys = list(self._terminals.keys())
        if idx < len(keys):
            self._terminals[keys[idx]].clear()
        else:
            self._global_term.clear()

    def _cancel_focused(self):
        key = self._focused_key()
        if key:
            self._cancel(key)

    # ── statusbar ──────────────────────────────────────────────────────────
    def _set_statusbar(self, text: str):
        self.root.after(0, lambda: self._statusbar_lbl.config(text=text))

    # ── deploy individual ──────────────────────────────────────────────────
    def _deploy_one(self, key: str, term: Terminal | None = None, on_complete=None):
        svc  = SVC_BY_KEY[key]
        term = term or self._terminals[key]

        # Verificação de diretório
        if not svc["cwd"].exists():
            term.write(f"\n  ✗  Diretório não encontrado:\n  {svc['cwd']}\n\n", "err_hl")
            term.write("  Verifique o caminho no topo do script (ROOT_DIR).\n\n", "tip")
            self._cards[key].set_state("error")
            term.set_status("error", f"Diretório não encontrado")
            if on_complete:
                on_complete(False)
            return

        # UI: estado running
        self._cards[key].set_state("running")
        self._select_tab(key)
        term.set_status("running", "Deploy em andamento…")
        term.start_spinner()
        term.write_divider(f"{svc['icon']}  {svc['label']}  ·  {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}")
        term.write(f"  $ {svc['cmd']}\n\n", "cmd")
        self._set_statusbar(f"Deployando {svc['label']}…")

        proc = DeployProcess(svc)
        self._procs[key] = proc

        def worker():
            proc.run(
                on_line=lambda line: term.write(line, "out"),
                on_done=lambda ok, dur, out, cat, tip: self._on_deploy_done(
                    key, ok, dur, out, cat, tip, term, on_complete
                ),
            )

        threading.Thread(target=worker, daemon=True, name=f"deploy-{key}").start()

    def _on_deploy_done(self, key, ok, duration, output, error_cat, error_tip,
                         term: Terminal, on_complete):
        self._procs[key] = None

        # Histórico
        self.history.record(key, ok, duration,
                             error_snippet="" if ok else output)
        self.root.after(0, self._hist_panel.refresh)
        self.root.after(0, self._cards[key].refresh_last)

        term.stop_spinner()
        svc = SVC_BY_KEY[key]

        if ok:
            term.write_divider()
            term.write(
                f"  ✓  Deploy concluído em {duration:.1f}s\n\n", "ok"
            )
            self._cards[key].set_state("ok")
            term.set_status("ok", f"Concluído em {duration:.1f}s")
            self._set_statusbar(f"{svc['label']}: deploy concluído em {duration:.1f}s")
        else:
            term.write_divider()
            if not self._procs.get(key + "_cancelled"):
                term.write_error_card(error_cat, error_tip, output)
            term.write(
                f"  ✗  Deploy falhou após {duration:.1f}s\n\n", "err"
            )
            self._cards[key].set_state("error")
            term.set_status("error", f"Falhou — {error_cat}")
            self._set_statusbar(f"{svc['label']}: FALHOU — {error_cat}")

        if on_complete:
            on_complete(ok)

    # ── cancelamento ────────────────────────────────────────────────────────
    def _cancel(self, key: str):
        proc = self._procs.get(key)
        if proc:
            proc.cancel()
            self._terminals[key].write("\n  ⊘  Deploy cancelado pelo usuário.\n\n", "warn")
            self._cards[key].set_state("idle")
            self._terminals[key].set_status("idle", "Cancelado.")
            self._terminals[key].stop_spinner()

    # ── deploy tudo ────────────────────────────────────────────────────────
    def deploy_all(self):
        # Seleciona aba global
        self._nb.select(len(SERVICES))
        gterm = self._global_term
        gterm.clear()

        # Desabilita botão
        self._btn_all.config(state=tk.DISABLED, bg=C["accent_dim"])
        gterm.write_divider(f"⚡  DEPLOY TUDO  ·  {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}")
        gterm.write(f"  Sequência: {' → '.join(s['label'] for s in SERVICES)}\n\n", "hdr")
        self._set_statusbar("Deploy completo em andamento…")

        results: list[tuple[str, bool, float]] = []

        def run_sequence():
            all_ok = True
            for svc in SERVICES:
                key  = svc["key"]
                term = self._terminals[key]
                done_event = threading.Event()
                step_result: dict = {}

                def step_done(ok, _key=key):
                    step_result["ok"] = ok
                    done_event.set()

                # Dispara deploy no thread principal (seguro para Tk)
                self.root.after(0, lambda k=key, t=term, cb=step_done: self._deploy_one(k, t, cb))

                # Aguarda conclusão
                done_event.wait()
                ok  = step_result.get("ok", False)
                dur = self.history.last(key)["duration"] if self.history.last(key) else 0

                results.append((svc["label"], ok, dur))
                all_ok = all_ok and ok

                # Espelho no terminal global
                sym = "✓" if ok else "✗"
                col = "ok" if ok else "err"
                gterm.write(f"  {sym}  {svc['icon']} {svc['label']}  ({dur}s)\n", col)

            # Resumo final
            gterm.write_divider()
            if all_ok:
                gterm.write("  🎉  Todos os deploys concluídos com sucesso!\n\n", "ok")
                self._set_statusbar("Deploy completo: todos os serviços OK.")
            else:
                failed = [r[0] for r in results if not r[1]]
                gterm.write(f"  ⚠  Deploy finalizado com erros em: {', '.join(failed)}\n\n", "err")
                self._set_statusbar(f"Deploy completo: ERROS em {', '.join(failed)}")

            gterm.write("  Duração total:  " +
                         f"{sum(r[2] for r in results):.1f}s\n\n", "hdr")

            # Re-habilita botão
            self.root.after(0, lambda: self._btn_all.config(
                state=tk.NORMAL, bg=C["accent"]
            ))

        threading.Thread(target=run_sequence, daemon=True, name="deploy-all").start()


# ══════════════════════════════════════════════════════════════════════════════
#  ENTRY POINT
# ══════════════════════════════════════════════════════════════════════════════
if __name__ == "__main__":
    root = tk.Tk()
    root.resizable(True, True)
    app = DeployApp(root)
    root.mainloop()