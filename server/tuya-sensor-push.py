#!/usr/bin/env python3
"""
Push Tuya temperature/humidity sensors into hb-dashboard.

Why a separate script: server.js has no npm dependencies and is not going to
grow a Tuya protocol implementation for this. tinytuya already lives on the Pi
for the blind, so this reuses that venv and feeds the readings through the
dashboard's existing /api/push-sensor endpoint — which means the tile, the
comfort band and the 24 h graph all come free, exactly like the Living Room
sensor pulled from the Sensibo bridge.

Local polling, not the Tuya cloud: these sensors broadcast on the LAN, so
readings are as fresh as the poll interval rather than whatever the cloud last
heard. A sensor that stays asleep will simply not answer, and its tile ages
out on the dashboard's own staleness rules.

Local keys are read from the tinytuya wizard's devices.json — run

    /var/lib/homebridge/tuya-blind/venv/bin/python -m tinytuya wizard

in the same directory as this script, and every paired device's key lands
there. Nothing here needs the key pasted anywhere by hand.

Run with:
    /var/lib/homebridge/tuya-blind/venv/bin/python tuya-sensor-push.py
"""
import json
import os
import sys
import time
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
DEVICES_JSON = os.path.join(HERE, "devices.json")
DASHBOARD = os.environ.get("DASHBOARD_URL", "http://127.0.0.1:8090")
INTERVAL = int(os.environ.get("TUYA_POLL_SECONDS", "120"))

# device id -> the name its dashboard tile should carry. "Bedroom" matches the
# app's built-in defaults (Room.BEDROOM, medium tile), so it needs no setup.
SENSORS = {
    "bfe36d02b20011353dicr1": "Bedroom",
}

try:
    import tinytuya
except ImportError:
    sys.exit("tinytuya missing — run this with the tuya-blind venv's python")


def load_keys():
    """id -> (key, ip, version) from the tinytuya wizard's output."""
    out = {}
    try:
        with open(DEVICES_JSON) as f:
            for d in json.load(f):
                if d.get("id") and d.get("key"):
                    out[d["id"]] = (d["key"], d.get("ip"), d.get("version") or d.get("ver") or 3.3)
    except FileNotFoundError:
        pass
    except Exception as e:
        print("devices.json unreadable: %s" % e, flush=True)
    return out


def find_ip(dev_id, fallback):
    """Prefer a live broadcast over whatever the wizard last recorded — these
    devices move around on DHCP like everything else in this house."""
    try:
        found = tinytuya.deviceScan(False, 12)
        for ip, d in found.items():
            if d.get("gwId") == dev_id:
                return ip
    except Exception:
        pass
    return fallback


def read(dev_id, key, ip, ver):
    d = tinytuya.Device(dev_id, ip, key, version=float(ver))
    d.set_socketTimeout(6)
    for _ in range(3):
        s = d.status()
        if isinstance(s, dict) and "dps" in s:
            return s["dps"]
        time.sleep(1)
    return None


def to_reading(dps):
    """Tuya temp sensors vary in which dp carries what, and most report
    temperature scaled by 10. Rather than hardcode one product's mapping,
    recognise the shape: a plausible temperature and a plausible humidity."""
    temp = hum = None
    for k in ("1", "101", "va_temperature", "temp_current"):
        if k in dps and isinstance(dps[k], (int, float)):
            v = float(dps[k])
            temp = v / 10.0 if abs(v) > 80 else v
            break
    for k in ("2", "102", "va_humidity", "humidity_value"):
        if k in dps and isinstance(dps[k], (int, float)):
            v = float(dps[k])
            hum = v / 10.0 if v > 100 else v
            break
    return temp, hum


def push(name, temp, hum):
    body = {"name": name}
    if temp is not None:
        body["temp"] = round(temp, 1)
    if hum is not None:
        body["humidity"] = round(hum)
    if len(body) == 1:
        return
    req = urllib.request.Request(
        DASHBOARD.rstrip("/") + "/api/push-sensor",
        data=json.dumps(body).encode(),
        headers={"Content-Type": "application/json"},
    )
    urllib.request.urlopen(req, timeout=8).read()
    print("pushed %s" % body, flush=True)


def main():
    while True:
        keys = load_keys()
        if not keys:
            print(
                "no local keys yet — run `python -m tinytuya wizard` in %s" % HERE,
                flush=True,
            )
        for dev_id, name in SENSORS.items():
            if dev_id not in keys:
                continue
            key, ip, ver = keys[dev_id]
            ip = find_ip(dev_id, ip)
            if not ip:
                print("%s: not on the network right now" % name, flush=True)
                continue
            try:
                dps = read(dev_id, key, ip, ver)
            except Exception as e:
                print("%s: %s" % (name, e), flush=True)
                continue
            if not dps:
                print("%s: no reply from %s" % (name, ip), flush=True)
                continue
            temp, hum = to_reading(dps)
            if temp is None and hum is None:
                # first run against an unknown product: show what it sent so the
                # mapping above can be corrected rather than guessed at again
                print("%s: unrecognised dps %s" % (name, dps), flush=True)
                continue
            try:
                push(name, temp, hum)
            except Exception as e:
                print("%s: push failed: %s" % (name, e), flush=True)
        time.sleep(INTERVAL)


if __name__ == "__main__":
    main()
