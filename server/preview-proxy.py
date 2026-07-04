#!/usr/bin/env python3
"""Local preview for the web dashboard frontend.

Serves server/public/index.html and proxies /api/* (and icons) to a live
hb-dashboard backend (see server.js), so edits render against real device
data without redeploying to the backend host on every change.
"""
import http.server
import os
import socketserver
import urllib.request

UPSTREAM = os.environ.get('UPSTREAM', 'http://192.168.68.75:8090')
HERE = os.path.dirname(os.path.abspath(__file__))
PORT = int(os.environ.get('PORT', 8091))


class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/sw.js':
            # no service worker in preview — avoids stale caches
            self.send_error(404)
            return
        if self.path.startswith('/api/') or self.path.startswith('/icon-') \
                or self.path == '/manifest.webmanifest':
            self.proxy('GET')
            return
        try:
            with open(os.path.join(HERE, 'public', 'index.html'), 'rb') as f:
                body = f.read()
            self.send_response(200)
            self.send_header('Content-Type', 'text/html; charset=utf-8')
            self.send_header('Content-Length', str(len(body)))
            self.send_header('Cache-Control', 'no-store')
            self.end_headers()
            self.wfile.write(body)
        except OSError as e:
            self.send_error(500, str(e))

    def do_POST(self):
        self.proxy('POST')

    def proxy(self, method):
        try:
            body = None
            if method == 'POST':
                length = int(self.headers.get('Content-Length', 0))
                body = self.rfile.read(length)
            req = urllib.request.Request(
                UPSTREAM + self.path, data=body, method=method,
                headers={'Content-Type': self.headers.get('Content-Type', 'application/json')})
            with urllib.request.urlopen(req, timeout=10) as r:
                data = r.read()
                self.send_response(r.status)
                self.send_header('Content-Type', r.headers.get('Content-Type', 'application/json'))
                self.send_header('Content-Length', str(len(data)))
                self.end_headers()
                self.wfile.write(data)
        except Exception as e:
            self.send_error(502, str(e))

    def log_message(self, fmt, *args):
        pass


class Server(http.server.ThreadingHTTPServer):
    # HTTPServer.server_bind resolves the machine's FQDN via reverse DNS,
    # which hangs indefinitely on networks that black-hole those lookups.
    def server_bind(self):
        socketserver.TCPServer.server_bind(self)
        self.server_name = 'localhost'
        self.server_port = PORT


if __name__ == '__main__':
    Server(('0.0.0.0', PORT), Handler).serve_forever()
