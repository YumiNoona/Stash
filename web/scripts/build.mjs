import { mkdir, copyFile, cp } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
const root = fileURLToPath(new URL('../', import.meta.url));
await mkdir(`${root}dist`, { recursive: true });
for (const name of ['index.html', 'styles.css', 'app.js', 'release.mjs']) await copyFile(root + name, `${root}dist/${name}`);
await cp(`${root}assets`, `${root}dist/assets`, { recursive: true });
console.log('Built static website in web/dist. No dependencies or remote fonts required.');
