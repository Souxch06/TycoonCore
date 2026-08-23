#!/usr/bin/env python3
"""Dependency-free ValoriaTycoon release audit; use --full to require Maven."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import shutil
import struct
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EXPECTED_MODELS = 259


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def hash_tree(root: Path) -> dict[str, str]:
    return {
        str(path.relative_to(root)).replace("\\", "/"): hashlib.sha256(path.read_bytes()).hexdigest()
        for path in sorted(root.rglob("*"))
        if path.is_file()
    }


def strip_java(source: str) -> tuple[str, str]:
    output: list[str] = []
    index = 0
    state = "code"
    while index < len(source):
        if state == "code":
            if source.startswith("//", index):
                state, index = "line", index + 2
            elif source.startswith("/*", index):
                state, index = "block", index + 2
            elif source.startswith('"""', index):
                state, index = "text", index + 3
            elif source[index] == '"':
                state, index = "string", index + 1
            elif source[index] == "'":
                state, index = "char", index + 1
            else:
                output.append(source[index])
                index += 1
        elif state == "line":
            if source[index] == "\n":
                state = "code"
                output.append("\n")
            index += 1
        elif state == "block":
            if source.startswith("*/", index):
                state, index = "code", index + 2
            else:
                index += 1
        elif state == "text":
            if source.startswith('"""', index):
                state, index = "code", index + 3
            elif source[index] == "\\":
                index += 2
            else:
                index += 1
        else:
            end = '"' if state == "string" else "'"
            if source[index] == "\\":
                index += 2
            elif source[index] == end:
                state, index = "code", index + 1
            else:
                index += 1
    return "".join(output), state


def verify_java() -> None:
    files = list((ROOT / "src/main/java").rglob("*.java")) + list(
        (ROOT / "src/test/java").rglob("*.java")
    )
    main_text: list[str] = []
    for path in files:
        source = path.read_text(encoding="utf-8")
        match = re.search(r"^package\s+([\w.]+);", source, re.MULTILINE)
        require(match is not None, f"Missing package: {path}")
        source_root = ROOT / ("src/main/java" if "/main/" in str(path) else "src/test/java")
        expected = source_root.joinpath(*match.group(1).split("."), path.name)
        require(path == expected, f"Package/path mismatch: {path}")
        stripped, state = strip_java(source)
        require(state == "code", f"Unterminated Java literal/comment: {path}")
        for opening, closing in (("(", ")"), ("{", "}"), ("[", "]")):
            depth = 0
            for character in stripped:
                depth += character == opening
                depth -= character == closing
                require(depth >= 0, f"Unexpected {closing}: {path}")
            require(depth == 0, f"Unbalanced {opening}{closing}: {path}")
        body = "\n".join(line for line in source.splitlines() if not line.startswith("import "))
        seen: set[str] = set()
        for line in source.splitlines():
            if line.startswith("import ") and not line.startswith("import static"):
                imported = line[7:-1]
                require(imported not in seen, f"Duplicate import {imported}: {path}")
                seen.add(imported)
                simple = imported.rsplit(".", 1)[-1]
                require(re.search(rf"\b{re.escape(simple)}\b", body) is not None,
                        f"Unused import {simple}: {path}")
        if "/main/" in str(path):
            main_text.append(source)

    all_main = "\n".join(main_text)
    item_root = ROOT / "resource-pack/assets/valoriatycoon/items"
    for model in set(re.findall(r'"((?:ui|item)/[a-z0-9_/]+)"', all_main)):
        if model.endswith(("/", "_")):
            continue
        require((item_root / f"{model}.json").is_file(), f"Missing static item model: {model}")


def verify_configs() -> None:
    resources = ROOT / "src/main/resources"
    for path in resources.glob("*.yml"):
        source = path.read_text(encoding="utf-8")
        require("\t" not in source, f"YAML tab: {path}")
        for number, line in enumerate(source.splitlines(), 1):
            if line.strip() and not line.lstrip().startswith("#"):
                require((len(line) - len(line.lstrip(" "))) % 2 == 0,
                        f"Odd YAML indentation {path}:{number}")

    plugin = (resources / "plugin.yml").read_text(encoding="utf-8")
    command_block = plugin.split("commands:\n", 1)[1].split("\npermissions:", 1)[0]
    declared = set(re.findall(r"^  ([a-z0-9_-]+):$", command_block, re.MULTILINE))
    java = (ROOT / "src/main/java/fr/valoriatycoon/ValoriaTycoonPlugin.java").read_text(
        encoding="utf-8"
    )
    registered = set(re.findall(r'commands\.register\("([a-z0-9_-]+)"', java))
    require(declared == registered,
            f"plugin.yml/CommandRegistrar mismatch: declared={declared}, registered={registered}")

    manager = (ROOT / "src/main/java/fr/valoriatycoon/config/ConfigManager.java").read_text(
        encoding="utf-8"
    )
    for path in resources.glob("*.yml"):
        if path.name in {"plugin.yml", "config.yml"}:
            continue
        require(f'saveResourceIfMissing("{path.name}")' in manager,
                f"ConfigManager does not install {path.name}")

    all_text = "\n".join(
        path.read_text(encoding="utf-8", errors="ignore")
        for path in (ROOT / "src").rglob("*")
        if path.is_file()
    )
    require("cabillaud" not in all_text.lower(), "Forbidden fish display name: cabillaud")
    require(not any("worker" in path.name.lower() for path in (ROOT / "src").rglob("*")),
            "Removed Workers module has returned")


