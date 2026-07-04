/* hb-dashboard service worker — enables install (PWA) + offline app shell. */
const CACHE = 'hb-dash-v1';
const SHELL = ['/', '/manifest.webmanifest', '/icon-192.png', '/icon-512.png'];

self.addEventListener('install', (e) => {
  e.waitUntil(
    caches.open(CACHE).then((c) => c.addAll(SHELL)).then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (e) => {
  if (e.request.method !== 'GET') return; // never intercept control POSTs
  const url = new URL(e.request.url);

  // Live data: network-only, graceful offline stub.
  if (url.pathname.startsWith('/api/')) {
    e.respondWith(
      fetch(e.request).catch(
        () => new Response('{"error":"offline"}', { status: 503, headers: { 'Content-Type': 'application/json' } })
      )
    );
    return;
  }

  // Page navigations: network-first (always get latest UI), fall back to cache offline.
  if (e.request.mode === 'navigate') {
    e.respondWith(
      fetch(e.request)
        .then((r) => { caches.open(CACHE).then((c) => c.put('/', r.clone())); return r; })
        .catch(() => caches.match('/'))
    );
    return;
  }

  // Static assets (icons, manifest): cache-first.
  e.respondWith(
    caches.match(e.request).then(
      (cached) => cached || fetch(e.request).then((r) => {
        if (r.ok) caches.open(CACHE).then((c) => c.put(e.request, r.clone()));
        return r;
      })
    )
  );
});
