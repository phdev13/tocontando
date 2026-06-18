import os
import re

dir_to_search = r"c:\Users\Philippe\Desktop\Quanto falta\quanto-falta-web"

# List of replacements (Regex/String, Replacement)
replacements = [
    (r'https?://quantofalta\.shop', 'https://tocontando.com.br'),
    (r'https?://www\.quantofalta\.shop', 'https://www.tocontando.com.br'),
    (r'https?://api\.quantofalta\.shop', 'https://api.tocontando.com.br'),
    (r'https?://share\.quantofalta\.shop', 'https://share.tocontando.com.br'),
    (r'https?://admin\.quantofalta\.shop', 'https://admin.tocontando.com.br'),
    (r'quantofalta\.shop', 'tocontando.com.br'), # remaining mentions
    
    (r'Tô Contando\?', 'Tô Contando'),
    (r'Tô Contando', 'Tô Contando'),
]

extensions = ('.html', '.js', '.json', '.css')

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    for pattern, rep in replacements:
        content = re.sub(pattern, rep, content)
        
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated: {filepath}")

for root, dirs, files in os.walk(dir_to_search):
    if 'node_modules' in root or 'dist' in root or '.git' in root:
        continue
    for file in files:
        if file.endswith(extensions):
            process_file(os.path.join(root, file))

print("Replacement complete.")
