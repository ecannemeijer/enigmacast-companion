"""Minimal fake OpenWebif + stream port, alleen om de Companion-brug op een emulator te testen.

Start:  python fake-openwebif.py [http_port]
Luistert op 0.0.0.0:<http_port> (standaard 8080) en op 8001 voor de streampoort.
"""

from __future__ import annotations

import json
import socket
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

ABOUT = {
    "info": {
        "brand": "Vu+",
        "model": "Uno 4K SE",
        "webifver": "1.7.9",
        "enigmaver": "2024-01-01",
        "imagedistro": "openATV",
        "imagever": "7.6",
        "friendlyimagedistro": "openATV 7.6",
    },
    "service": {
        "name": "NPO 1 HD",
        "ref": "1:0:19:4B6F:2E:19:C00000:0:0:0:",
    },
}

EMPTY_LISTS = ("/api/bouquets", "/api/getservices", "/api/epgmulti", "/api/epgnow", "/api/timerlist", "/api/movielist")


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_GET(self) -> None:  # noqa: N802
        path = self.path.split("?", 1)[0]
        if path == "/api/about":
            self._json(ABOUT)
            return
        if path == "/api/statusinfo":
            self._json({"instandby": False})
            return
        if path == "/api/getcurrent":
            self._json({"info": {"name": "NPO 1 HD", "ref": ABOUT["service"]["ref"]}})
            return
        if path in EMPTY_LISTS:
            self._json([])
            return
        if path.startswith("/api/"):
            self._json({"result": True})
            return
        self.send_response(404)
        self.send_header("Content-Length", "0")
        self.end_headers()

    def _json(self, value) -> None:
        body = json.dumps(value).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *args) -> None:  # noqa: D401
        return


def stream_port() -> None:
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(("0.0.0.0", 8001))
    server.listen(8)
    while True:
        client, _ = server.accept()
        threading.Thread(target=_hold, args=(client,), daemon=True).start()


def _hold(client: socket.socket) -> None:
    try:
        time.sleep(20)
    finally:
        client.close()


def main() -> None:
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    threading.Thread(target=stream_port, daemon=True).start()
    httpd = ThreadingHTTPServer(("0.0.0.0", port), Handler)
    print(f"fake OpenWebif on 0.0.0.0:{port}, stream on 8001", flush=True)
    httpd.serve_forever()


if __name__ == "__main__":
    main()
