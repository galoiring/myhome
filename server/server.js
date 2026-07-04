#!/usr/bin/env node
/*
 * hb-dashboard — tiny local dashboard backend for Homebridge (insecure-mode
 * HAP API). Zero npm dependencies. LAN use only — do not expose to the
 * internet: HAP_PIN below grants full control of every paired accessory.
 */
const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');

const LISTEN_PORT = process.env.PORT || 8090;
const HAP_HOST = process.env.HAP_HOST || '127.0.0.1';
const HAP_PORT = process.env.HAP_PORT || 51705;
// Homebridge's insecure-mode setup PIN — the same one shown in the
// Homebridge UI / config.json. There is no safe default; you must set this.
const HAP_PIN = process.env.HAP_PIN;
const SETTINGS_FILE = path.join(__dirname, 'settings.json');

if (!HAP_PIN) {
  console.error('HAP_PIN environment variable is required (your Homebridge insecure-mode PIN).');
  process.exit(1);
}

// example coordinates — override via WEATHER_LAT/WEATHER_LON for your location
const WEATHER_LAT = process.env.WEATHER_LAT || '31.80';
const WEATHER_LON = process.env.WEATHER_LON || '34.65';
const WEATHER_URL =
  `https://api.open-meteo.com/v1/forecast?latitude=${WEATHER_LAT}&longitude=${WEATHER_LON}` +
  '&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m' +
  '&hourly=temperature_2m,weather_code,precipitation_probability' +
  '&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max' +
  // 2 days of hourly so "next 6 hours" still works late in the evening
  '&timezone=auto&forecast_days=2';
let weatherCache = { ts: 0, body: null };

function hapRequest(method, pathName, body) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null;
    const req = http.request(
      {
        host: HAP_HOST,
        port: HAP_PORT,
        method,
        path: pathName,
        agent: false, // HAP's evented HTTP server hangs on keep-alive connections
        headers: {
          'Content-Type': 'application/json',
          Authorization: HAP_PIN,
          Connection: 'close',
          ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        },
      },
      (res) => {
        let chunks = '';
        res.on('data', (c) => (chunks += c));
        res.on('end', () => resolve({ status: res.statusCode, body: chunks }));
      }
    );
    req.on('error', reject);
    if (data) req.write(data);
    req.end();
  });
}

function sanitizeSettings(s) {
  return {
    names: s.names && typeof s.names === 'object' ? s.names : {},
    groups: Array.isArray(s.groups) ? s.groups : [],
    hidden: Array.isArray(s.hidden) ? s.hidden : [],
    shellies: Array.isArray(s.shellies) ? s.shellies : [],
  };
}

function readSettings() {
  try {
    return sanitizeSettings(JSON.parse(fs.readFileSync(SETTINGS_FILE, 'utf8')));
  } catch {
    return sanitizeSettings({});
  }
}

function shellyRpc(ip, method, params) {
  return new Promise((resolve, reject) => {
    const data = params ? JSON.stringify(params) : null;
    const req = http.request(
      {
        host: ip,
        port: 80,
        method: data ? 'POST' : 'GET',
        path: '/rpc/' + method,
        timeout: 3000,
        headers: data
          ? { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(data) }
          : {},
      },
      (res) => {
        let chunks = '';
        res.on('data', (c) => (chunks += c));
        res.on('end', () => resolve({ status: res.statusCode, body: chunks }));
      }
    );
    req.on('timeout', () => req.destroy(new Error('timeout')));
    req.on('error', reject);
    if (data) req.write(data);
    req.end();
  });
}

function fetchWeather() {
  return new Promise((resolve, reject) => {
    https
      .get(WEATHER_URL, (res) => {
        let b = '';
        res.on('data', (c) => (b += c));
        res.on('end', () => resolve(b));
      })
      .on('error', reject);
  });
}

function writeSettings(s) {
  const tmp = SETTINGS_FILE + '.tmp';
  fs.writeFileSync(tmp, JSON.stringify(s, null, 2));
  fs.renameSync(tmp, SETTINGS_FILE);
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let raw = '';
    req.on('data', (c) => {
      raw += c;
      if (raw.length > 1e6) reject(new Error('body too large'));
    });
    req.on('end', () => resolve(raw));
    req.on('error', reject);
  });
}

function json(res, status, obj) {
  res.writeHead(status, { 'Content-Type': 'application/json', 'Cache-Control': 'no-cache' });
  res.end(typeof obj === 'string' ? obj : JSON.stringify(obj));
}

