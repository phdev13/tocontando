import os
import argparse
import sys

IGNORED_DIRS = {
    '.git', '.gradle', '.idea', 'build', 'dist', 'out', 'node_modules',
    '.next', '.wrangler', 'coverage', '.cache', 'release', 'tmp', 'temp',
    'venv', '.venv'
}

IGNORED_EXTENSIONS = {
    '.apk', '.aab', '.jar', '.class', '.dex', '.png', '.jpg', '.jpeg',
    '.webp', '.gif', '.ico', '.pdf', '.zip', '.keystore', '.jks', '.db',
    '.sqlite', '.so', '.woff', '.woff2'
}

SELF_FILENAMES = {
    'migrate_to_tocontando.py', 'auto_replace.py', 'replace_android.py',
    'replace_dashboard.py', 'replace_web.py'
}

PROTECTED_TERMS = [
    'com.phdev.quantofalta',
    'QuantoFaltaApplication',
    'QuantoFalta-Worker',
    'phdev13/QuantoFalta',
    'quanto-falta-api',
    'quanto-falta-dashboard',
    'quanto-falta-web',
    'quanto_falta_db',
    # Evita geração acidental de pacote errado
    'com.phdev.tocontando',
]

# Mais específico primeiro para evitar double-replace
REPLACEMENTS = [
    ('https://api-staging.quantofalta.shop', 'https://api-staging.tocontando.com.br'),
    ('https://admin.quantofalta.shop',       'https://admin.tocontando.com.br'),
    ('https://share.quantofalta.shop',       'https://share.tocontando.com.br'),
    ('https://api.quantofalta.shop',         'https://api.tocontando.com.br'),
    ('https://www.quantofalta.shop',         'https://www.tocontando.com.br'),
    ('https://quantofalta.shop',             'https://tocontando.com.br'),

    ('api-staging.quantofalta.shop',         'api-staging.tocontando.com.br'),
    ('admin.quantofalta.shop',               'admin.tocontando.com.br'),
    ('share.quantofalta.shop',               'share.tocontando.com.br'),
    ('api.quantofalta.shop',                 'api.tocontando.com.br'),
    ('www.quantofalta.shop',                 'www.tocontando.com.br'),
    ('quantofalta.shop',                     'tocontando.com.br'),

    ('contato@quantofalta.shop',             'contato@tocontando.com.br'),

    ('Quanto Falta?',                        'Tô Contando'),
    ('Quanto Falta',                         'Tô Contando'),
]

# Tamanho máximo de arquivo lido em memória (10 MB)
MAX_FILE_SIZE = 10 * 1024 * 1024


def is_ignored_path(path: str) -> bool:
    """Retorna True se qualquer componente do caminho estiver em IGNORED_DIRS."""
    parts = path.replace('\\', '/').split('/')
    return any(part in IGNORED_DIRS for part in parts)


def apply_replacements(content: str) -> tuple[str, list[tuple[str, str, int]]]:
    """Aplica REPLACEMENTS em ordem e retorna (novo_conteúdo, lista_de_mudanças)."""
    changes = []
    for old, new in REPLACEMENTS:
        count = content.count(old)
        if count:
            content = content.replace(old, new)
            changes.append((old, new, count))
    return content, changes


def validate_protected(original: str, modified: str, filepath: str) -> bool:
    """
    Garante que nenhum termo protegido teve sua contagem alterada.
    Retorna True se estiver tudo ok, False se detectar violação.
    """
    for term in PROTECTED_TERMS:
        before = original.count(term)
        after = modified.count(term)
        if before != after:
            print(
                f"[ERRO CRÍTICO] '{term}' aparecia {before}x e passaria a {after}x "
                f"em {filepath}. ABORTANDO!"
            )
            return False
    return True


def process_file(filepath: str, apply: bool) -> tuple[int, int]:
    """
    Processa um arquivo. Retorna (arquivos_alterados, total_substituições).
    Encerra o processo em caso de violação de proteção.
    """
    if os.path.getsize(filepath) > MAX_FILE_SIZE:
        print(f"[AVISO] {filepath} excede {MAX_FILE_SIZE // (1024*1024)} MB, ignorando.")
        return 0, 0

    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            original = f.read()
    except (UnicodeDecodeError, PermissionError):
        return 0, 0

    modified, changes = apply_replacements(original)

    if not changes:
        return 0, 0

    if not validate_protected(original, modified, filepath):
        sys.exit(1)

    label = 'APLICAR' if apply else 'DRY-RUN'
    print(f"[{label}] {filepath}")
    total = 0
    for old, new, count in changes:
        print(f"  - {count}x '{old}' -> '{new}'")
        total += count

    if apply:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(modified)

    return 1, total


def main():
    parser = argparse.ArgumentParser(description="Migra a marca Quanto Falta? para Tô Contando.")
    parser.add_argument('--dry-run', action='store_true', help='Simula e relata as substituições')
    parser.add_argument('--apply',   action='store_true', help='Aplica as substituições')
    args = parser.parse_args()

    if not args.dry_run and not args.apply:
        print("Forneça --dry-run ou --apply")
        sys.exit(1)

    if args.dry_run and args.apply:
        print("Use --dry-run OU --apply, não os dois.")
        sys.exit(1)

    mode = "APPLY" if args.apply else "DRY-RUN"
    print(f"Modo {mode} ativo.\n")

    total_replacements = 0
    files_altered = 0

    for root, dirs, files in os.walk('.'):
        # Filtra subdiretórios ignorados in-place (impede os.walk de descer neles)
        dirs[:] = [d for d in dirs if d not in IGNORED_DIRS]

        # Segurança extra: ignora se o próprio root contiver um dir proibido
        if is_ignored_path(root):
            continue

        for filename in files:
            if filename in SELF_FILENAMES:
                continue

            ext = os.path.splitext(filename)[1].lower()
            if ext in IGNORED_EXTENSIONS:
                continue

            filepath = os.path.join(root, filename)
            altered, count = process_file(filepath, apply=args.apply)
            files_altered += altered
            total_replacements += count

    print("\n===============================")
    print(f"Arquivos {'alterados' if args.apply else 'que seriam alterados'}: {files_altered}")
    print(f"Total de substituições: {total_replacements}")
    print("===============================")


if __name__ == '__main__':
    main()