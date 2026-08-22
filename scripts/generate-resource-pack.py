#!/usr/bin/env python3
"""Generate ValoriaTycoon's deterministic 16x16 item-model resource pack."""

from __future__ import annotations

import json
import shutil
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "resource-pack" / "assets" / "valoriatycoon"
TRANSPARENT = (0, 0, 0, 0)
DARK = (24, 15, 43, 255)
INK = (12, 9, 24, 255)
GOLD = (249, 204, 83, 255)
LIGHT = (255, 241, 181, 255)
PURPLE = (104, 72, 181, 255)
VIOLET = (57, 37, 106, 255)
CYAN = (74, 220, 232, 255)
GREEN = (83, 190, 104, 255)
RED = (218, 75, 87, 255)
BLUE = (66, 125, 213, 255)
ORANGE = (232, 136, 60, 255)
GRAY = (140, 145, 160, 255)
WHITE = (235, 240, 247, 255)

FONT = {
    "A": ("01110", "10001", "10001", "11111", "10001", "10001", "10001"),
    "B": ("11110", "10001", "10001", "11110", "10001", "10001", "11110"),
    "C": ("01111", "10000", "10000", "10000", "10000", "10000", "01111"),
    "D": ("11110", "10001", "10001", "10001", "10001", "10001", "11110"),
    "E": ("11111", "10000", "10000", "11110", "10000", "10000", "11111"),
    "F": ("11111", "10000", "10000", "11110", "10000", "10000", "10000"),
    "G": ("01111", "10000", "10000", "10111", "10001", "10001", "01111"),
    "H": ("10001", "10001", "10001", "11111", "10001", "10001", "10001"),
    "I": ("11111", "00100", "00100", "00100", "00100", "00100", "11111"),
    "J": ("00111", "00010", "00010", "00010", "10010", "10010", "01100"),
    "K": ("10001", "10010", "10100", "11000", "10100", "10010", "10001"),
    "L": ("10000", "10000", "10000", "10000", "10000", "10000", "11111"),
    "M": ("10001", "11011", "10101", "10101", "10001", "10001", "10001"),
    "N": ("10001", "11001", "10101", "10011", "10001", "10001", "10001"),
    "O": ("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
    "P": ("11110", "10001", "10001", "11110", "10000", "10000", "10000"),
    "Q": ("01110", "10001", "10001", "10001", "10101", "10010", "01101"),
    "R": ("11110", "10001", "10001", "11110", "10100", "10010", "10001"),
    "S": ("01111", "10000", "10000", "01110", "00001", "00001", "11110"),
    "T": ("11111", "00100", "00100", "00100", "00100", "00100", "00100"),
    "U": ("10001", "10001", "10001", "10001", "10001", "10001", "01110"),
    "V": ("10001", "10001", "10001", "10001", "10001", "01010", "00100"),
    "W": ("10001", "10001", "10001", "10101", "10101", "11011", "10001"),
    "X": ("10001", "10001", "01010", "00100", "01010", "10001", "10001"),
    "Y": ("10001", "10001", "01010", "00100", "00100", "00100", "00100"),
    "Z": ("11111", "00001", "00010", "00100", "01000", "10000", "11111"),
    "0": ("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
    "1": ("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    "2": ("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
    "3": ("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
    "4": ("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    "5": ("11111", "10000", "10000", "11110", "00001", "00001", "11110"),
    "6": ("01110", "10000", "10000", "11110", "10001", "10001", "01110"),
    "7": ("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    "8": ("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    "9": ("01110", "10001", "10001", "01111", "00001", "00001", "01110"),
}


class Canvas:
    def __init__(self) -> None:
        self.pixels = [TRANSPARENT] * 256

    def set(self, x: int, y: int, color: tuple[int, int, int, int]) -> None:
        if 0 <= x < 16 and 0 <= y < 16:
            self.pixels[y * 16 + x] = color

    def rect(self, x1: int, y1: int, x2: int, y2: int, color: tuple[int, int, int, int]) -> None:
        for y in range(y1, y2 + 1):
            for x in range(x1, x2 + 1):
                self.set(x, y, color)

    def line(self, x1: int, y1: int, x2: int, y2: int, color: tuple[int, int, int, int]) -> None:
        dx, sx = abs(x2 - x1), 1 if x1 < x2 else -1
        dy, sy = -abs(y2 - y1), 1 if y1 < y2 else -1
        error = dx + dy
        while True:
            self.set(x1, y1, color)
            if x1 == x2 and y1 == y2:
                return
            twice = 2 * error
            if twice >= dy:
                error += dy
                x1 += sx
            if twice <= dx:
                error += dx
                y1 += sy


def png(path: Path, pixels: list[tuple[int, int, int, int]]) -> None:
    def chunk(kind: bytes, data: bytes) -> bytes:
        checksum = zlib.crc32(kind + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", checksum)

    rows = []
    for y in range(16):
        row = bytearray()
        for x in range(16):
            row.extend(pixels[y * 16 + x])
        rows.append(b"\x00" + bytes(row))
    data = b"\x89PNG\r\n\x1a\n"
    data += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(b"".join(rows), 9))
    data += chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def pack_icon(pixels: list[tuple[int, int, int, int]]) -> None:
    width = height = 64
    output = []
    for y in range(height):
        for x in range(width):
            source = pixels[(y // 4) * 16 + x // 4]
            background = DARK if (x // 8 + y // 8) % 2 == 0 else VIOLET
            output.append(source if source[3] else background)

    def chunk(kind: bytes, data: bytes) -> bytes:
        checksum = zlib.crc32(kind + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", checksum)

    rows = []
    for y in range(height):
        row = bytearray()
        for x in range(width):
            row.extend(output[y * width + x])
        rows.append(b"\x00" + bytes(row))
    data = b"\x89PNG\r\n\x1a\n"
    data += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(b"".join(rows), 9))
    data += chunk(b"IEND", b"")
    (ROOT.parents[1] / "pack.png").write_bytes(data)


def medallion(base: tuple[int, int, int, int], border: tuple[int, int, int, int] = GOLD) -> Canvas:
    c = Canvas()
    shape = {
        1: (5, 10), 2: (3, 12), 3: (2, 13), 4: (2, 13), 5: (1, 14), 6: (1, 14),
        7: (1, 14), 8: (1, 14), 9: (1, 14), 10: (1, 14), 11: (2, 13), 12: (2, 13),
        13: (3, 12), 14: (5, 10),
    }
    mask = {(x, y) for y, bounds in shape.items() for x in range(bounds[0], bounds[1] + 1)}
    for x, y in mask:
        boundary = any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        c.set(x, y, INK if boundary else border if x in (2, 13) or y in (2, 13) else base)
    c.set(4, 4, tuple(min(255, channel + 45) for channel in base[:3]) + (255,))
    return c


def glyph(c: Canvas, text: str, color: tuple[int, int, int, int] = LIGHT) -> None:
    text = text[:2].upper()
    width = len(text) * 5 + max(0, len(text) - 1)
    start_x = (16 - width) // 2
    for index, character in enumerate(text):
        pattern = FONT.get(character)
        if pattern is None:
            continue
        ox = start_x + index * 6
        for y, row in enumerate(pattern):
            for x, value in enumerate(row):
                if value == "1":
                    c.set(ox + x, 4 + y, color)


def symbol(c: Canvas, name: str, color: tuple[int, int, int, int] = LIGHT) -> None:
    if name == "coin":
        for y, bounds in {4: (6, 9), 5: (5, 10), 6: (5, 10), 7: (5, 10), 8: (5, 10), 9: (5, 10), 10: (6, 9)}.items():
            for x in range(bounds[0], bounds[1] + 1): c.set(x, y, GOLD if x in bounds or y in (4, 10) else color)
        c.line(8, 5, 8, 9, LIGHT); c.set(7, 6, LIGHT); c.set(9, 8, LIGHT)
    elif name == "bars":
        c.rect(4, 9, 5, 11, color); c.rect(7, 6, 8, 11, color); c.rect(10, 4, 11, 11, color)
        c.line(3, 12, 12, 12, GOLD)
    elif name == "wheat":
        c.line(8, 4, 8, 12, GREEN)
        for x, y in ((6, 5), (10, 6), (6, 7), (10, 8), (6, 9), (10, 10)):
            c.set(x, y, GOLD); c.set((x + 8) // 2, y + 1, GOLD)
    elif name == "gear":
        c.rect(5, 5, 10, 10, color); c.rect(7, 3, 8, 12, color); c.rect(3, 7, 12, 8, color)
        c.rect(7, 7, 8, 8, DARK)
    elif name == "arrow":
        c.line(8, 3, 8, 12, color); c.line(4, 7, 8, 3, color); c.line(12, 7, 8, 3, color)
        c.line(5, 12, 11, 12, GOLD)
    elif name == "pick":
        c.line(5, 12, 11, 4, color); c.line(5, 4, 11, 5, GOLD); c.set(4, 5, GOLD); c.set(12, 6, GOLD)
    elif name == "arrows":
        c.line(4, 6, 11, 6, color); c.line(9, 4, 11, 6, color); c.line(9, 8, 11, 6, color)
        c.line(11, 10, 4, 10, GOLD); c.line(6, 8, 4, 10, GOLD); c.line(6, 12, 4, 10, GOLD)
    elif name == "paw":
        c.rect(6, 8, 9, 11, color)
        for x, y in ((4, 6), (7, 5), (10, 5), (12, 7)): c.rect(x, y, x + 1, y + 1, color)
    elif name == "scroll":
        c.rect(5, 4, 10, 11, WHITE); c.line(6, 6, 9, 6, PURPLE); c.line(6, 8, 9, 8, PURPLE)
        c.line(4, 4, 6, 4, GOLD); c.line(9, 11, 11, 11, GOLD)
    elif name == "crown":
        c.line(4, 6, 5, 11, GOLD); c.line(11, 6, 10, 11, GOLD); c.line(5, 11, 10, 11, GOLD)
        c.line(4, 6, 7, 9, GOLD); c.line(7, 9, 8, 4, GOLD); c.line(8, 4, 10, 9, GOLD); c.line(10, 9, 11, 6, GOLD)
        c.set(8, 8, color)
    elif name == "lock":
        c.rect(5, 7, 10, 12, RED); c.line(6, 7, 6, 5, WHITE); c.line(9, 7, 9, 5, WHITE); c.line(6, 5, 9, 5, WHITE)
        c.set(8, 9, DARK)
    elif name == "chest":
        c.rect(3, 6, 12, 12, ORANGE); c.line(3, 8, 12, 8, INK); c.rect(7, 8, 8, 10, GOLD)
    elif name == "fish":
        c.rect(5, 6, 10, 10, CYAN); c.line(11, 8, 13, 5, BLUE); c.line(11, 8, 13, 11, BLUE); c.set(5, 7, INK)
    elif name == "tree":
        c.rect(7, 9, 8, 13, ORANGE); c.rect(5, 5, 10, 9, GREEN); c.rect(6, 3, 9, 6, GREEN)
    elif name == "members":
        c.rect(4, 5, 6, 7, color); c.rect(9, 5, 11, 7, color); c.rect(3, 9, 7, 11, color); c.rect(8, 9, 12, 11, color)
    elif name == "hopper":
        c.line(4, 5, 11, 5, color); c.line(4, 5, 7, 10, color); c.line(11, 5, 8, 10, color); c.rect(7, 10, 8, 12, GOLD)
    elif name == "speed":
        c.line(4, 11, 11, 4, color); c.line(7, 4, 11, 4, color); c.line(11, 4, 11, 8, color)
        c.line(3, 8, 6, 8, GOLD); c.line(3, 11, 5, 11, GOLD)
    elif name == "spark":
        c.line(8, 3, 8, 12, color); c.line(3, 8, 12, 8, color); c.line(5, 5, 11, 11, GOLD); c.line(11, 5, 5, 11, GOLD)
    elif name == "clock":
        c.rect(5, 4, 10, 11, color); c.rect(4, 6, 11, 9, color)
        c.rect(6, 5, 9, 10, DARK); c.line(8, 6, 8, 8, LIGHT); c.line(8, 8, 10, 9, GOLD)
    elif name == "back":
        c.line(4, 8, 12, 8, color); c.line(4, 8, 8, 4, color); c.line(4, 8, 8, 12, color)
    else:
        glyph(c, name, color)


def ui_icon(symbol_name: str, base: tuple[int, int, int, int] = VIOLET, accent: tuple[int, int, int, int] = LIGHT) -> list[tuple[int, int, int, int]]:
    c = medallion(base)
    symbol(c, symbol_name, accent)
    return c.pixels


def filler_tile() -> list[tuple[int, int, int, int]]:
    c = Canvas()
    c.rect(0, 0, 15, 15, (19, 12, 35, 255))
    c.line(0, 0, 15, 0, VIOLET); c.line(0, 15, 15, 15, DARK)
    c.line(0, 0, 0, 15, VIOLET); c.line(15, 0, 15, 15, DARK)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)): c.set(x, y, GOLD)
    c.set(7, 7, (34, 21, 61, 255)); c.set(8, 8, (34, 21, 61, 255))
    return c.pixels


def animal_icon(kind: str, base: tuple[int, int, int, int]) -> list[tuple[int, int, int, int]]:
    c = medallion(base)
    if kind == "rabbit":
        c.rect(6, 3, 7, 7, WHITE); c.rect(9, 3, 10, 7, WHITE); c.rect(5, 7, 11, 12, WHITE); c.set(7, 9, INK); c.set(10, 9, INK); c.set(8, 11, RED)
    elif kind in ("fox", "wolf"):
        fur = ORANGE if kind == "fox" else GRAY
        c.line(4, 5, 6, 3, fur); c.line(11, 5, 9, 3, fur); c.rect(4, 5, 11, 11, fur); c.set(6, 8, INK); c.set(9, 8, INK); c.set(8, 10, INK)
    elif kind == "bee":
        c.rect(4, 6, 11, 10, GOLD); c.line(6, 6, 6, 10, INK); c.line(9, 6, 9, 10, INK); c.rect(3, 4, 5, 6, CYAN); c.rect(10, 4, 12, 6, CYAN)
    elif kind == "allay":
        c.rect(6, 5, 9, 11, CYAN); c.line(5, 6, 3, 10, WHITE); c.line(10, 6, 12, 10, WHITE); c.set(7, 7, INK); c.set(9, 7, INK)
    elif kind == "golem":
        c.rect(4, 4, 11, 12, WHITE); c.rect(3, 7, 12, 9, GRAY); c.set(6, 7, RED); c.set(9, 7, RED); c.line(7, 10, 9, 10, INK)
    elif kind == "dragon":
        c.rect(5, 6, 10, 11, GREEN); c.line(5, 6, 3, 3, GOLD); c.line(10, 6, 12, 3, GOLD); c.set(6, 8, INK); c.set(9, 8, INK); c.line(8, 10, 12, 12, RED)
    else:
        c.line(8, 3, 5, 10, ORANGE); c.line(8, 3, 11, 10, RED); c.line(5, 10, 8, 12, GOLD); c.line(11, 10, 8, 12, GOLD); c.set(8, 7, WHITE)
    return c.pixels


def compact_icon(color: tuple[int, int, int, int], level: int) -> list[tuple[int, int, int, int]]:
    c = Canvas()
    c.rect(3, 4, 11, 12, INK); c.rect(4, 3, 12, 11, color); c.line(4, 3, 12, 3, LIGHT); c.line(12, 3, 12, 11, DARK)
    c.line(4, 3, 7, 6, LIGHT); c.line(12, 3, 9, 6, DARK); c.line(7, 6, 9, 6, GOLD)
    for index in range(level): c.rect(5 + index * 3, 9, 6 + index * 3, 10, GOLD)
    return c.pixels


def tool_icon(tool: str, color: tuple[int, int, int, int]) -> list[tuple[int, int, int, int]]:
    c = Canvas()
    handle = (111, 72, 42, 255)
    if tool == "fishing_rod":
        c.line(4, 12, 10, 3, handle); c.line(10, 3, 13, 7, color); c.line(13, 7, 13, 12, CYAN); c.set(13, 13, RED)
    elif tool == "hoe":
        c.line(5, 13, 10, 4, handle); c.line(9, 4, 13, 5, color); c.line(13, 5, 12, 7, color)
    elif tool == "axe":
        c.line(5, 13, 10, 4, handle); c.rect(8, 3, 12, 7, color); c.set(8, 7, TRANSPARENT); c.set(12, 3, LIGHT)
    else:
        c.line(5, 13, 10, 5, handle); c.line(4, 4, 11, 3, color); c.line(4, 4, 2, 7, color); c.line(11, 3, 13, 6, color)
    return c.pixels


def ranked_tool_icon(
    tool: str,
    theme: dict[str, tuple[int, int, int, int]],
    rank: int,
) -> list[tuple[int, int, int, int]]:
    """Draw one full rank identity shared coherently by every multi-tool form."""
    c = Canvas()
    metal, edge, shadow = theme["metal"], theme["edge"], theme["shadow"]
    handle, handle_light = theme["handle"], theme["handle_light"]
    wrap, gem = theme["wrap"], theme["gem"]

    if tool == "fishing_rod":
        # Reinforced rod, line, hook and rank-colored reel.
        c.line(3, 14, 10, 2, INK); c.line(4, 14, 11, 2, INK)
        c.line(4, 13, 10, 3, handle); c.line(5, 12, 11, 2, handle_light)
        c.line(10, 2, 13, 5, metal); c.set(11, 2, edge); c.set(12, 4, shadow)
        c.line(13, 5, 13, 12, (91, 188, 213, 255)); c.set(13, 13, edge)
        c.rect(6, 8, 8, 10, INK); c.rect(7, 8, 8, 9, wrap); c.set(8, 9, gem)
        head_x, head_y = 10, 3
    else:
        # Two-pixel-wide role-playing handle with a shared rank wrap.
        c.line(3, 15, 10, 4, INK); c.line(4, 15, 11, 4, INK)
        c.line(4, 14, 10, 5, handle); c.line(5, 14, 11, 4, handle_light)
        c.set(6, 11, wrap); c.set(7, 10, wrap); c.set(7, 11, gem)
        c.set(4, 14, edge)
        if tool == "hoe":
            c.line(8, 3, 13, 3, INK); c.line(9, 4, 14, 4, INK); c.line(14, 4, 12, 8, INK)
            c.line(9, 3, 13, 3, edge); c.line(10, 4, 13, 4, metal); c.line(13, 5, 12, 7, shadow)
            head_x, head_y = 11, 4
        elif tool == "axe":
            c.rect(7, 2, 13, 8, INK); c.rect(8, 3, 12, 7, metal)
            c.line(9, 3, 12, 3, edge); c.line(12, 4, 12, 7, shadow)
            c.set(8, 7, TRANSPARENT); c.set(9, 7, TRANSPARENT); c.set(7, 8, TRANSPARENT)
            head_x, head_y = 10, 5
        else:
            c.line(2, 4, 11, 1, INK); c.line(2, 5, 4, 9, INK); c.line(11, 1, 15, 5, INK)
            c.line(3, 4, 11, 2, edge); c.line(4, 5, 11, 3, metal)
            c.line(3, 5, 4, 8, shadow); c.line(11, 2, 14, 5, shadow)
            head_x, head_y = 9, 4

    # Heraldic evolution: every rank changes both palette and ornamentation.
    if rank == 0:  # Sans rang: chipped and roughly bound.
        c.set(head_x, head_y, shadow); c.set(5, 13, GRAY)
    elif rank == 1:  # Citoyen: simple brass seal and green civic wrapping.
        c.set(head_x, head_y, gem); c.set(head_x + 1, head_y, edge); c.set(3, 14, GOLD)
    elif rank == 2:  # Artisan: paired copper rivets.
        c.set(head_x - 1, head_y, wrap); c.set(head_x + 1, head_y, wrap); c.set(head_x, head_y, LIGHT)
    elif rank == 3:  # Marchand: emerald coin in a gold setting.
        c.rect(head_x - 1, head_y - 1, head_x + 1, head_y + 1, GOLD); c.set(head_x, head_y, gem)
    elif rank == 4:  # Écuyer: blue tabard tassel.
        c.set(head_x, head_y, gem); c.line(5, 12, 4, 14, wrap); c.set(4, 14, edge)
    elif rank == 5:  # Chevalier: red heraldic cross.
        c.set(head_x, head_y, gem)
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)): c.set(head_x + dx, head_y + dy, wrap)
    elif rank == 6:  # Baron: ruby and flared gold guard.
        c.set(head_x, head_y, gem); c.set(head_x - 2, head_y, edge); c.set(head_x + 2, head_y, edge)
        c.set(6, 12, wrap)
    elif rank == 7:  # Vicomte: twin amethysts and pale-gold filigree.
        c.set(head_x - 1, head_y, gem); c.set(head_x + 1, head_y, gem)
        c.set(head_x, head_y - 1, edge); c.set(head_x, head_y + 1, edge)
    elif rank == 8:  # Comte: sapphire crystal star.
        c.set(head_x, head_y, LIGHT)
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)): c.set(head_x + dx, head_y + dy, gem)
        c.set(4, 13, wrap)
    elif rank == 9:  # Marquis: violet jewel with gold rays.
        c.set(head_x, head_y, gem)
        for dx, dy in ((-1, -1), (1, -1), (-1, 1), (1, 1)): c.set(head_x + dx, head_y + dy, GOLD)
        c.set(6, 12, wrap); c.set(5, 13, edge)
    else:  # Duc: crowned netherite, cyan rune and gold grip.
        c.set(head_x, head_y, gem); c.set(head_x - 1, head_y - 1, GOLD); c.set(head_x + 1, head_y - 1, GOLD)
        c.set(head_x, head_y - 2, LIGHT); c.set(head_x - 1, head_y, edge); c.set(head_x + 1, head_y, edge)
        c.set(5, 13, CYAN); c.set(6, 12, GOLD); c.set(7, 11, CYAN)
    return c.pixels


def generator_icon(kind: str, color: tuple[int, int, int, int]) -> list[tuple[int, int, int, int]]:
    c = Canvas(); c.rect(2, 4, 12, 13, INK); c.rect(3, 3, 13, 12, color); c.line(3, 3, 13, 3, LIGHT); c.line(13, 3, 13, 12, DARK)
    symbol(c, {"miner": "pick", "farmer": "wheat", "lumber": "tree", "fisher": "fish"}[kind], WHITE)
    c.rect(11, 10, 13, 12, GOLD); c.set(12, 11, INK)
    return c.pixels


def reward_bag_icon(
    base: tuple[int, int, int, int],
    accent: tuple[int, int, int, int],
    tier: int,
    mark: str,
) -> list[tuple[int, int, int, int]]:
    c = Canvas()
    c.rect(5, 2, 10, 4, INK); c.rect(6, 2, 9, 3, accent)
    c.line(4, 5, 11, 5, INK); c.line(3, 7, 12, 7, INK)
    c.rect(3, 8, 12, 13, INK); c.rect(4, 6, 11, 12, base)
    c.set(4, 7, LIGHT); c.set(11, 12, DARK)
    if mark == "coin":
        symbol(c, "coin", accent)
    else:
        c.line(6, 8, 8, 11, accent); c.line(8, 11, 10, 8, accent)
    for index in range(tier):
        c.set(5 + index * 2, 14, accent)
    return c.pixels


def reward_vial_icon(tier: int) -> list[tuple[int, int, int, int]]:
    c = Canvas()
    c.rect(6, 2, 9, 4, GOLD); c.rect(5, 4, 10, 6, INK)
    c.rect(4, 6, 11, 13, INK); c.rect(5, 6, 10, 12, (64, 182, 220, 255))
    c.line(5, 7, 10, 12, CYAN); c.set(6, 7, WHITE); c.set(9, 10, LIGHT)
    for index in range(tier): c.set(5 + index * 2, 14, CYAN)
    return c.pixels


def reward_bundle_icon(tier: int) -> list[tuple[int, int, int, int]]:
    c = Canvas()
    c.rect(2, 5, 13, 13, INK); c.rect(3, 4, 12, 12, (128, 79, 37, 255))
    c.line(3, 7, 12, 7, GOLD); c.line(6, 4, 6, 12, DARK); c.line(10, 4, 10, 12, DARK)
    symbol(c, "wheat", GREEN)
    for index in range(tier): c.set(6 + index * 2, 14, GOLD)
    return c.pixels


def reward_voucher_icon(mark: str, base: tuple[int, int, int, int]) -> list[tuple[int, int, int, int]]:
    c = Canvas()
    c.rect(3, 2, 12, 13, INK); c.rect(4, 2, 11, 12, (238, 226, 180, 255))
    c.line(5, 4, 10, 4, base); c.line(5, 11, 10, 11, GOLD)
    symbol(c, mark, base)
    c.set(3, 3, GOLD); c.set(12, 12, GOLD)
    return c.pixels


def crate_texture(
    kind: str,
    base: tuple[int, int, int, int],
    trim: tuple[int, int, int, int],
    accent: tuple[int, int, int, int],
) -> list[tuple[int, int, int, int]]:
    """Draw a unique pixel-art skin and reserve top-row swatches for 3D ornaments."""
    c = Canvas()
    c.rect(0, 0, 15, 15, INK)
    c.rect(1, 1, 14, 14, base)
    c.rect(2, 2, 13, 4, tuple(max(0, channel - 22) for channel in base[:3]) + (255,))
    c.rect(2, 11, 13, 13, tuple(max(0, channel - 34) for channel in base[:3]) + (255,))
    c.line(1, 5, 14, 5, trim); c.line(1, 10, 14, 10, trim)
    c.line(4, 1, 4, 14, DARK); c.line(11, 1, 11, 14, DARK)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        c.set(x, y, accent)

    if kind == "vote":
        # Turquoise ballot seal: a white vote slip and a large validated check.
        c.rect(5, 3, 11, 11, WHITE); c.rect(6, 4, 10, 10, (210, 245, 249, 255))
        c.line(6, 7, 8, 9, trim); c.line(8, 9, 11, 5, trim)
        c.set(4, 12, accent); c.set(12, 3, accent)
    elif kind == "quest":
        # Enchanted parchment with cyan runes and golden rollers.
        c.rect(5, 3, 10, 12, (238, 229, 193, 255))
        c.line(4, 3, 6, 3, trim); c.line(9, 12, 11, 12, trim)
        c.line(6, 6, 9, 6, accent); c.line(6, 8, 8, 8, accent); c.set(9, 10, accent)
    elif kind == "farm":
        # Rustic planks, wheat and fresh leaves.
        c.line(2, 4, 13, 4, (92, 55, 29, 255)); c.line(2, 12, 13, 12, (92, 55, 29, 255))
        symbol(c, "wheat", trim)
        c.set(5, 6, accent); c.set(10, 8, accent)
    elif kind == "common":
        # Emerald steel with a simple silver civic shield.
        c.rect(5, 3, 10, 11, DARK); c.rect(6, 4, 9, 10, accent)
        c.set(6, 10, TRANSPARENT); c.set(9, 10, TRANSPARENT)
        glyph(c, "C", trim)
    elif kind == "rare":
        # Sapphire facets and an icy central gem.
        c.line(3, 8, 8, 2, accent); c.line(8, 2, 13, 8, accent)
        c.line(3, 8, 8, 13, trim); c.line(8, 13, 13, 8, trim)
        c.rect(7, 6, 9, 9, LIGHT); c.set(8, 7, WHITE)
    elif kind == "epic":
        # Orange magma cracks over a dark forged shell.
        c.line(3, 13, 6, 8, trim); c.line(6, 8, 5, 4, accent)
        c.line(6, 8, 10, 6, trim); c.line(10, 6, 12, 2, accent)
        c.line(10, 6, 12, 11, trim); c.set(8, 8, LIGHT)
    elif kind == "legendary":
        # Sun-gold royal crown with a white central jewel.
        symbol(c, "crown", accent)
        c.set(4, 4, LIGHT); c.set(12, 4, LIGHT); c.set(3, 12, trim); c.set(13, 12, trim)
    elif kind == "valoria":
        # Crimson royal seal: black steel, gold filigree and a ruby V.
        c.line(2, 2, 13, 13, trim); c.line(13, 2, 2, 13, trim)
        c.rect(4, 3, 11, 12, (70, 9, 18, 255)); symbol(c, "crown", trim)
        c.line(6, 9, 8, 12, accent); c.line(8, 12, 11, 8, accent)
        c.set(8, 5, WHITE)
    else:
        # Pets: magical lavender shell, cyan paw seal and warm pink highlights.
        symbol(c, "paw", accent)
        c.set(3, 3, trim); c.set(12, 3, trim); c.set(4, 12, accent); c.set(11, 12, accent)

    # Solid-color swatches sampled by the custom cuboids. They remain a discreet upper rim in-world.
    c.set(0, 0, INK)
    c.set(1, 0, base)
    c.set(2, 0, trim)
    c.set(3, 0, accent)
    c.set(4, 0, LIGHT)
    return c.pixels


def model_element(
    start: list[float],
    end: list[float],
    material: str = "body",
    rotation: dict[str, object] | None = None,
) -> dict[str, object]:
    swatches = {
        "dark": [0, 0, 1, 1],
        "base": [1, 0, 2, 1],
        "trim": [2, 0, 3, 1],
        "accent": [3, 0, 4, 1],
        "light": [4, 0, 5, 1],
    }
    uv = [0, 0, 16, 16] if material == "body" else swatches[material]
    element: dict[str, object] = {
        "from": start,
        "to": end,
        "faces": {
            face: {"uv": uv, "texture": "#crate"}
            for face in ("north", "east", "south", "west", "up", "down")
        },
    }
    if rotation is not None:
        element["rotation"] = rotation
    return element


def crate_elements(kind: str) -> list[dict[str, object]]:
    """Build nine recognizable silhouettes instead of recoloring one vanilla cube."""
    cube = model_element
    if kind == "vote":
        return [
            cube([2, 1, 3], [14, 10, 13]), cube([1, 10, 2], [15, 12, 14], "trim"),
            cube([3, 4, 2], [4, 10, 3], "accent"), cube([12, 4, 2], [13, 10, 3], "accent"),
            cube([5, 12, 5], [11, 14, 11], "light", {
                "angle": -22.5, "axis": "x", "origin": [8, 12, 8], "rescale": True,
            }),
            cube([5, 6, 2], [11, 10, 3], "body"), cube([7, 5, 1.5], [9, 7, 2.5], "trim"),
        ]
    if kind == "quest":
        return [
            cube([2, 1, 3], [14, 10, 13]), cube([1, 9, 2], [15, 12, 14], "dark"),
            cube([1, 2, 2], [3, 11, 14], "trim"), cube([13, 2, 2], [15, 11, 14], "trim"),
            cube([4, 11, 4], [8, 13, 12], "light", {
                "angle": -22.5, "axis": "z", "origin": [8, 12, 8], "rescale": True,
            }),
            cube([8, 11, 4], [12, 13, 12], "light", {
                "angle": 22.5, "axis": "z", "origin": [8, 12, 8], "rescale": True,
            }),
            cube([6, 6, 1.5], [10, 10, 3], "body"),
        ]
    if kind == "farm":
        return [
            cube([1, 1, 2], [15, 10, 14]), cube([1, 3, 1.5], [15, 4, 14.5], "dark"),
            cube([1, 7, 1.5], [15, 8, 14.5], "dark"), cube([3, 1, 1.4], [4, 10, 14.6], "trim"),
            cube([12, 1, 1.4], [13, 10, 14.6], "trim"), cube([1, 10, 2], [15, 12, 14], "accent"),
            cube([7.5, 11, 7.5], [8.5, 17, 8.5], "trim"),
            cube([5, 13, 7], [8, 14.5, 9], "accent", {
                "angle": -22.5, "axis": "z", "origin": [8, 14, 8], "rescale": True,
            }),
            cube([8, 14, 7], [11, 15.5, 9], "accent", {
                "angle": 22.5, "axis": "z", "origin": [8, 15, 8], "rescale": True,
            }),
        ]
    if kind == "common":
        return [
            cube([2, 1, 2], [14, 10, 14]), cube([1.5, 9, 1.5], [14.5, 12, 14.5], "base"),
            cube([1, 2, 1], [3, 11, 3], "trim"), cube([13, 2, 1], [15, 11, 3], "trim"),
            cube([1, 2, 13], [3, 11, 15], "trim"), cube([13, 2, 13], [15, 11, 15], "trim"),
            cube([6, 6, 1], [10, 10, 2.5], "body"), cube([7, 5, 0.5], [9, 7, 2], "light"),
        ]
    if kind == "rare":
        return [
            cube([2, 1, 2], [14, 10, 14]), cube([1, 9, 1], [15, 12, 15], "trim"),
            cube([3, 2, 1.5], [4, 10, 14.5], "accent"), cube([12, 2, 1.5], [13, 10, 14.5], "accent"),
            cube([6, 11, 6], [10, 15, 10], "accent", {
                "angle": 45, "axis": "y", "origin": [8, 13, 8], "rescale": True,
            }),
            cube([6, 5, 1], [10, 10, 2.5], "body", {
                "angle": 45, "axis": "z", "origin": [8, 8, 2], "rescale": True,
            }),
            cube([7, 7, 0.5], [9, 9, 2], "light"),
        ]
    if kind == "epic":
        return [
            cube([2, 1, 2], [14, 10, 14]), cube([1, 9, 1], [15, 12, 15], "dark"),
            cube([2, 4, 1.5], [14, 5, 14.5], "trim"), cube([4, 1, 1.5], [5, 11, 14.5], "accent"),
            cube([11, 1, 1.5], [12, 11, 14.5], "accent"),
            cube([2, 11, 2], [4, 16, 4], "trim", {
                "angle": -22.5, "axis": "z", "origin": [3, 12, 3], "rescale": True,
            }),
            cube([12, 11, 2], [14, 16, 4], "trim", {
                "angle": 22.5, "axis": "z", "origin": [13, 12, 3], "rescale": True,
            }),
            cube([6, 5, 1], [10, 10, 2.5], "body"), cube([7, 7, 0.5], [9, 9, 2], "light"),
        ]
    if kind == "legendary":
        return [
            cube([1.5, 1, 2], [14.5, 10, 14]), cube([0.5, 9, 1], [15.5, 12, 15], "trim"),
            cube([2, 2, 1.3], [3.5, 11, 14.7], "light"), cube([12.5, 2, 1.3], [14, 11, 14.7], "light"),
            cube([5, 12, 5], [11, 14, 11], "trim"), cube([5, 14, 5], [6.5, 17, 11], "accent"),
            cube([7.25, 14, 5], [8.75, 18, 11], "light"), cube([9.5, 14, 5], [11, 17, 11], "accent"),
            cube([6, 5, 1], [10, 10, 2.5], "body"), cube([7, 6, 0.5], [9, 9, 2], "light"),
        ]
    if kind == "valoria":
        return [
            cube([0.5, 1, 1.5], [15.5, 11, 14.5]), cube([-0.5, 10, 0.5], [16.5, 13, 15.5], "dark"),
            cube([1, 2, 1], [3, 12, 15], "trim"), cube([13, 2, 1], [15, 12, 15], "trim"),
            cube([0, 5, 1], [16, 7, 15], "accent"), cube([5, 12, 5], [11, 14, 11], "trim"),
            cube([4, 14, 5], [6, 18, 11], "trim", {
                "angle": -22.5, "axis": "z", "origin": [8, 14, 8], "rescale": True,
            }),
            cube([7, 14, 5], [9, 19, 11], "light"),
            cube([10, 14, 5], [12, 18, 11], "trim", {
                "angle": 22.5, "axis": "z", "origin": [8, 14, 8], "rescale": True,
            }),
            cube([-1, 14, 7], [5, 15, 9], "accent", {
                "angle": -22.5, "axis": "z", "origin": [8, 14, 8], "rescale": True,
            }),
            cube([11, 14, 7], [17, 15, 9], "accent", {
                "angle": 22.5, "axis": "z", "origin": [8, 14, 8], "rescale": True,
            }),
            cube([5, 5, 0.5], [11, 11, 2], "body"), cube([7, 6, 0], [9, 9, 1.5], "light"),
        ]
    return [
        cube([2, 1, 2], [14, 10, 14]), cube([1, 9, 1], [15, 12, 15], "trim"),
        cube([2, 11, 4], [6, 16, 8], "accent", {
            "angle": -22.5, "axis": "z", "origin": [5, 12, 8], "rescale": True,
        }),
        cube([10, 11, 4], [14, 16, 8], "accent", {
            "angle": 22.5, "axis": "z", "origin": [11, 12, 8], "rescale": True,
        }),
        cube([5, 5, 1], [11, 10, 2.5], "body"), cube([7, 6, 0.5], [9, 8, 2], "light"),
        cube([1, 3, 5], [3, 8, 11], "accent"), cube([13, 3, 5], [15, 8, 11], "trim"),
    ]


def write_crate_model(
    path: str,
    kind: str,
    pixels: list[tuple[int, int, int, int]],
) -> None:
    item_path = ROOT / "items" / f"{path}.json"
    model_path = ROOT / "models" / "item" / f"{path}.json"
    texture_path = ROOT / "textures" / "item" / f"{path}.png"
    item_path.parent.mkdir(parents=True, exist_ok=True)
    model_path.parent.mkdir(parents=True, exist_ok=True)
    item_path.write_text(json.dumps({
        "model": {"type": "minecraft:model", "model": f"valoriatycoon:item/{path}"},
    }, indent=2) + "\n")
    model_path.write_text(json.dumps({
        "parent": "minecraft:block/block",
        "textures": {"particle": f"valoriatycoon:item/{path}", "crate": f"valoriatycoon:item/{path}"},
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.72, 0.72, 0.72]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1.15, 1.15, 1.15]},
        },
        "elements": crate_elements(kind),
    }, indent=2) + "\n")
    png(texture_path, pixels)


