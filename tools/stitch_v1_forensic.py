#!/usr/bin/env python3
import hashlib
import html.parser
import json
import os
import posixpath
import re
import struct
import sys
import zipfile

EXPECTED_SHA = "7b3428e474e7c778a11fecf995d417bc1b6d5d9575a879fa9da1e86eb637895d"
EXPECTED_COUNTS = {"code.html": 159, ".png": 160, ".md": 7, "files": 326}

HEX_RE = re.compile(r"#[0-9a-fA-F]{3,8}\b")
CSS_NUMERIC_RE = re.compile(r"(?<![A-Za-z0-9_-])(\d+(?:\.\d+)?)(px|rem|em|sp|dp|%)\b")
FONT_RE = re.compile(r"font-family\s*:\s*([^;}]+)", re.I)
URL_RE = re.compile(r"url\(\s*['\"]?([^'\")]+)", re.I)

class Parser(html.parser.HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.title = []
        self.headings = []
        self.text = []
        self.images = []
        self.buttons = []
        self.inputs = []
        self.links = []
        self.scripts = []
        self._capture = None

    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        if tag == "title": self._capture = "title"
        elif re.fullmatch(r"h[1-6]", tag): self._capture = tag
        if tag == "img": self.images.append(a.get("src", ""))
        if tag == "button": self.buttons.append({"text": "", "type": a.get("type"), "aria": a.get("aria-label"), "onclick": a.get("onclick")})
        if tag == "input": self.inputs.append({"type": a.get("type"), "name": a.get("name"), "placeholder": a.get("placeholder")})
        if tag == "a": self.links.append({"href": a.get("href"), "text": ""})
        if tag == "script": self.scripts.append(a.get("src") or "inline")

    def handle_endtag(self, tag):
        if self._capture == tag or (self._capture and self._capture.startswith("h") and tag == self._capture):
            self._capture = None

    def handle_data(self, data):
        s = " ".join(data.split())
        if not s: return
        if self._capture == "title": self.title.append(s)
        elif self._capture and self._capture.startswith("h"): self.headings.append(s)
        self.text.append(s)
        if self.buttons: self.buttons[-1]["text"] = (self.buttons[-1]["text"] + " " + s).strip()
        if self.links: self.links[-1]["text"] = (self.links[-1]["text"] + " " + s).strip()


def sha256_bytes(data):
    return hashlib.sha256(data).hexdigest()


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def png_metadata(data):
    if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n":
        return {"valid_png_signature": False}
    width, height = struct.unpack(">II", data[16:24])
    return {"valid_png_signature": True, "width": width, "height": height, "bytes": len(data), "sha256": sha256_bytes(data)}


def resolve_asset(state_path, reference):
    if not reference or reference.startswith(("data:", "http:", "https:", "#")):
        return None
    state_dir = posixpath.dirname(state_path)
    candidate = posixpath.normpath(posixpath.join(state_dir, reference.split("#", 1)[0].split("?", 1)[0]))
    return candidate.lstrip("./")


def analyze_html(name, data, zip_names):
    text = data.decode("utf-8", "replace")
    p = Parser()
    p.feed(text)
    colors = sorted(set(HEX_RE.findall(text)), key=lambda x: x.lower())
    numeric = sorted(set(CSS_NUMERIC_RE.findall(text)))
    fonts = sorted(set(x.strip() for x in FONT_RE.findall(text)))
    refs = []
    raw_refs = list(p.images) + URL_RE.findall(text)
    for ref in raw_refs:
        resolved = resolve_asset(name, ref)
        refs.append({"source": ref, "resolved": resolved, "exists_in_zip": resolved in zip_names if resolved else False})
    return {
        "path": name,
        "sha256": sha256_bytes(data),
        "bytes": len(data),
        "title": " ".join(p.title),
        "headings": p.headings[:20],
        "buttons": p.buttons[:100],
        "inputs": p.inputs[:100],
        "images": p.images[:100],
        "asset_references": refs[:200],
        "links": p.links[:100],
        "scripts": p.scripts[:100],
        "hex_colors": colors,
        "font_families": fonts,
        "numeric_css_tokens": [list(x) for x in numeric[:200]],
        "visible_text_sample": " ".join(p.text)[:2000],
    }


def main(path):
    if not os.path.isfile(path):
        raise SystemExit(f"artifact missing: {path}")
    actual_sha = sha256(path)
    if actual_sha != EXPECTED_SHA:
        raise SystemExit(f"SHA mismatch: {actual_sha} != {EXPECTED_SHA}")
    with zipfile.ZipFile(path) as z:
        names = [n for n in z.namelist() if not n.endswith("/")]
        name_set = set(names)
        counts = {
            "code.html": sum(n.lower().endswith("code.html") for n in names),
            ".png": sum(n.lower().endswith(".png") for n in names),
            ".md": sum(n.lower().endswith((".md", ".markdown")) for n in names),
            "files": len(names),
        }
        for key, expected in EXPECTED_COUNTS.items():
            if counts[key] != expected:
                raise SystemExit(f"count mismatch {key}: {counts[key]} != {expected}")
        html_states = []
        for name in sorted(names):
            if name.lower().endswith("code.html"):
                html_states.append(analyze_html(name, z.read(name), name_set))
        pngs = []
        for name in sorted(names):
            if name.lower().endswith(".png"):
                data = z.read(name)
                pngs.append({"path": name, **png_metadata(data)})
        markdown = [n for n in names if n.lower().endswith((".md", ".markdown"))]
        referenced_assets = sorted({r["resolved"] for s in html_states for r in s["asset_references"] if r["resolved"] and r["exists_in_zip"]})
        manifest = {
            "artifact": os.path.basename(path),
            "sha256": actual_sha,
            "counts": counts,
            "files": sorted(names),
            "png_files": sorted(n["path"] for n in pngs),
            "png_assets": pngs,
            "referenced_png_assets": [p for p in pngs if p["path"] in referenced_assets],
            "markdown_files": sorted(markdown),
            "html_states": html_states,
        }
    os.makedirs("stitch-forensic", exist_ok=True)
    with open("stitch-forensic/deep-manifest.json", "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2, ensure_ascii=False)
    with open("stitch-forensic/deep-summary.txt", "w", encoding="utf-8") as f:
        f.write(f"ZIP_SHA256={actual_sha}\n")
        f.write(json.dumps(counts, sort_keys=True) + "\n")
        f.write(f"PNG_REFERENCED={len(manifest['referenced_png_assets'])}\n")
        for state in html_states:
            f.write(f"\nSTATE={state['path']}\nTITLE={state['title']}\n")
            f.write("HEADINGS=" + " | ".join(state["headings"]) + "\n")
            f.write("BUTTONS=" + json.dumps(state["buttons"], ensure_ascii=False) + "\n")
            f.write("INPUTS=" + json.dumps(state["inputs"], ensure_ascii=False) + "\n")
            f.write("IMAGES=" + json.dumps(state["images"], ensure_ascii=False) + "\n")
            f.write("ASSET_REFERENCES=" + json.dumps(state["asset_references"], ensure_ascii=False) + "\n")
            f.write("COLORS=" + ",".join(state["hex_colors"]) + "\n")
            f.write("FONTS=" + ",".join(state["font_families"]) + "\n")
    print(f"STITCH_FORENSIC=PASS SHA256={actual_sha}")
    print(f"COUNTS={json.dumps(counts, sort_keys=True)}")
    print(f"PNG_REFERENCED={len(manifest['referenced_png_assets'])}")
    print("MANIFEST=stitch-forensic/deep-manifest.json")

if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "stitch/stitch_alfa_device_control_v1.0.0.zip")