def verify_resource_pack() -> None:
    pack = ROOT / "resource-pack"
    ET.parse(ROOT / "pom.xml")
    ET.parse(ROOT / "src/assembly/resource-pack.xml")
    json.loads((pack / "pack.mcmeta").read_text(encoding="utf-8"))
    for path in pack.rglob("*.json"):
        json.loads(path.read_text(encoding="utf-8"))

    namespace = pack / "assets/valoriatycoon"
    groups: list[set[str]] = []
    for directory, suffix in (("items", ".json"), ("models/item", ".json"), ("textures/item", ".png")):
        base = namespace / directory
        groups.append({
            str(path.relative_to(base).with_suffix("")).replace("\\", "/")
            for path in base.rglob(f"*{suffix}")
        })
    require(len(groups[0]) == EXPECTED_MODELS, f"Expected {EXPECTED_MODELS} item models")
    require(groups[0] == groups[1] == groups[2], "Item/model/texture sets differ")

    for path in (namespace / "textures/item").rglob("*.png"):
        data = path.read_bytes()
        require(data.startswith(b"\x89PNG\r\n\x1a\n"), f"Invalid PNG: {path}")
        require(struct.unpack(">II", data[16:24]) == (32, 32), f"Texture is not premium 32x32: {path}")

    gui = pack / "assets/minecraft/textures/gui"
    expected_gui = {
        gui / "container/generic_54.png": (256, 256),
        gui / "sprites/container/slot.png": (18, 18),
        gui / "sprites/container/slot_highlight_front.png": (18, 18),
    }
    for path, dimensions in expected_gui.items():
        require(path.is_file(), f"Missing premium GUI texture: {path}")
        data = path.read_bytes()
        require(data.startswith(b"\x89PNG\r\n\x1a\n"), f"Invalid GUI PNG: {path}")
        require(struct.unpack(">II", data[16:24]) == dimensions, f"Invalid GUI dimensions: {path}")

    before = hash_tree(pack)
    subprocess.run([sys.executable, "scripts/generate-resource-pack.py"], cwd=ROOT, check=True,
                   stdout=subprocess.DEVNULL)
    require(before == hash_tree(pack), "Resource-pack generation is not deterministic")

    with tempfile.TemporaryDirectory() as temporary:
        archive = Path(temporary) / "pack.zip"
        with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as output:
            for path in sorted(pack.rglob("*")):
                if path.is_file() and path.name != "README.md":
                    output.write(path, path.relative_to(pack))
        with zipfile.ZipFile(archive) as source:
            names = set(source.namelist())
        require("pack.mcmeta" in names and "pack.png" in names, "Broken resource-pack ZIP root")
        require(all(not name.startswith("resource-pack/") for name in names), "ZIP has an extra root")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--full", action="store_true", help="also require mvn clean verify")
    arguments = parser.parse_args()

    git = shutil.which("git")
    if git is not None and (ROOT / ".git").exists():
        subprocess.run([git, "diff", "--check"], cwd=ROOT, check=True)
    verify_java()
    verify_configs()
    verify_resource_pack()
    print(f"Static release audit passed: {EXPECTED_MODELS} models, Java/config/package consistency OK.")

    maven = shutil.which("mvn")
    if arguments.full:
        require(maven is not None, "Maven is required for --full")
        subprocess.run([maven, "clean", "verify"], cwd=ROOT, check=True)
        print("Maven clean verify passed.")
    elif maven is None:
        print("Maven unavailable: runtime/Paper compilation remains an external release gate.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (AssertionError, subprocess.CalledProcessError, ET.ParseError, json.JSONDecodeError) as error:
        print(f"RELEASE AUDIT FAILED: {error}", file=sys.stderr)
        raise SystemExit(1)
