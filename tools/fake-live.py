"""Fake OpenWebif + WAV-stream voor de Live-tab van de Companion-app.

Start: python fake-live.py
HTTP: 0.0.0.0:8090, stream: 0.0.0.0:8081
"""

from __future__ import annotations

import base64
import json
import math
import os
import struct
import threading
import time
import urllib.parse
import wave
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

BOUQUET = '1:7:1:0:0:0:0:0:0:0:FROM BOUQUET "userbouquet.favourites.tv" ORDER BY bouquet'
CHANNELS = [
    ("NPO 1", "1:0:19:4B6F:2E:19:C00000:0:0:0:", 1),
    ("NPO 2", "1:0:19:4B70:2E:19:C00000:0:0:0:", 2),
    ("RTL 4", "1:0:19:4B71:2E:19:C00000:0:0:0:", 3),
]

WAV = bytearray()
VIDEO = bytearray()
VIDEO_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "test-video.mp4")


def build_video() -> None:
    try:
        with open(VIDEO_PATH, "rb") as f:
            VIDEO.extend(f.read())
    except OSError:
        pass


def build_wav() -> None:
    rate = 44_100
    seconds = 60
    with wave.open("/tmp/fake-live.wav", "wb") as out:
        out.setnchannels(2)
        out.setsampwidth(2)
        out.setframerate(rate)
        frames = bytearray()
        for i in range(rate * seconds):
            value = int(12_000 * math.sin(2 * math.pi * 440 * i / rate))
            frames += struct.pack("<hh", value, value)
        out.writeframes(frames)
    with open("/tmp/fake-live.wav", "rb") as f:
        WAV.extend(f.read())


def now() -> int:
    return int(time.time())


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_GET(self) -> None:  # noqa: N802
        path = self.path.split("?", 1)[0]
        if path == "/api/bouquets":
            self._json({"bouquets": [{"servicename": "TV", "servicereference": BOUQUET}]})
            return
        if path == "/api/getservices":
            self._json(
                {
                    "services": [
                        {"servicename": name, "servicereference": ref, "pos": pos}
                        for name, ref, pos in CHANNELS
                    ]
                }
            )
            return
        if path == "/api/epgnow":
            start = now() - 300
            self._json(
                {
                    "events": [
                        {
                            "sref": ref,
                            "begin_timestamp": start,
                            "duration_sec": 3600,
                            "title": f"Programma op {name}",
                            "id": 1,
                        }
                        for name, ref, _ in CHANNELS
                    ]
                }
            )
            return
        if path == "/api/about":
            self._json({"info": {"brand": "Vu+", "model": "Uno 4K SE", "webifver": "1.7.9"}})
            return
        self.send_response(404)
        self.send_header("Content-Length", "0")
        self.end_headers()

    def _json(self, value) -> None:
        body = json.dumps(value).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *args) -> None:
        return


class StreamHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_GET(self) -> None:  # noqa: N802
        self.send_response(200)
        self.send_header("Content-Type", "audio/wav")
        self.send_header("Content-Length", str(len(WAV)))
        self.send_header("Accept-Ranges", "bytes")
        self.end_headers()
        self.wfile.write(WAV)

    def log_message(self, *args) -> None:
        return


class PlaylistHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_GET(self) -> None:  # noqa: N802
        if not self.path.startswith("/playlist.m3u"):
            self.send_response(404)
            self.send_header("Content-Length", "0")
            self.end_headers()
            return
        body = (
            "#EXTM3U\n"
            '#EXTINF:-1 tvg-name="NPO 1" group-title="Nederland",NPO 1\n'
            "http://127.0.0.1:8081/iptv-npo1\n"
            '#EXTINF:-1 tvg-name="NPO 2" group-title="Nederland",NPO 2\n'
            "http://127.0.0.1:8081/iptv-npo2\n"
            '#EXTINF:-1 tvg-name="NOS" group-title="Nieuws",NOS Journaal\n'
            "http://127.0.0.1:8081/iptv-nos\n"
        ).encode()
        self.send_response(200)
        self.send_header("Content-Type", "audio/x-mpegurl")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *args) -> None:
        return


class XtreamHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_GET(self) -> None:  # noqa: N802
        parsed = urllib.parse.urlparse(self.path)
        if parsed.path.endswith("/202.mp4") and VIDEO:
            self.send_response(200)
            self.send_header("Content-Type", "video/mp4")
            self.send_header("Content-Length", str(len(VIDEO)))
            self.end_headers()
            self.wfile.write(VIDEO)
            return
        if parsed.path.startswith(("/live/", "/movie/", "/series/", "/timeshift/")):
            self.send_response(200)
            self.send_header("Content-Type", "audio/wav")
            self.send_header("Content-Length", str(len(WAV)))
            self.end_headers()
            self.wfile.write(WAV)
            return
        if parsed.path != "/player_api.php":
            self.send_response(404)
            self.send_header("Content-Length", "0")
            self.end_headers()
            return
        action = urllib.parse.parse_qs(parsed.query).get("action", [""])[0]
        if action == "get_live_categories":
            payload = [{"category_id": "1", "category_name": "Nederland"}]
        elif action == "get_live_streams":
            payload = [
                {"stream_id": "101", "name": "NPO 1", "category_id": "1", "num": "1",
                 "tv_archive": "1", "tv_archive_duration": "7"},
                {"stream_id": "102", "name": "NPO 2", "category_id": "1", "num": "2"},
            ]
        elif action == "get_vod_categories":
            payload = [{"category_id": "10", "category_name": "Films"}]
        elif action == "get_vod_streams":
            payload = [
                {"stream_id": "201", "name": "Testfilm", "category_id": "10", "container_extension": "mp4"},
                {"stream_id": "202", "name": "Testvideo", "category_id": "10", "container_extension": "mp4"},
            ]
        elif action == "get_series_categories":
            payload = [{"category_id": "20", "category_name": "Series"}]
        elif action == "get_series":
            payload = [
                {"series_id": "301", "name": "Testserie", "series_name": "Testserie", "category_id": "20"},
            ]
        elif action == "get_short_epg":
            now = int(time.time())
            listings = [
                (now - 7200, now - 3600, "NOS Journaal"),
                (now - 3600, now - 1800, "Nieuwsuur"),
                (now - 1800, now - 600, "Documentaire"),
            ]
            payload = {
                "epg_listings": [
                    {
                        "start_timestamp": str(start),
                        "stop_timestamp": str(end),
                        "title": base64.b64encode(title.encode()).decode(),
                        "description": base64.b64encode(b"Testprogramma").decode(),
                    }
                    for start, end, title in listings
                ]
            }
        elif action == "get_series_info":
            payload = {
                "episodes": {
                    "1": [
                        {
                            "id": "401",
                            "season": "1",
                            "episode_num": "1",
                            "title": "Pilot",
                            "container_extension": "mp4",
                        }
                    ]
                }
            }
        else:
            payload = {"user_info": {"auth": 1, "username": "demo"}}
        body = json.dumps(payload).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *args) -> None:
        return


def main() -> None:
    build_wav()
    build_video()
    threading.Thread(
        target=lambda: ThreadingHTTPServer(("0.0.0.0", 8081), StreamHandler).serve_forever(),
        daemon=True,
    ).start()
    threading.Thread(
        target=lambda: ThreadingHTTPServer(("0.0.0.0", 8092), PlaylistHandler).serve_forever(),
        daemon=True,
    ).start()
    threading.Thread(
        target=lambda: ThreadingHTTPServer(("0.0.0.0", 8093), XtreamHandler).serve_forever(),
        daemon=True,
    ).start()
    print("fake live on 8090, stream on 8081, playlist on 8092, xtream on 8093", flush=True)
    ThreadingHTTPServer(("0.0.0.0", 8090), Handler).serve_forever()


if __name__ == "__main__":
    main()
