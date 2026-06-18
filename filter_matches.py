import os
import re
import json

root = r'c:\Users\Philippe\Desktop\Quanto falta'

files_to_check = []
with open(os.path.join(root, 'search_results.txt'), 'r', encoding='utf-8') as f:
    files_to_check = f.read().splitlines()

# Regex to match the search targets
pattern = re.compile(r'(quanto\s*falta|quantofalta|falta\s*pouco|faltapouco|quanto-falta|falta-pouco|quanto_falta)', re.IGNORECASE)

# Exceptions that should not be replaced
exceptions = [
    re.compile(r'com\.phdev\.quantofalta', re.IGNORECASE),
    re.compile(r'quantofalta\.shop', re.IGNORECASE),
    re.compile(r'quanto-falta-web', re.IGNORECASE),
    re.compile(r'quanto-falta-db', re.IGNORECASE),
    re.compile(r'quanto-falta-server', re.IGNORECASE),
    re.compile(r'novositequantofalta', re.IGNORECASE),
    re.compile(r'quanto-falta-d1', re.IGNORECASE),
]

actionable_occurrences = []

for filepath in files_to_check:
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        
        for i, line in enumerate(lines):
            # Check if line has any match
            if pattern.search(line):
                # Remove exceptions
                cleaned_line = line
                for ex in exceptions:
                    cleaned_line = ex.sub('', cleaned_line)
                
                # If there's still a match in the cleaned line, it's an actionable occurrence
                if pattern.search(cleaned_line):
                    actionable_occurrences.append({
                        "file": filepath,
                        "line_num": i + 1,
                        "content": line.strip()
                    })
    except Exception as e:
        pass

with open(os.path.join(root, 'actionable_occurrences.json'), 'w', encoding='utf-8') as f:
    json.dump(actionable_occurrences, f, indent=2, ensure_ascii=False)

print(f"Found {len(actionable_occurrences)} actionable occurrences.")
