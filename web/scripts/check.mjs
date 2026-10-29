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
assert(html.includes('data-download-latest') && html.includes('/releases/latest'), 'Latest-release download missing');
assert(!html.includes('fonts.googleapis.com'), 'Fonts should remain local');
console.log('Website assets, anchors, screenshot set and production download references verified.');

const { latestApkUrl } = await import('../release.mjs');
const asset = (version) => ({ name: `Stash-${version}.apk`, state: 'uploaded', browser_download_url: `https://github.com/YumiNoona/Stash/releases/download/${version}/Stash-${version}.apk` });
for (const version of ['v1.0.0', 'v2.4.0']) {
  const url = await latestApkUrl(async (endpoint, options) => {
    assert.equal(endpoint, 'https://api.github.com/repos/YumiNoona/Stash/releases/latest');
    assert.equal(options.cache, 'no-store');
    return { ok: true, json: async () => ({ assets: [{ name: 'checksums.sha256' }, asset(version)] }) };
  });
  assert.equal(url, asset(version).browser_download_url);
}
await assert.rejects(() => latestApkUrl(async () => ({ ok: false })));
await assert.rejects(() => latestApkUrl(async () => ({ ok: true, json: async () => ({ assets: [] }) })));
await assert.rejects(() => latestApkUrl(async () => ({ ok: true, json: async () => ({ assets: [{ name: 'Stash.apk', browser_download_url: 'https://example.com/fake.apk' }] }) })));
console.log('Fresh latest-release resolution, future versions and failure handling verified.');
