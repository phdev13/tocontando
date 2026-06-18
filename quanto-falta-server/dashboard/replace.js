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
    { from: /bg-\[#110f17\]/g, to: 'bg-[var(--color-surface)]' },
    { from: /bg-\[#151419\]/g, to: 'bg-[var(--color-surface-hover)]' },
    { from: /border-\[#1f1e24\]/g, to: 'border-[var(--color-border)]' },
    { from: /border-\[#27272a\]/g, to: 'border-[var(--color-border-hover)]' },
    { from: /text-\[#8b8a91\]/g, to: 'text-[var(--color-primary-muted)]' },
    { from: /text-\[#a1a1aa\]/g, to: 'text-[var(--color-primary-muted)]' },
    { from: /bg-\[#27272a\]/g, to: 'bg-[var(--color-border-hover)]' },
    { from: /bg-\[#0b0a0f\]/g, to: 'bg-[var(--color-background)]' },
    { from: /bg-\[#18181b\]/g, to: 'bg-[var(--color-surface-hover)]' },
    { from: /bg-\[#09090b\]/g, to: 'bg-[var(--color-surface)]' }
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
