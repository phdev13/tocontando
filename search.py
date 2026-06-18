import os
import re

root = r'c:\Users\Philippe\Desktop\Quanto falta'
exclude_dirs = {'node_modules', 'build', '.git', '.gradle', 'kspCaches', '.wrangler', 'out', '.next', 'dist', 'tools'}
pattern = re.compile(r'quanto\s*falta|quantofalta|falta\s*pouco|faltapouco|quanto-falta|falta-pouco|quanto_falta', re.IGNORECASE)

matches = []

for r, d, files in os.walk(root):
    d[:] = [dirname for dirname in d if dirname not in exclude_dirs]
    for f in files:
        if f.endswith(('.kt', '.java', '.xml', '.ts', '.js', '.html', '.css', '.json', '.yaml', '.toml', '.md', '.sql', '.txt', '.py', 'example', '.env')):
            filepath = os.path.join(r, f)
            try:
                content = open(filepath, 'r', encoding='utf-8', errors='ignore').read()
                if pattern.search(content):
                    matches.append(filepath)
            except Exception as e:
                pass

with open(os.path.join(root, 'search_results.txt'), 'w', encoding='utf-8') as f:
    f.write('\n'.join(matches))

print(f"Found {len(matches)} files.")
