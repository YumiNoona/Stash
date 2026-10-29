import { readFile, access } from 'node:fs/promises';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';
const root = fileURLToPath(new URL('../', import.meta.url));
const html = await readFile(root + 'index.html', 'utf8');
const css = await readFile(root + 'styles.css', 'utf8');
for (const [, file] of [...html.matchAll(/(?:src|href)="([^"#]+)"/g), ...css.matchAll(/url\('([^']+)'\)/g)]) {
  if (!/^(https?:|mailto:)/.test(file)) await access(resolve(root, file));
}
for (const name of ['cards', 'documents', 'identity', 'home', 'viewer', 'dark']) await access(root + `assets/screenshots/${name}.webp`);
const ids = [...html.matchAll(/\bid="([^"]+)"/g)].map(match => match[1]);
assert.equal(new Set(ids).size, ids.length, 'Duplicate HTML IDs');
for (const [, anchor] of html.matchAll(/href="#([^"]+)"/g)) assert(ids.includes(anchor), `Missing anchor ${anchor}`);
assert(html.includes('Stash-v1.0.0.apk'), 'Production download missing');
assert(!html.includes('fonts.googleapis.com'), 'Fonts should remain local');
console.log('Website assets, anchors, screenshot set and production download references verified.');
