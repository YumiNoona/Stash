const views = {
  cards: { index: '01 / YOUR EVERYDAY WALLET', title: 'A card collection\nwith personality.', description: 'Keep bank cards together with a proper landscape preview. Pick your artwork, add a subtle glint, or bring your own image, GIF or video.', bullets: ['Separate bank and partner brand details', 'Custom artwork and optional motion', 'Private details with reveal controls'], caption: 'YOUR WALLET, YOUR WAY', alt: 'Sample payment cards in Stash' },
  documents: { index: '02 / EVERY PAGE, TOGETHER', title: 'A paper trail.\nWithout the pile.', description: 'Scan or import images, PDFs and text. Keep a whole project in one multi-page document, then use folders and tags to find it when it matters.', bullets: ['Multi-page imports and a full-screen viewer', 'On-device text recognition', 'Select, move, share or download files'], caption: 'A LITTLE MORE ORGANIZED', alt: 'Sample documents and folders in Stash' },
  identity: { index: '03 / DETAILS CLOSE AT HAND', title: 'Know where\nthe important bits are.', description: 'Save Aadhaar, PAN, voter ID and driving licence information in familiar front and back layouts. Add your own photo and keep original scans alongside.', bullets: ['Editable names, numbers, addresses and dates', 'Private fields protected by device credentials', 'Available offline, with originals kept separately'], caption: 'PERSONAL WALLET VIEWS', alt: 'A sample identity card stored in Stash' }
};
const tabs = [...document.querySelectorAll('[role="tab"]')];
function selectTab(tab) {
  const data = views[tab.dataset.view];
  tabs.forEach(t => { t.setAttribute('aria-selected', String(t === tab)); t.tabIndex = t === tab ? 0 : -1; });
  document.querySelector('#vault-panel').setAttribute('aria-labelledby', tab.id);
  document.querySelector('#feature-index').textContent = data.index;
  document.querySelector('#feature-title').replaceChildren(...data.title.split('\n').flatMap((line, index) => index ? [document.createElement('br'), document.createTextNode(line)] : [document.createTextNode(line)]));
  document.querySelector('#feature-description').textContent = data.description;
  document.querySelector('#feature-list').replaceChildren(...data.bullets.map(text => { const li = document.createElement('li'); li.textContent = text; return li; }));
  document.querySelector('#visual-caption').textContent = data.caption;
  const image = document.querySelector('#showcase-image');
  image.src = `assets/screenshots/${tab.dataset.view}.webp`;
  image.alt = data.alt;
}
tabs.forEach((tab, index) => {
  tab.addEventListener('click', () => selectTab(tab));
  tab.addEventListener('keydown', event => {
    let target;
    if (event.key === 'ArrowRight') target = tabs[(index + 1) % tabs.length];
    if (event.key === 'ArrowLeft') target = tabs[(index + tabs.length - 1) % tabs.length];
    if (event.key === 'Home') target = tabs[0];
    if (event.key === 'End') target = tabs.at(-1);
    if (target) { event.preventDefault(); selectTab(target); target.focus(); }
  });
});
const menuButton = document.querySelector('.menu-toggle');
const navigation = document.querySelector('#navigation');
function closeMenu() { navigation.classList.remove('open'); menuButton.setAttribute('aria-expanded', 'false'); menuButton.setAttribute('aria-label', 'Open menu'); }
menuButton.addEventListener('click', () => {
  const open = menuButton.getAttribute('aria-expanded') !== 'true';
  navigation.classList.toggle('open', open); menuButton.setAttribute('aria-expanded', String(open)); menuButton.setAttribute('aria-label', open ? 'Close menu' : 'Open menu');
});
navigation.querySelectorAll('a').forEach(link => link.addEventListener('click', closeMenu));
document.addEventListener('keydown', event => { if (event.key === 'Escape' && menuButton.getAttribute('aria-expanded') === 'true') { closeMenu(); menuButton.focus(); } });