const server = http.createServer(async (req, res) => {
  try {
    if (req.url === '/api/accessories' && req.method === 'GET') {
      const r = await hapRequest('GET', '/accessories');
      json(res, r.status, r.body);
      return;
    }

    if (req.url === '/api/set' && req.method === 'POST') {
      const raw = await readBody(req);
      const body = JSON.parse(raw);
      // accept either {aid,iid,value} or {characteristics:[{aid,iid,value},...]}
      const chars = Array.isArray(body.characteristics)
        ? body.characteristics
        : [{ aid: body.aid, iid: body.iid, value: body.value }];
      if (!chars.every((c) => typeof c.aid === 'number' && typeof c.iid === 'number')) {
        json(res, 400, { error: 'aid and iid must be numbers' });
        return;
      }
      const r = await hapRequest('PUT', '/characteristics', { characteristics: chars });
      json(res, r.status === 204 ? 200 : r.status, r.body || '{"ok":true}');
      return;
    }

    if (req.url === '/api/weather' && req.method === 'GET') {
      if (!weatherCache.body || Date.now() - weatherCache.ts > 10 * 60 * 1000) {
        try {
          weatherCache = { ts: Date.now(), body: await fetchWeather() };
        } catch (e) {
          if (!weatherCache.body) {
            json(res, 502, { error: 'weather fetch failed: ' + e.message });
            return;
          }
          // keep serving the stale cache if the API is briefly unreachable
        }
      }
      json(res, 200, weatherCache.body);
      return;
    }

    if (req.url === '/api/shelly' && req.method === 'GET') {
      const list = readSettings().shellies;
      const out = await Promise.all(
        list.map(async (dev) => {
          try {
            const r = await shellyRpc(dev.ip, 'Shelly.GetInfoExt');
            const info = JSON.parse(r.body);
            return {
              ip: dev.ip,
              name: info.name,
              components: (info.components || [])
                .filter((c) => c.type === 0) // switches only for now
                .map((c) => ({ id: c.id, type: c.type, name: c.name, state: c.state, apower: c.apower })),
            };
          } catch (e) {
            return { ip: dev.ip, error: String(e.message || e) };
          }
        })
      );
      json(res, 200, out);
      return;
    }

    if (req.url === '/api/shelly/set' && req.method === 'POST') {
      const raw = await readBody(req);
      const { ip, id, type, state } = JSON.parse(raw);
      const allowed = readSettings().shellies.some((d) => d.ip === ip);
      if (!allowed) {
        json(res, 403, { error: 'unknown shelly ip' });
        return;
      }
      const r = await shellyRpc(ip, 'Shelly.SetState', {
        id: Number(id),
        type: Number(type) || 0,
        state: { state: !!state },
      });
      json(res, r.status === 200 ? 200 : r.status, '{"ok":true}');
      return;
    }

    if (req.url === '/api/settings' && req.method === 'GET') {
      json(res, 200, readSettings());
      return;
    }

    if (req.url === '/api/settings' && req.method === 'POST') {
      const raw = await readBody(req);
      const s = JSON.parse(raw);
      if (!s || typeof s !== 'object') {
        json(res, 400, { error: 'settings must be an object' });
        return;
      }
      const clean = sanitizeSettings(s);
      writeSettings(clean);
      json(res, 200, clean);
      return;
    }

    // PWA static assets (manifest, service worker, icons)
    const STATIC = {
      '/manifest.webmanifest': ['application/manifest+json', 'manifest.webmanifest'],
      '/sw.js': ['text/javascript; charset=utf-8', 'sw.js'],
      '/icon-192.png': ['image/png', 'icon-192.png'],
      '/icon-512.png': ['image/png', 'icon-512.png'],
      '/icon-maskable.png': ['image/png', 'icon-maskable.png'],
    };
    if (req.method === 'GET' && STATIC[req.url]) {
      const [type, file] = STATIC[req.url];
      const buf = fs.readFileSync(path.join(__dirname, 'public', file));
      res.writeHead(200, {
        'Content-Type': type,
        'Cache-Control': req.url === '/sw.js' ? 'no-cache' : 'public, max-age=86400',
      });
      res.end(buf);
      return;
    }

    if (req.url === '/' || req.url === '/index.html') {
      const html = fs.readFileSync(path.join(__dirname, 'public', 'index.html'));
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-cache' });
      res.end(html);
      return;
    }

    res.writeHead(404);
    res.end('not found');
  } catch (e) {
    json(res, 500, { error: String(e) });
  }
});

server.listen(LISTEN_PORT, '0.0.0.0', () => {
  console.log(`hb-dashboard listening on http://0.0.0.0:${LISTEN_PORT}`);
});
