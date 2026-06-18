const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const frontend = path.join(root, 'frontend');
const htmlFiles = ['index.html', 'share.html', 'termos.html', 'privacidade.html'];
const requiredFiles = [
  'assets/logo.png',
  'assets/og-default.png',
  'assets/print1.png',
  'assets/print2.png',
  'assets/print3.png',
  'style.css',
  'app.js',
  'manifest.json',
  '_redirects',
];

const failures = [];

for (const rel of requiredFiles) {
  if (!fs.existsSync(path.join(frontend, rel))) {
    failures.push(`Missing required file: ${rel}`);
  }
}

for (const file of htmlFiles) {
  const abs = path.join(frontend, file);
  const html = fs.readFileSync(abs, 'utf8');

  if (html.includes('logo.webp')) {
    failures.push(`${file} still references logo.webp`);
  }

  if (html.includes('Ver preço')) {
    failures.push(`${file} still contains permanent "Ver preço" copy`);
  }

  const refs = [...html.matchAll(/\b(?:src|href|content)=["']([^"']+)["']/g)]
    .map(match => match[1])
    .filter(ref =>
      ref.startsWith('/') &&
      !ref.startsWith('//') &&
      !ref.startsWith('/api') &&
      !ref.startsWith('/share') &&
      !ref.startsWith('/s')
    );

  for (const ref of refs) {
    const cleanRef = ref.split('#')[0].split('?')[0];
    if (cleanRef === '/') continue;
    const target = path.join(frontend, cleanRef.replace(/^\//, ''));
    if (!fs.existsSync(target)) {
      failures.push(`${file} references missing local file: ${ref}`);
    }
  }
}

const redirects = fs.readFileSync(path.join(frontend, '_redirects'), 'utf8');
for (const route of ['/share/* /share.html 200', '/s/* /share.html 200']) {
  if (!redirects.includes(route)) {
    failures.push(`Missing redirect: ${route}`);
  }
}

if (failures.length) {
  console.error(failures.join('\n'));
  process.exit(1);
}

console.log('Static validation passed.');
