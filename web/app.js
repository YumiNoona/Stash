const views = {
  cards: { index: 'THE WALLET / 01', title: 'A home for\nevery card.', description: 'Keep bank and partner details together. Choose artwork you like, add a little motion, or use your own.', primary: 'cards', secondary: 'dark', primaryAlt: 'Two sample bank cards in Stash', secondaryAlt: 'Stash home in dark mode', label: 'LIGHT / DARK', counter: '01 — 03' },
  documents: { index: 'THE FILES / 02', title: 'Every page.\nIn its place.', description: 'Scan or import a document. Keep its pages together, sort it into a folder, and find it without the paper pile.', primary: 'documents', secondary: 'viewer', primaryAlt: 'Sample documents organized in Stash', secondaryAlt: 'A two-page sample document in the full-screen viewer', label: 'COLLECTION / READER', counter: '02 — 03' },
  identity: { index: 'THE DETAILS / 03', title: 'The details\nyou reach for.', description: 'Keep ID details in familiar front and back layouts, with your photo and originals alongside. A personal record, available offline.', primary: 'identity', secondary: 'home', primaryAlt: 'Fictional PAN identity details in Stash', secondaryAlt: 'The Stash home screen with fictional sample records', label: 'IDENTITY / HOME', counter: '03 — 03' }
};
const tabs = [...document.querySelectorAll('[role="tab"]')];
function selectTab(tab) {
  const view = views[tab.dataset.view];
  tabs.forEach(t => { t.setAttribute('aria-selected', String(t === tab)); t.tabIndex = t === tab ? 0 : -1; });
  document.querySelector('#screen-panel').setAttribute('aria-labelledby', tab.id);
  document.querySelector('#screen-index').textContent = view.index;
  document.querySelector('#screen-title').replaceChildren(...view.title.split('\n').flatMap((line, i) => i ? [document.createElement('br'), document.createTextNode(' ' + line)] : [document.createTextNode(line)]));
  document.querySelector('#screen-description').textContent = view.description;
  document.querySelector('#screen-counter').textContent = view.counter;
  document.querySelector('#stage-label').textContent = view.label;
  for (const [id, key] of [['main-screen', 'primary'], ['secondary-screen', 'secondary']]) {
    const img = document.querySelector('#' + id);
    img.src = `assets/screenshots/${view[key]}.webp`;
    img.alt = view[key + 'Alt'];
  }
}
tabs.forEach((tab, i) => {
  tab.addEventListener('click', () => selectTab(tab));
  tab.addEventListener('keydown', event => {
    const next = event.key === 'ArrowRight' ? tabs[(i + 1) % tabs.length] : event.key === 'ArrowLeft' ? tabs[(i + tabs.length - 1) % tabs.length] : event.key === 'Home' ? tabs[0] : event.key === 'End' ? tabs.at(-1) : null;
    if (next) { event.preventDefault(); selectTab(next); next.focus(); }
  });
});
