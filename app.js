/* ============ putar · music player ============ */
'use strict';

/* ---------- helpers ---------- */
const $  = (s) => document.querySelector(s);
const $$ = (s) => Array.from(document.querySelectorAll(s));
const el = (tag, cls, text) => {
  const n = document.createElement(tag);
  if (cls) n.className = cls;
  if (text != null) n.textContent = text;
  return n;
};
const uid = () => 't' + Date.now().toString(36) + Math.random().toString(36).slice(2, 8);

function fmtTime(t) {
  if (!isFinite(t) || t < 0) return '0:00';
  const s = Math.floor(t);
  const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), sec = s % 60;
  const mm = h ? String(m).padStart(2, '0') : m;
  return (h ? h + ':' : '') + mm + ':' + String(sec).padStart(2, '0');
}
function hashHue(str) {
  let h = 0;
  for (let i = 0; i < str.length; i++) h = (h * 31 + str.charCodeAt(i)) >>> 0;
  return h % 360;
}
function hostOf(url) {
  try { return new URL(url).hostname.replace(/^www\./, ''); }
  catch { return 'stream'; }
}
const AUDIO_EXT = /\.(mp3|ogg|oga|wav|m4a|aac|flac|opus|webm|weba)$/i;

const STORE_KEY = 'putar.playlist.v1';
const DEMOS = [
  { title: 'SoundHelix — Song 1 (demo)',  url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3' },
  { title: 'SoundHelix — Song 2 (demo)',  url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3' },
  { title: 'SoundHelix — Song 9 (demo)',  url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3' }
];

/* ---------- state ---------- */
const state = {
  tracks: [],      // {id,title,url,kind:'file'|'url',dur?}
  index: -1,
  shuffle: false,
  repeat: 'off',   // 'off' | 'all' | 'one'
  volume: 0.85,
  muted: false
};
let toastTimer = 0;

const audio = new Audio();
audio.preload = 'metadata';

/* equalizer */
let eqCtx = null, eqAnalyser = null, eqSource = null, eqOk = false, corsFallback = false, loadedId = null;
const eqCanvas = $('#eq');
const eqCv = eqCanvas.getContext('2d');

/* ---------- dom refs ---------- */
const dom = {
  body: document.body,
  kicker: $('#kicker'), trackTitle: $('#trackTitle'),
  monogram: $('#monogram'), discLabel: $('#discLabel'),
  curT: $('#curT'), durT: $('#durT'), seek: $('#seek'),
  queue: $('#queue'), queueEmpty: $('#queueEmpty'), countChip: $('#countChip'),
  btnPlay: $('#btnPlay'), btnPrev: $('#btnPrev'), btnNext: $('#btnNext'),
  btnShuffle: $('#btnShuffle'), btnRepeat: $('#btnRepeat'),
  btnMute: $('#btnMute'), vol: $('#vol'),
  btnFiles: $('#btnFiles'), fileInput: $('#fileInput'), btnUrl: $('#btnUrl'),
  modal: $('#modal'), urlInput: $('#urlInput'), urlForm: $('#urlForm'),
  btnCancel: $('#btnCancel'), modalClose: $('#modalClose'),
  emptyDemos: $('#emptyDemos'), modalDemos: $('#modalDemos'),
  dropOverlay: $('#dropOverlay'), toast: $('#toast')
};

/* ---------- toast ---------- */
function toast(msg) {
  dom.toast.textContent = msg;
  dom.toast.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { dom.toast.hidden = true; }, 3400);
}

/* ============================================================
   playlist ops
   ============================================================ */
function current() { return state.tracks[state.index] || null; }

function setCurrent(i, { load = false, play = false } = {}) {
  state.index = i;
  const t = current();
  if (!t) {
    dom.kicker.textContent = '—';
    dom.trackTitle.textContent = 'Belum ada lagu';
    dom.monogram.textContent = '?';
    dom.discLabel.style.background = '';
    dom.curT.textContent = '0:00';
    dom.durT.textContent = '0:00';
    dom.seek.value = 0; dom.seek.disabled = true;
    setSeekFill(0);
    dom.body.classList.remove('has-track');
    return;
  }
  dom.kicker.textContent = t.kind === 'file' ? 'File lokal' : 'Stream · ' + hostOf(t.url);
  dom.trackTitle.textContent = t.title;
  dom.trackTitle.title = t.title;
  const initials = t.title.split(/\s+/).filter(Boolean).slice(0, 2).map(w => w[0].toUpperCase()).join('') || '♪';
  dom.monogram.textContent = initials;
  dom.discLabel.style.background = 'hsl(' + hashHue(t.title) + ' 72% 58%)';
  dom.curT.textContent = '0:00';
  dom.durT.textContent = t.dur ? fmtTime(t.dur) : '0:00';
  dom.seek.value = 0; setSeekFill(0);
  dom.body.classList.add('has-track');
  if (load) loadCurrent();
  if (play) playCurrent();
  renderQueue();
  setMediaSession(t);
  save();
}

function loadCurrent() {
  const t = current();
  if (!t) return;
  audio.pause();
  corsFallback = false;
  // batal pemuatan lama bila user ganti lagu cepat
  if (audio.src) { audio.removeAttribute('src'); audio.load(); }
  audio.crossOrigin = (t.kind === 'url' && /^https?:/i.test(t.url)) ? 'anonymous' : null;
  audio.src = t.url;
  loadedId = t.id;
}

function playCurrent() {
  const t = current();
  if (!t) return;
  if (loadedId !== t.id) loadCurrent();
  const p = audio.play();
  if (p && p.catch) p.catch(() => toast('Tidak bisa memutar — cek URL / koneksi, lalu coba lagi.'));
}

function playIndex(i) {
  if (i < 0 || i >= state.tracks.length) return;
  if (i === state.index && audio.src) { playCurrent(); return; }
  setCurrent(i, { load: true, play: true });
}

function removeTrack(i) {
  const wasCurrent = i === state.index;
  const wasPlaying = wasCurrent && !audio.paused;
  state.tracks.splice(i, 1);
  if (wasCurrent) {
    if (audio.src) { audio.pause(); audio.removeAttribute('src'); audio.load(); }
    if (state.tracks.length === 0) {
      setCurrent(-1);
    } else {
      const next = Math.min(i, state.tracks.length - 1);
      if (wasPlaying) playIndex(next); else setCurrent(next);
    }
  } else if (i < state.index) {
    state.index--;
  }
  renderQueue();
  save();
}

/* ---------- add sources ---------- */
function addUrlTrack(title, url) {
  if (!/^https?:\/\//i.test(url)) { toast('URL harus diawali http(s):// dan mengarah ke file audio.'); return false; }
  state.tracks.push({ id: uid(), title: title || url.split('/').pop().split('?')[0] || url, url, kind: 'url' });
  if (state.index === -1) setCurrent(0);
  renderQueue(); save();
  return true;
}

function addFiles(fileList) {
  const files = Array.from(fileList).filter(f => f.type.startsWith('audio/') || AUDIO_EXT.test(f.name));
  if (!files.length) { toast('Tidak ada file audio yang dikenali.'); return; }
  for (const f of files) {
    state.tracks.push({
      id: uid(),
      title: f.name.replace(/\.[^.]+$/, ''),
      url: URL.createObjectURL(f),
      kind: 'file'
    });
  }
  if (state.index === -1) setCurrent(0);
  renderQueue();
  toast(files.length + ' lagu ditambahkan dari perangkatmu.');
}

/* ---------- render queue ---------- */
function renderQueue() {
  dom.queue.textContent = '';
  const n = state.tracks.length;
  dom.countChip.textContent = n + ' lagu';
  dom.queueEmpty.hidden = n > 0;
  dom.queue.hidden = n === 0;

  state.tracks.forEach((t, i) => {
    const li = el('li', 'track' + (i === state.index ? ' active' : ''));
    li.dataset.i = i;

    const main = el('button', 'row-main');
    main.type = 'button';
    main.setAttribute('aria-label', 'Putar ' + t.title);

    const idx = el('span', 'col-idx');
    const num = el('span', 'num', String(i + 1).padStart(2, '0'));
    const mini = el('span', 'eq-mini');
    for (let k = 0; k < 4; k++) mini.appendChild(el('i'));
    idx.appendChild(num); idx.appendChild(mini);

    const col = el('span', 'col-main');
    col.appendChild(el('span', 't-title', t.title));
    col.appendChild(el('span', 't-sub', t.kind === 'file' ? 'File lokal · sesi ini' : hostOf(t.url) + ' · stream'));

    const dur = el('span', 't-dur', t.dur != null ? fmtTime(t.dur) : '—');
    main.appendChild(idx); main.appendChild(col); main.appendChild(dur);

    const rm = el('button', 'rm');
    rm.type = 'button';
    rm.setAttribute('aria-label', 'Hapus ' + t.title);
    rm.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/><path d="M10 11v6M14 11v6"/></svg>';

    li.appendChild(main); li.appendChild(rm);
    dom.queue.appendChild(li);
  });
}

/* ---------- demo chips ---------- */
function demoChip(demo) {
  const b = el('button', 'demo-chip', demo.title.replace(' (demo)', '').replace('SoundHelix — ', ''));
  b.type = 'button';
  b.addEventListener('click', () => {
    if (addUrlTrack(demo.title, demo.url)) toast('Demo ditambahkan ke playlist.');
  });
  return b;
}

/* ============================================================
   seek / volume UI
   ============================================================ */
function setSeekFill(pct) {
  dom.seek.style.setProperty('--p', pct + '%');
}
function updateSeekUI() {
  const t = current();
  if (!t || !audio.duration || !isFinite(audio.duration)) return;
  const p = (audio.currentTime / audio.duration) * 100;
  dom.seek.value = Math.round(p * 10); // 0..1000
  setSeekFill(p);
  dom.curT.textContent = fmtTime(audio.currentTime);
}
function applyVolumeUI() {
  dom.vol.value = state.volume;
  dom.body.classList.toggle('is-muted', state.muted);
  dom.btnMute.title = state.muted ? 'Nyalakan suara (M)' : 'Bisu (M)';
}

/* ============================================================
   audio events
   ============================================================ */
audio.addEventListener('loadedmetadata', () => {
  const t = current();
  if (!t) return;
  t.dur = audio.duration;
  dom.durT.textContent = fmtTime(audio.duration);
  const lis = $$('.track');
  if (lis[state.index]) {
    const d = lis[state.index].querySelector('.t-dur');
    if (d) d.textContent = fmtTime(audio.duration);
  }
  dom.seek.disabled = false;
  if (eqAnalyser) eqOk = !corsFallback;
});
audio.addEventListener('timeupdate', updateSeekUI);
audio.addEventListener('durationchange', () => {
  const t = current();
  if (t && audio.duration && isFinite(audio.duration)) { t.dur = audio.duration; dom.durT.textContent = fmtTime(audio.duration); }
});
audio.addEventListener('play', () => {
  dom.body.classList.add('is-playing');
  if (eqCtx && eqCtx.state === 'suspended') eqCtx.resume().catch(() => {});
});
audio.addEventListener('pause', () => dom.body.classList.remove('is-playing'));
audio.addEventListener('ended', () => {
  if (state.repeat === 'one') {
    audio.currentTime = 0;
    playCurrent();
    return;
  }
  const n = state.tracks.length;
  if (n === 0) return;
  if (state.shuffle) {
    let i;
    do { i = Math.floor(Math.random() * n); } while (n > 1 && i === state.index);
    playIndex(i);
    return;
  }
  if (state.index < n - 1) { playIndex(state.index + 1); return; }
  if (state.repeat === 'all') { playIndex(0); return; }
  // repeat off & lagu terakhir selesai: berhenti rapi
  dom.body.classList.remove('is-playing');
  audio.currentTime = 0;
});
audio.addEventListener('error', () => {
  const t = current();
  if (!t) return;
  if (audio.crossOrigin && t.kind === 'url') {
    // coba sekali lagi tanpa crossOrigin (server tanpa CORS) — visualizer mati, audio jalan
    corsFallback = true;
    audio.crossOrigin = null;
    audio.load();
    return;
  }
  dom.body.classList.remove('is-playing');
  toast('Gagal memuat "' + t.title + '". Cek URL-nya atau koneksimu.');
});

/* seek bar */
let scrubbing = false;
dom.seek.addEventListener('input', () => {
  scrubbing = true;
  const t = current();
  const p = Number(dom.seek.value) / 10;
  setSeekFill(p);
  if (t && audio.duration && isFinite(audio.duration)) dom.curT.textContent = fmtTime((p / 100) * audio.duration);
});
dom.seek.addEventListener('change', () => {
  scrubbing = false;
  if (!current() || !audio.duration) return;
  audio.currentTime = (Number(dom.seek.value) / 1000) * audio.duration;
});

/* volume */
dom.vol.addEventListener('input', () => {
  state.volume = Number(dom.vol.value);
  state.muted = state.volume === 0;
  audio.volume = state.volume;
  audio.muted = state.muted;
  applyVolumeUI(); save();
});
dom.btnMute.addEventListener('click', () => {
  state.muted = !state.muted;
  audio.muted = state.muted;
  applyVolumeUI(); save();
});

/* ============================================================
   transport controls
   ============================================================ */
dom.btnPlay.addEventListener('click', () => {
  if (!current()) { if (state.tracks.length) playIndex(0); return; }
  if (audio.paused) { ensureEQ(); playCurrent(); }
  else audio.pause();
});
dom.btnNext.addEventListener('click', () => {
  const n = state.tracks.length;
  if (!n) return;
  if (state.shuffle && n > 1) {
    let i; do { i = Math.floor(Math.random() * n); } while (i === state.index);
    playIndex(i);
  } else playIndex((state.index + 1) % n);
});
dom.btnPrev.addEventListener('click', () => {
  const n = state.tracks.length;
  if (!n) return;
  if (audio.currentTime > 3 || state.index <= 0) { audio.currentTime = 0; return; }
  playIndex(state.index - 1);
});
dom.btnShuffle.addEventListener('click', () => {
  state.shuffle = !state.shuffle;
  dom.btnShuffle.classList.toggle('on', state.shuffle);
  dom.btnShuffle.title = state.shuffle ? 'Acak aktif (S)' : 'Acak (S)';
  save();
});
dom.btnRepeat.addEventListener('click', () => {
  state.repeat = state.repeat === 'off' ? 'all' : state.repeat === 'all' ? 'one' : 'off';
  dom.btnRepeat.classList.toggle('on', state.repeat !== 'off');
  dom.btnRepeat.dataset.mode = state.repeat;
  dom.btnRepeat.title = state.repeat === 'off' ? 'Ulangi (R)' : state.repeat === 'all' ? 'Ulangi semua (R)' : 'Ulangi satu lagu (R)';
  save();
});

/* queue delegation */
dom.queue.addEventListener('click', (e) => {
  const rm = e.target.closest('.rm');
  if (rm) { removeTrack(Number(rm.closest('.track').dataset.i)); return; }
  const main = e.target.closest('.row-main');
  if (main) {
    const i = Number(main.closest('.track').dataset.i);
    if (i === state.index && !audio.paused) { audio.pause(); return; }
    ensureEQ();
    playIndex(i);
  }
});

/* ============================================================
   equalizer (Web Audio analyser)
   ============================================================ */
function ensureEQ() {
  if (eqCtx || !window.AudioContext) return;
  try {
    const Ctx = window.AudioContext || window.webkitAudioContext;
    eqCtx = new Ctx();
    eqSource = eqCtx.createMediaElementSource(audio);
    eqAnalyser = eqCtx.createAnalyser();
    eqAnalyser.fftSize = 128;
    eqAnalyser.smoothingTimeConstant = 0.8;
    eqSource.connect(eqAnalyser);
    eqAnalyser.connect(eqCtx.destination);
    eqOk = !corsFallback && audio.readyState >= 1;
  } catch { eqCtx = null; }
}

function sizeEQ() {
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  eqCanvas.width = Math.max(2, Math.floor(eqCanvas.clientWidth * dpr));
  eqCanvas.height = Math.max(2, Math.floor(eqCanvas.clientHeight * dpr));
}
function drawEQ() {
  requestAnimationFrame(drawEQ);
  const w = eqCanvas.width, h = eqCanvas.height;
  if (!w || !h) { sizeEQ(); return; }
  eqCv.clearRect(0, 0, w, h);
  const bars = 48, bw = (w / bars) * 0.55, gap = (w / bars) * 0.45;
  eqCv.fillStyle = 'rgba(255,90,54,.85)';
  const data = new Uint8Array(eqAnalyser ? eqAnalyser.frequencyBinCount : 0);
  const haveSignal = !!(eqAnalyser && eqOk && !audio.paused);
  if (eqAnalyser && eqOk && !audio.paused) eqAnalyser.getByteFrequencyData(data);
  for (let i = 0; i < bars; i++) {
    const v = haveSignal ? data[Math.floor((i / bars) * data.length * 0.8)] : 0;
    const bh = Math.max(1.5, (v / 255) * h * 0.96);
    const x = i * (w / bars) + gap / 2;
    if (haveSignal) eqCv.fillRect(x, h - bh, bw, bh);
    else { eqCv.fillStyle = 'rgba(255,255,255,.05)'; eqCv.fillRect(x, h - 1.5, bw, 1.5); eqCv.fillStyle = 'rgba(255,90,54,.85)'; }
  }
}

/* ============================================================
   add dialogs: files & URL
   ============================================================ */
dom.btnFiles.addEventListener('click', () => dom.fileInput.click());
dom.fileInput.addEventListener('change', () => {
  if (dom.fileInput.files.length) addFiles(dom.fileInput.files);
  dom.fileInput.value = '';
});

function openModal() {
  dom.modal.hidden = false;
  setTimeout(() => dom.urlInput.focus(), 30);
}
function closeModal() { dom.modal.hidden = true; dom.urlForm.reset(); }
dom.btnUrl.addEventListener('click', openModal);
dom.btnCancel.addEventListener('click', closeModal);
dom.modalClose.addEventListener('click', closeModal);
dom.modal.addEventListener('click', (e) => { if (e.target === dom.modal) closeModal(); });
dom.urlForm.addEventListener('submit', (e) => {
  e.preventDefault();
  const url = dom.urlInput.value.trim();
  if (addUrlTrack(null, url)) { closeModal(); toast('Lagu ditambahkan ke playlist.'); }
});

/* ---------- drag & drop ---------- */
let dragDepth = 0;
window.addEventListener('dragenter', (e) => {
  if (!e.dataTransfer) return;
  e.preventDefault();
  dragDepth++;
  dom.dropOverlay.hidden = false;
});
window.addEventListener('dragleave', () => {
  dragDepth = Math.max(0, dragDepth - 1);
  if (dragDepth === 0) dom.dropOverlay.hidden = true;
});
window.addEventListener('dragover', (e) => e.preventDefault());
window.addEventListener('drop', (e) => {
  e.preventDefault();
  dragDepth = 0;
  dom.dropOverlay.hidden = true;
  if (e.dataTransfer && e.dataTransfer.files.length) addFiles(e.dataTransfer.files);
});

/* ============================================================
   media session (tombol media OS/browser)
   ============================================================ */
function setMediaSession(t) {
  if (!('mediaSession' in navigator)) return;
  try {
    navigator.mediaSession.metadata = new MediaMetadata({
      title: t.title,
      artist: t.kind === 'file' ? 'File lokal' : hostOf(t.url),
      album: 'putar'
    });
  } catch { /* noop */ }
}
if ('mediaSession' in navigator) {
  try {
    navigator.mediaSession.setActionHandler('play', () => { ensureEQ(); playCurrent(); });
    navigator.mediaSession.setActionHandler('pause', () => audio.pause());
    navigator.mediaSession.setActionHandler('previoustrack', () => {
      if (audio.currentTime > 3) audio.currentTime = 0; else dom.btnPrev.click();
    });
    navigator.mediaSession.setActionHandler('nexttrack', () => dom.btnNext.click());
  } catch { /* noop */ }
}

/* ============================================================
   keyboard
   ============================================================ */
document.addEventListener('keydown', (e) => {
  const tag = (e.target.tagName || '').toUpperCase();
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || tag === 'BUTTON') {
    if (e.key === 'Escape') closeModal();
    return;
  }
  if (e.key === 'Escape') { closeModal(); return; }
  if (!dom.modal.hidden) return;
  switch (e.key) {
    case ' ': case 'Spacebar':
      e.preventDefault();
      dom.btnPlay.click();
      break;
    case 'ArrowRight':
      e.preventDefault();
      if (audio.duration) audio.currentTime = Math.min(audio.duration, audio.currentTime + 5);
      break;
    case 'ArrowLeft':
      e.preventDefault();
      if (audio.duration) audio.currentTime = Math.max(0, audio.currentTime - 5);
      break;
    case 'ArrowUp':
      e.preventDefault();
      state.volume = Math.min(1, Math.round((state.volume + 0.05) * 100) / 100);
      state.muted = false;
      audio.volume = state.volume; audio.muted = false;
      applyVolumeUI(); save();
      break;
    case 'ArrowDown':
      e.preventDefault();
      state.volume = Math.max(0, Math.round((state.volume - 0.05) * 100) / 100);
      if (state.volume === 0) state.muted = true;
      audio.volume = state.volume; audio.muted = state.muted;
      applyVolumeUI(); save();
      break;
    case 'm': case 'M': dom.btnMute.click(); break;
    case 's': case 'S': dom.btnShuffle.click(); break;
    case 'r': case 'R': dom.btnRepeat.click(); break;
    case 'n': case 'N': dom.btnNext.click(); break;
    case 'p': case 'P': dom.btnPrev.click(); break;
  }
});

/* ============================================================
   persistence
   ============================================================ */
function save() {
  try {
    const urlTracks = state.tracks.filter(t => t.kind === 'url').map(t => ({ title: t.title, url: t.url, dur: t.dur != null ? t.dur : null }));
    const cur = current();
    const curIsUrl = cur && cur.kind === 'url';
    const curUrl = curIsUrl ? cur.url : null;
    const data = {
      tracks: urlTracks,
      curUrl,
      shuffle: state.shuffle,
      repeat: state.repeat,
      volume: state.volume,
      muted: state.muted
    };
    localStorage.setItem(STORE_KEY, JSON.stringify(data));
  } catch { /* storage penuh / privat — abaikan */ }
}

function restore() {
  let data = null;
  try { data = JSON.parse(localStorage.getItem(STORE_KEY) || 'null'); } catch { data = null; }
  if (data && Array.isArray(data.tracks) && data.tracks.length) {
    let restored = -1;
    data.tracks.forEach((t) => {
      if (!t.url || !/^https?:\/\//i.test(t.url)) return;
      state.tracks.push({ id: uid(), title: t.title || t.url, url: t.url, kind: 'url', dur: t.dur != null ? t.dur : undefined });
      if (data.curUrl && t.url === data.curUrl) restored = state.tracks.length - 1;
    });
    if (state.tracks.length) setCurrent(restored >= 0 ? restored : 0);
  }
  if (typeof data?.volume === 'number') state.volume = data.volume;
  if (typeof data?.muted === 'boolean') state.muted = data.muted;
  if (data?.shuffle) { state.shuffle = true; dom.btnShuffle.classList.add('on'); dom.btnShuffle.title = 'Acak aktif (S)'; }
  if (data?.repeat && data.repeat !== 'off') {
    state.repeat = data.repeat;
    dom.btnRepeat.classList.add('on');
    dom.btnRepeat.dataset.mode = state.repeat;
  }
  audio.volume = state.volume;
  audio.muted = state.muted;
}

/* ============================================================
   boot
   ============================================================ */
// populate demo chips (both empty-state & modal)
for (const d of DEMOS) {
  dom.emptyDemos.appendChild(demoChip(d));
  dom.modalDemos.appendChild(demoChip(d));
}
applyVolumeUI();
restore();
applyVolumeUI();
renderQueue();
if (state.tracks.length === 0) setCurrent(-1);

sizeEQ();
drawEQ();
window.addEventListener('resize', sizeEQ);
