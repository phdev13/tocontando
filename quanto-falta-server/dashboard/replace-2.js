import fs from 'fs';
import path from 'path';

function walkDir(dir, callback) {
  fs.readdirSync(dir).forEach(f => {
    let dirPath = path.join(dir, f);
    let isDirectory = fs.statSync(dirPath).isDirectory();
    isDirectory ? walkDir(dirPath, callback) : callback(path.join(dir, f));
  });
}

function replaceColors() {
  const dirs = ['app', 'components'];
  const regexes = [
    { from: /bg-\[#1f1e24\]/g, to: 'bg-[var(--color-border)]' },
    { from: /text-\[#5b5a61\]/g, to: 'text-[var(--color-primary-muted)]' },
    { from: /hover:bg-\[#1f1e24\]/g, to: 'hover:bg-[var(--color-border)]' },
    { from: /text-\[#fafafa\]/g, to: 'text-[var(--color-primary)]' },
    { from: /ring-\[#09090b\]/g, to: 'ring-[var(--color-background)]' }
  ];

  dirs.forEach(dir => {
    walkDir(dir, filePath => {
      if (filePath.endsWith('.tsx') || filePath.endsWith('.ts')) {
        let content = fs.readFileSync(filePath, 'utf8');
        let newContent = content;
        regexes.forEach(r => {
          newContent = newContent.replace(r.from, r.to);
        });
        if (content !== newContent) {
          fs.writeFileSync(filePath, newContent);
          console.log(`Updated ${filePath}`);
        }
      }
    });
  });
}

replaceColors();
