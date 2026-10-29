# Stash website

A compact, dependency-free product website with two sections: an editorial app gallery and a combined privacy/download area. Warm paper tones, large Space Grotesk typography and real app screens replace decorative marketing blocks. Screenshots use fictional sample data.

```sh
npm run dev      # http://127.0.0.1:4173
npm run check    # verify assets, anchors and download links
npm run build    # static output in dist/
```

Requires Node.js 20+. No installation step is needed. Publish the contents of `dist/` to any static host. The app download links point to the production GitHub release. The gallery supports mouse, touch and keyboard navigation. Installation notes use a native disclosure. The page and download links remain usable without JavaScript.

`assets/screenshots/` is the canonical screenshot set, also used by the repository README. `assets/fonts-LICENSE.txt` covers the bundled font.
