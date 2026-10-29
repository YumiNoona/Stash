export const LATEST_RELEASE_PAGE = 'https://github.com/YumiNoona/Stash/releases/latest';
const RELEASE_API = 'https://api.github.com/repos/YumiNoona/Stash/releases/latest';

/** Resolve at click time, including future versioned APK filenames. */
export async function latestApkUrl(fetchRelease = globalThis.fetch) {
  const response = await fetchRelease(RELEASE_API, {
    headers: { Accept: 'application/vnd.github+json' },
    cache: 'no-store',
    signal: AbortSignal.timeout(10000)
  });
  if (!response.ok) throw new Error('GitHub release lookup failed');
  const release = await response.json();
  const apks = (release.assets || []).filter(asset => /\.apk$/i.test(asset.name) && (!asset.state || asset.state === 'uploaded'));
  const apk = apks.find(asset => /^stash\.apk$/i.test(asset.name)) || apks.find(asset => /universal/i.test(asset.name)) || apks[0];
  if (!apk) throw new Error('No APK in the latest release');
  const url = new URL(apk.browser_download_url);
  if (url.origin !== 'https://github.com' || !url.pathname.startsWith('/YumiNoona/Stash/releases/download/')) {
    throw new Error('Unexpected release download URL');
  }
  return url.href;
}
