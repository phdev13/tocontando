import os
import re
import json

root = r'c:\Users\Philippe\Desktop\Quanto falta'

with open(os.path.join(root, 'actionable_occurrences.json'), 'r', encoding='utf-8') as f:
    occurrences = json.load(f)

# Files we should skip completely for automated replacements:
# - Migrations (e.g. 0001_initial.sql, backups)
# - Old historical docs
skip_files = ['0001_initial.sql', 'quanto-falta-d1-remote-20260615-204839.sql', 'API_UNIFICATION_INVENTORY.md', 'implementacoes.txt']

replacements_made = 0
files_altered = set()
preserved = []

for occ in occurrences:
    filepath = occ['file']
    if any(skip in filepath for skip in skip_files):
        preserved.append({"file": filepath, "line": occ['line_num'], "reason": "Historical or Migration file"})
        continue

    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except:
        continue
        
    original_content = content

    # 1. Replace "Quanto Falta?" and "Quanto Falta" in UI strings
    # We must be careful not to replace it if it's inside an old URL alias
    content = re.sub(r'Quanto Falta\?', 'Tô Contando', content)
    content = re.sub(r'Quanto Falta', 'Tô Contando', content)
    content = re.sub(r'QuantoFalta', 'ToContando', content)
    
    # Exclude the exact package name `com.phdev.quantofalta` from being corrupted
    # But we already did 'Quanto Falta', not 'quantofalta'
    
    # 2. Replace URLs
    # https://quantofalta.shop -> https://tocontando.com.br
    # https://www.quantofalta.shop -> https://www.tocontando.com.br
    # https://api.quantofalta.shop -> https://api.tocontando.com.br
    # https://share.quantofalta.shop -> https://share.tocontando.com.br
    # https://admin.quantofalta.shop -> https://admin.tocontando.com.br
    
    # But wait, we must preserve legacy endpoints if they are explicitly for backward compatibility
    # If the file is 'OtaApiClient.kt', 'ApiClient.kt' - it should point to the new domain! The old app points to the old domain.
    
    content = re.sub(r'https?://(?:www\.)?quantofalta\.shop', 'https://tocontando.com.br', content)
    content = re.sub(r'https?://api\.quantofalta\.shop', 'https://api.tocontando.com.br', content)
    content = re.sub(r'https?://share\.quantofalta\.shop', 'https://share.tocontando.com.br', content)
    content = re.sub(r'https?://admin\.quantofalta\.shop', 'https://admin.tocontando.com.br', content)
    content = re.sub(r'https?://assets\.quantofalta\.shop', 'https://assets.tocontando.com.br', content)
    
    # Non-url usages of the domain (e.g., config strings)
    # Be very careful here.
    
    # Re-apply package name protections just in case
    content = content.replace('com.phdev.ToContando', 'com.phdev.quantofalta')
    
    if content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        files_altered.add(filepath)
        replacements_made += 1

summary = {
    "replacements_made": replacements_made,
    "files_altered": list(files_altered),
    "preserved": preserved
}

with open(os.path.join(root, 'replace_summary.json'), 'w', encoding='utf-8') as f:
    json.dump(summary, f, indent=2, ensure_ascii=False)

print(f"Altered {len(files_altered)} files. Made {replacements_made} replacements.")
