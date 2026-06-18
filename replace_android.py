import os
import re

dir_to_search = r"c:\Users\Philippe\Desktop\Quanto falta\quanto-falta_"

replacements = [
    (r'https?://quantofalta\.shop', 'https://tocontando.com.br'),
    (r'https?://www\.quantofalta\.shop', 'https://www.tocontando.com.br'),
    (r'https?://api\.quantofalta\.shop', 'https://api.tocontando.com.br'),
    (r'https?://share\.quantofalta\.shop', 'https://share.tocontando.com.br'),
    (r'https?://admin\.quantofalta\.shop', 'https://admin.tocontando.com.br'),
    (r'Tô Contando\?', 'Tô Contando'),
    (r'Tô Contando', 'Tô Contando')
]

extensions = ('.html', '.js', '.json', '.kt', '.xml', '.kts')

def process_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        return
    
    original = content
    for pattern, rep in replacements:
        # Ignore applicationId or package declarations.
        # But wait! 'Tô Contando' has space, so it's not the package name 'quantofalta'.
        # And the domains are fully qualified.
        # But wait! What if 'Tô Contando' matches 'Tô Contando'? Yes.
        # What about 'quantofalta'? The script doesn't replace 'quantofalta', it replaces domains.
        content = re.sub(pattern, rep, content)
        
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated: {filepath}")

for root, dirs, files in os.walk(dir_to_search):
    if 'node_modules' in root or 'build' in root or '.git' in root or '.gradle' in root:
        continue
    for file in files:
        if file.endswith(extensions):
            process_file(os.path.join(root, file))

print("Replacement complete.")