def write_model(path: str, pixels: list[tuple[int, int, int, int]]) -> None:
    item_path = ROOT / "items" / f"{path}.json"
    model_path = ROOT / "models" / "item" / f"{path}.json"
    texture_path = ROOT / "textures" / "item" / f"{path}.png"
    item_path.parent.mkdir(parents=True, exist_ok=True)
    model_path.parent.mkdir(parents=True, exist_ok=True)
    item_path.write_text(json.dumps({"model": {"type": "minecraft:model", "model": f"valoriatycoon:item/{path}"}}, indent=2) + "\n")
    model_path.write_text(json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": f"valoriatycoon:item/{path}"}}, indent=2) + "\n")
    png(texture_path, pixels)


def clean_generated_assets() -> None:
    # Preserve the two hand-authored pet egg models at the namespace root.
    for relative in ("items/ui", "items/item", "models/item/ui", "models/item/item", "textures/item/ui", "textures/item/item"):
        shutil.rmtree(ROOT / relative, ignore_errors=True)


def main() -> None:
    clean_generated_assets()
    write_model("ui/filler", filler_tile())
    icons: dict[str, tuple[str, tuple[int, int, int, int]]] = {
        "balance": ("coin", VIOLET), "stats": ("bars", BLUE), "farm": ("wheat", (41, 104, 62, 255)),
        "machines": ("gear", (102, 85, 68, 255)), "upgrades": ("arrow", PURPLE),
        "tool_upgrades": ("pick", BLUE), "autosell": ("arrows", ORANGE), "settings": ("gear", GRAY),
        "leaderboards": ("crown", BLUE), "pets": ("paw", PURPLE),
        "quests": ("scroll", RED), "ranks": ("crown", ORANGE),
    }
    for name, (mark, base) in icons.items(): write_model(f"ui/main/{name}", ui_icon(mark, base))

    for name, mark, base in (
        ("mine", "pick", BLUE), ("fields", "wheat", GREEN), ("fishing", "fish", BLUE), ("forest", "tree", GREEN)
    ): write_model(f"ui/farm/{name}", ui_icon(mark, base))
    zone_data = {
        "charbon": ("C", GRAY), "fer_cuivre": ("FC", ORANGE), "or_redstone_lapis": ("OR", RED),
        "diamant_emeraude": ("DE", CYAN), "ble": ("B", GREEN), "carottes": ("CA", ORANGE),
        "pommes_de_terre": ("PT", ORANGE), "betteraves": ("BE", RED), "chene": ("C", GREEN),
        "bouleau": ("BO", WHITE), "sapin": ("S", GREEN), "chene_noir": ("CN", VIOLET),
    }
    for name, (mark, base) in zone_data.items(): write_model(f"ui/farm/zone/{name}", ui_icon(mark, base))
    write_model("ui/status/locked", ui_icon("lock", DARK, RED))

    for name, mark, base in (
        ("locked", "lock", DARK), ("enabled", "arrows", GREEN), ("disabled", "hopper", GRAY),
        ("upgrade", "arrow", ORANGE), ("maximum", "spark", PURPLE)
    ): write_model(f"ui/autosell/{name}", ui_icon(mark, base))

    for name, mark in (("plot_size", "arrow"), ("hopper_limit", "hopper"), ("member_limit", "members")):
        write_model(f"ui/upgrade/{name}", ui_icon(mark, PURPLE))

    rank_colors = [GRAY, ORANGE, GOLD, BLUE, CYAN, GREEN, PURPLE, RED, ORANGE, (211, 170, 55, 255)]
    for level, base in enumerate(rank_colors, 1):
        c = medallion(base)
        symbol(c, "crown", LIGHT)
        c.set(3 + min(level, 9), 13, CYAN if level < 10 else WHITE)
        write_model(f"ui/rank/{level}", c.pixels)
    write_model("ui/rank/maximum", ui_icon("crown", PURPLE, WHITE))

    for name, mark, base in (
        ("money", "coin", ORANGE), ("island_level", "arrow", GREEN),
        ("rank", "crown", PURPLE), ("production", "gear", BLUE),
        ("playtime", "clock", (62, 91, 135, 255)),
    ):
        write_model(f"ui/leaderboard/{name}", ui_icon(mark, base))
    for name, mark, base in (
        ("gold", "1", (194, 132, 34, 255)), ("silver", "2", GRAY),
        ("bronze", "3", (152, 83, 48, 255)), ("standard", "T", VIOLET),
    ):
        write_model(f"ui/leaderboard/entry/{name}", ui_icon(mark, base))
    write_model("ui/leaderboard/me", ui_icon("M", CYAN))
    write_model("ui/leaderboard/updated", ui_icon("clock", BLUE))
    write_model("ui/leaderboard/back", ui_icon("back", VIOLET))
    write_model("ui/warp/tutorial", ui_icon("scroll", (46, 112, 150, 255), GOLD))
    write_model("ui/warp/crates", ui_icon("chest", PURPLE, GOLD))

    pets = {
        "rabbit_farmer": ("rabbit", GREEN), "fox_prospector": ("fox", ORANGE), "miner_wolf": ("wolf", BLUE),
        "golden_bee": ("bee", ORANGE), "allay_collector": ("allay", BLUE), "industrial_golem": ("golem", GRAY),
        "miniature_dragon": ("dragon", GREEN), "phoenix": ("phoenix", RED),
    }
    for pet, (kind, base) in pets.items(): write_model(f"ui/pet/{pet}", animal_icon(kind, base))
    write_model("ui/pet/crate", ui_icon("chest", PURPLE))

    write_model("ui/payment/money", ui_icon("coin", ORANGE))
    write_model("ui/payment/coins", ui_icon("spark", GREEN))

    machine_colors = {"miner": BLUE, "farmer": GREEN, "lumber": ORANGE, "fisher": CYAN}
    for machine, base in machine_colors.items(): write_model(f"ui/machine/{machine}", generator_icon(machine, base))
    for name, mark, base in (
        ("collect", "chest", ORANGE), ("output", "spark", BLUE), ("speed", "speed", CYAN), ("sell_price", "coin", GREEN)
    ): write_model(f"ui/machine/{name}", ui_icon(mark, base))

    tool_colors = {"pickaxe": BLUE, "hoe": GREEN, "axe": ORANGE, "fishing_rod": CYAN}
    for tool, base in tool_colors.items(): write_model(f"ui/tool/{tool}", tool_icon(tool, base))
    write_model("ui/tool/info", ui_icon("I", BLUE))
    capabilities = [
        "efficiency", "level_boost", "money_boost", "coin_boost", "speed_burst", "area_mining",
        "ore_fortune", "auto_smelt", "gem_finder", "mine_coin_finder", "area_harvest",
        "harvest_fortune", "auto_replant", "seed_finder", "farm_coin_finder", "ufo_harvest",
        "timber", "wood_fortune", "apple_finder", "wood_coin_finder", "double_catch",
        "treasure_luck", "rare_catch", "fish_coin_finder", "farm_key_finder", "crate_key_finder",
    ]
    for index, capability in enumerate(capabilities):
        abbreviation = "".join(part[0] for part in capability.split("_"))[:2]
        palette = (BLUE, GREEN, ORANGE, PURPLE)[index % 4]
        write_model(f"ui/tool/capability/{capability}", ui_icon(abbreviation, palette))

    for tool, base in tool_colors.items(): write_model(f"ui/quest/tool/{tool}", tool_icon(tool, base))
    for rarity, base in (("common", GRAY), ("rare", BLUE), ("epic", PURPLE), ("legendary", ORANGE)):
        write_model(f"ui/quest/summary/{rarity}", ui_icon(rarity[0].upper(), base))

    write_model("item/key/pet_crate", ui_icon("K", (151, 65, 171, 255), CYAN))
    for crate, mark, base, edge in (
        ("vote", "V", (30, 170, 185, 255), WHITE),
        ("quest", "Q", (48, 118, 168, 255), CYAN),
        ("farm", "F", (142, 92, 34, 255), GREEN),
        ("common", "C", (39, 145, 72, 255), WHITE),
        ("rare", "R", BLUE, CYAN),
        ("epic", "E", (222, 91, 20, 255), GOLD),
        ("legendary", "L", (228, 178, 26, 255), WHITE),
        ("valoria", "crown", (158, 17, 38, 255), GOLD),
    ):
        write_model(f"item/key/crate_{crate}", ui_icon(mark, base, edge))
    for crate, base, trim, accent in (
        ("vote", (19, 119, 139, 255), (69, 231, 239, 255), WHITE),
        ("quest", (31, 83, 128, 255), CYAN, (248, 218, 132, 255)),
        ("farm", (111, 65, 29, 255), GOLD, (72, 190, 82, 255)),
        ("common", (25, 112, 54, 255), (72, 221, 105, 255), (202, 224, 208, 255)),
        ("rare", (24, 61, 153, 255), (61, 139, 255, 255), (111, 227, 255, 255)),
        ("epic", (92, 31, 20, 255), (255, 120, 24, 255), (255, 196, 57, 255)),
        ("legendary", (163, 113, 10, 255), (255, 211, 45, 255), (255, 248, 178, 255)),
        ("valoria", (116, 8, 27, 255), (255, 186, 39, 255), (255, 46, 75, 255)),
        ("pets", (111, 44, 130, 255), (246, 105, 179, 255), (83, 231, 238, 255)),
    ):
        write_crate_model(
            f"item/crate/{crate}",
            crate,
            crate_texture(crate, base, trim, accent),
        )
    for tier in range(1, 6):
        write_model(
            f"item/reward/money_bag/{tier}",
            reward_bag_icon((142, 83, 34, 255), GOLD, tier, "money"),
        )
        write_model(
            f"item/reward/coin_bag/{tier}",
            reward_bag_icon((42, 126, 74, 255), (103, 238, 137, 255), tier, "coin"),
        )
    write_model(
        "item/reward/coin_bag/universal",
        reward_bag_icon((91, 43, 130, 255), CYAN, 5, "coin"),
    )
    for tier in range(1, 5):
        write_model(f"item/reward/xp_vial/{tier}", reward_vial_icon(tier))
    for tier in range(1, 4):
        write_model(f"item/reward/resource_bundle/{tier}", reward_bundle_icon(tier))
    for voucher, mark, base in (
        ("item", "chest", ORANGE),
        ("generator", "gear", BLUE),
        ("key", "K", GOLD),
        ("pet_key", "paw", PURPLE),
    ):
        write_model(f"item/reward/voucher/{voucher}", reward_voucher_icon(mark, base))

    for machine, base in machine_colors.items(): write_model(f"item/generator/{machine}", generator_icon(machine, base))

    compact_colors = {
        "coal": (55, 59, 67, 255), "copper_ingot": (203, 116, 75, 255), "iron_ingot": (197, 204, 204, 255),
        "gold_ingot": (241, 190, 48, 255), "redstone": (193, 40, 45, 255), "lapis_lazuli": (49, 82, 180, 255),
        "diamond": (72, 218, 207, 255), "emerald": (46, 190, 100, 255), "wheat": (218, 181, 69, 255),
        "carrot": (224, 121, 39, 255), "potato": (177, 137, 68, 255), "beetroot": (166, 48, 74, 255),
        "oak_log": (142, 105, 61, 255), "birch_log": (210, 198, 151, 255), "spruce_log": (91, 66, 43, 255),
        "dark_oak_log": (62, 42, 30, 255),
    }
    for material, color in compact_colors.items():
        for level in range(1, 4): write_model(f"item/compact/{material}/{level}", compact_icon(color, level))

    rank_themes = [
        # metal, edge, shadow, handle, handle highlight, wrap, heraldic gem
        {"metal": (126, 83, 43, 255), "edge": (174, 126, 70, 255), "shadow": (69, 43, 29, 255), "handle": (75, 47, 30, 255), "handle_light": (128, 82, 44, 255), "wrap": GRAY, "gem": (111, 101, 87, 255)},
        {"metal": (156, 104, 53, 255), "edge": (213, 165, 79, 255), "shadow": (88, 51, 30, 255), "handle": (91, 55, 31, 255), "handle_light": (153, 97, 47, 255), "wrap": (49, 137, 74, 255), "gem": (75, 205, 105, 255)},
        {"metal": (126, 132, 137, 255), "edge": (207, 130, 78, 255), "shadow": (68, 72, 80, 255), "handle": (93, 58, 34, 255), "handle_light": (151, 95, 52, 255), "wrap": (190, 101, 61, 255), "gem": (102, 211, 221, 255)},
        {"metal": (108, 122, 142, 255), "edge": GOLD, "shadow": (53, 61, 79, 255), "handle": (80, 43, 34, 255), "handle_light": (143, 76, 47, 255), "wrap": (37, 145, 86, 255), "gem": (55, 221, 118, 255)},
        {"metal": (190, 199, 207, 255), "edge": WHITE, "shadow": (93, 107, 124, 255), "handle": (44, 55, 82, 255), "handle_light": (91, 111, 153, 255), "wrap": (50, 100, 203, 255), "gem": (80, 160, 247, 255)},
        {"metal": (210, 218, 224, 255), "edge": WHITE, "shadow": (79, 88, 104, 255), "handle": (69, 42, 34, 255), "handle_light": (132, 76, 51, 255), "wrap": (173, 42, 57, 255), "gem": (238, 73, 83, 255)},
        {"metal": (235, 177, 43, 255), "edge": (255, 229, 125, 255), "shadow": (151, 86, 29, 255), "handle": (92, 37, 31, 255), "handle_light": (158, 66, 43, 255), "wrap": (174, 42, 55, 255), "gem": (238, 56, 67, 255)},
        {"metal": (239, 197, 76, 255), "edge": (255, 239, 168, 255), "shadow": (145, 96, 34, 255), "handle": (64, 32, 70, 255), "handle_light": (121, 56, 126, 255), "wrap": (119, 57, 190, 255), "gem": (193, 91, 245, 255)},
        {"metal": (71, 207, 208, 255), "edge": (187, 255, 248, 255), "shadow": (33, 103, 147, 255), "handle": (30, 48, 77, 255), "handle_light": (54, 91, 137, 255), "wrap": (41, 91, 204, 255), "gem": (81, 143, 255, 255)},
        {"metal": (102, 224, 220, 255), "edge": GOLD, "shadow": (39, 122, 160, 255), "handle": (62, 35, 82, 255), "handle_light": (119, 63, 139, 255), "wrap": (224, 174, 52, 255), "gem": (178, 77, 232, 255)},
        {"metal": (83, 67, 81, 255), "edge": GOLD, "shadow": (30, 24, 37, 255), "handle": (29, 24, 34, 255), "handle_light": (83, 67, 81, 255), "wrap": CYAN, "gem": (102, 242, 231, 255)},
    ]
    for rank, theme in enumerate(rank_themes):
        for tool in tool_colors:
            write_model(
                f"item/multitool/rank/{rank}/{tool}",
                ranked_tool_icon(tool, theme, rank),
            )

    pack_icon(ui_icon("crown", PURPLE, WHITE))
    generated = sum(1 for _ in (ROOT / "items").rglob("*.json"))
    print(f"Generated {generated} ValoriaTycoon item models in {ROOT}")


if __name__ == "__main__":
    main()
