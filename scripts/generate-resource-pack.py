#!/usr/bin/env python3
"""Generate ValoriaTycoon's deterministic premium 32x32 medieval-fantasy resource pack."""

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
    def __init__(self, width: int = 16, height: int | None = None) -> None:
        self.width = width
        self.height = width if height is None else height
        self.pixels = [TRANSPARENT] * (self.width * self.height)

    def set(self, x: int, y: int, color: tuple[int, int, int, int]) -> None:
        if 0 <= x < self.width and 0 <= y < self.height:
            self.pixels[y * self.width + x] = color

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


def tint(
    color: tuple[int, int, int, int],
    amount: int,
) -> tuple[int, int, int, int]:
    """Move an opaque pixel toward white/black without changing its alpha."""
    red, green, blue, alpha = color
    if amount >= 0:
        return (
            red + (255 - red) * amount // 100,
            green + (255 - green) * amount // 100,
            blue + (255 - blue) * amount // 100,
            alpha,
        )
    factor = 100 + amount
    return (red * factor // 100, green * factor // 100, blue * factor // 100, alpha)


def logical(pixels: list[tuple[int, int, int, int]], x: int, y: int) -> tuple[int, int, int, int]:
    return pixels[y * 16 + x] if 0 <= x < 16 and 0 <= y < 16 else TRANSPARENT


def premium_pixels(
    pixels: list[tuple[int, int, int, int]],
) -> list[tuple[int, int, int, int]]:
    """Scale logical 16px art to detailed 32px RPG pixel art with bevels and cast shadows."""
    if len(pixels) != 256:
        raise ValueError("Premium source textures must use the 16x16 logical canvas")
    output = [TRANSPARENT] * (32 * 32)
    for y in range(16):
        for x in range(16):
            center = logical(pixels, x, y)
            if center[3] == 0:
                continue
            north = logical(pixels, x, y - 1)
            west = logical(pixels, x - 1, y)
            east = logical(pixels, x + 1, y)
            south = logical(pixels, x, y + 1)
            # Scale2x preserves sharp corners while rounding only intentional diagonals.
            quadrants = (
                west if west == north and west != south and north != east else center,
                east if north == east and north != west and east != south else center,
                west if west == south and west != north and south != east else center,
                east if south == east and west != south and north != east else center,
            )
            for sy in range(2):
                for sx in range(2):
                    ox, oy = x * 2 + sx, y * 2 + sy
                    source = quadrants[sy * 2 + sx]
                    light = (8, 3, -3, -10)[sy * 2 + sx]
                    if north[3] == 0 and sy == 0:
                        light += 9
                    if west[3] == 0 and sx == 0:
                        light += 5
                    if south[3] == 0 and sy == 1:
                        light -= 7
                    if east[3] == 0 and sx == 1:
                        light -= 5
                    # Deterministic restrained grain prevents large surfaces looking plastic.
                    grain = ((x * 13 + y * 7 + sx * 3 + sy * 5) % 5) - 2
                    output[oy * 32 + ox] = tint(source, light + grain)

    # One sub-pixel cast shadow anchors item sprites without blurring their silhouette.
    shadow = (5, 7, 13, 145)
    original = list(output)
    for y in range(31, -1, -1):
        for x in range(31, -1, -1):
            if original[y * 32 + x][3] != 0:
                continue
            neighbours = ((x - 1, y - 1), (x, y - 1), (x - 1, y))
            if any(0 <= nx < 32 and 0 <= ny < 32 and original[ny * 32 + nx][3] > 220
                   for nx, ny in neighbours):
                output[y * 32 + x] = shadow
    return output


def write_png(
    path: Path,
    width: int,
    height: int,
    pixels: list[tuple[int, int, int, int]],
) -> None:
    def chunk(kind: bytes, data: bytes) -> bytes:
        checksum = zlib.crc32(kind + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", checksum)

    if len(pixels) != width * height:
        raise ValueError("PNG pixel count does not match its dimensions")
    rows = []
    for y in range(height):
        row = bytearray()
        for x in range(width):
            row.extend(pixels[y * width + x])
        rows.append(b"\x00" + bytes(row))
    data = b"\x89PNG\r\n\x1a\n"
    data += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(b"".join(rows), 9))
    data += chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def png(path: Path, pixels: list[tuple[int, int, int, int]]) -> None:
    write_png(path, 32, 32, premium_pixels(pixels))


def pack_icon(pixels: list[tuple[int, int, int, int]]) -> None:
    source = premium_pixels(pixels)
    output = []
    for y in range(64):
        for x in range(64):
            pixel = source[(y // 2) * 32 + x // 2]
            distance = abs(x - 31.5) + abs(y - 31.5)
            background = tint(VIOLET if (x // 8 + y // 8) % 2 else DARK, int(max(-8, 10 - distance / 3)))
            output.append(pixel if pixel[3] else background)
    write_png(ROOT.parents[1] / "pack.png", 64, 64, output)


def gui_slot_pixels(theme: str = "player") -> list[tuple[int, int, int, int]]:
    c = Canvas(18)
    if theme == "storage":
        rim, inner, high, corner = (
            (105, 48, 66, 255), (43, 20, 30, 255), (173, 79, 87, 255), (190, 118, 46, 255)
        )
    else:
        rim, inner, high, corner = (
            (82, 42, 111, 255), (24, 20, 45, 255), (139, 72, 162, 255), (113, 68, 137, 255)
        )
    c.rect(0, 0, 17, 17, (7, 5, 12, 255))
    c.rect(1, 1, 16, 16, rim)
    c.rect(2, 2, 15, 15, inner)
    c.line(2, 2, 15, 2, high); c.line(2, 2, 2, 15, tint(high, -18))
    c.line(2, 15, 15, 15, (8, 6, 13, 255)); c.line(15, 2, 15, 15, tint(rim, -45))
    c.set(3, 3, tint(GOLD, -12) if theme == "storage" else tint(PURPLE, 28))
    c.set(14, 14, corner)
    return c.pixels


def gui_container_pixels() -> list[tuple[int, int, int, int]]:
    """Full vanilla-compatible 6-row container atlas with a Hyping-scale Valoria facade."""
    c = Canvas(256)
    plum = (72, 27, 91, 255)
    violet = (132, 53, 154, 255)
    bright = (200, 93, 203, 255)
    recess = (37, 15, 42, 255)
    lower = (26, 25, 54, 255)
    bronze = (190, 120, 39, 255)
    red_banner = (169, 30, 44, 255)

    # Main panel and heavy drop shadow.
    c.rect(3, 3, 178, 224, (5, 4, 9, 210))
    c.rect(0, 0, 175, 221, INK)
    c.rect(2, 2, 173, 219, plum)
    c.rect(6, 5, 169, 216, recess)

    # Architectural gold/violet columns, segmented like a fantasy palace facade.
    for left in (0, 165):
        c.rect(left, 13, left + 10, 212, (39, 16, 52, 255))
        c.rect(left + 1, 16, left + 4, 208, violet)
        c.line(left + 5, 16, left + 5, 208, bright)
        c.rect(left + 6, 16, left + 8, 208, tint(plum, -18))
        c.line(left + 9, 16, left + 9, 208, INK)
        for y in range(21, 207, 18):
            c.line(left + 2, y, left + 7, y + 5, tint(GOLD, -8))
            c.set(left + 2, y + 1, LIGHT)
        for y in (13, 122, 126, 207):
            c.rect(left, y, left + 10, y + 4, bronze)
            c.line(left + 1, y, left + 9, y, tint(GOLD, 24))
            c.set(left + 2, y + 1, LIGHT); c.set(left + 8, y + 3, tint(bronze, -32))
    c.rect(0, 215, 12, 221, bronze); c.rect(163, 215, 175, 221, bronze)
    c.rect(2, 214, 9, 218, violet); c.rect(166, 214, 173, 218, violet)
    c.set(1, 217, LIGHT); c.set(173, 217, LIGHT)

    # Layered pediment and broad red title ribbon behind the actual inventory title.
    c.rect(8, 0, 167, 16, (50, 18, 65, 255))
    c.rect(14, 0, 161, 3, violet)
    c.line(16, 0, 159, 0, bright); c.line(9, 15, 166, 15, bronze)
    c.rect(7, 3, 168, 14, bronze)
    c.rect(10, 4, 165, 13, red_banner)
    c.line(11, 4, 164, 4, (244, 91, 84, 255)); c.line(11, 13, 164, 13, (76, 9, 27, 255))
    c.rect(2, 5, 10, 11, bronze); c.rect(165, 5, 173, 11, bronze)
    c.set(4, 6, LIGHT); c.set(171, 6, LIGHT)
    # Crown finial.
    c.set(84, 1, GOLD); c.set(87, 0, LIGHT); c.set(90, 1, GOLD)

    # Upper storage recess. The vanilla renderer crops this area for 1–6 rows.
    c.rect(7, 17, 168, 124, (91, 31, 51, 255))
    c.rect(9, 19, 166, 122, (57, 23, 37, 255))
    c.line(9, 19, 166, 19, (181, 72, 83, 255))
    c.line(9, 122, 166, 122, (25, 9, 19, 255))
    storage_slot = gui_slot_pixels("storage")
    for row in range(6):
        for column in range(9):
            ox, oy = 7 + column * 18, 17 + row * 18
            for sy in range(18):
                for sx in range(18):
                    c.set(ox + sx, oy + sy, storage_slot[sy * 18 + sx])

    # Player inventory is sampled by vanilla from y=126 regardless of container row count.
    c.rect(0, 126, 175, 221, lower)
    c.rect(6, 126, 169, 216, (35, 31, 67, 255))
    c.line(8, 127, 167, 127, bright); c.line(8, 215, 167, 215, (10, 8, 19, 255))
    # Discreet central royal crest between storage and player inventory.
    c.rect(75, 126, 100, 137, (47, 22, 65, 255))
    c.line(77, 127, 98, 127, tint(GOLD, -4)); c.set(87, 130, GOLD); c.set(88, 129, LIGHT)
    player_slot = gui_slot_pixels("player")
    for row in range(3):
        for column in range(9):
            ox, oy = 7 + column * 18, 139 + row * 18
            for sy in range(18):
                for sx in range(18):
                    c.set(ox + sx, oy + sy, player_slot[sy * 18 + sx])
    for column in range(9):
        ox, oy = 7 + column * 18, 197
        for sy in range(18):
            for sx in range(18):
                c.set(ox + sx, oy + sy, player_slot[sy * 18 + sx])

    # Small gems and filigree stop the frame looking like a flat rectangle.
    for x, y, color in ((5, 8, CYAN), (170, 8, RED), (4, 213, PURPLE), (171, 213, CYAN)):
        c.set(x, y, LIGHT); c.set(x + 1, y, color); c.set(x, y + 1, tint(color, -20))
    for y in range(24, 119, 16):
        c.set(4, y, GOLD); c.set(171, y + 7, tint(GOLD, -12))
    return c.pixels


def draw_pixel_text(
    canvas: Canvas,
    text: str,
    x: int,
    y: int,
    scale: int,
    color: tuple[int, int, int, int],
    shadow: tuple[int, int, int, int],
) -> None:
    cursor = x
    for character in text.upper():
        pattern = FONT.get(character)
        if pattern is None:
            cursor += 4 * scale
            continue
        for py, row in enumerate(pattern):
            for px, value in enumerate(row):
                if value != "1":
                    continue
                sx, sy = cursor + px * scale, y + py * scale
                canvas.rect(sx + scale // 2, sy + scale // 2, sx + scale - 1 + scale // 2,
                            sy + scale - 1 + scale // 2, shadow)
                canvas.rect(sx, sy, sx + scale - 1, sy + scale - 1, color)
                canvas.line(sx, sy, sx + scale - 1, sy, tint(color, 24))
        cursor += 6 * scale


def gui_header_pixels() -> list[tuple[int, int, int, int]]:
    """Floating castle header rendered by a private font glyph above every Valoria menu."""
    c = Canvas(176, 64)
    purple = (112, 42, 141, 255)
    bright = (184, 76, 194, 255)
    bronze = (190, 113, 34, 255)
    red = (164, 25, 41, 255)

    # Original server wordmark with a red extruded shadow, not Hyping artwork.
    text = "VALORIA"
    logo_width = len(text) * 18 - 3
    draw_pixel_text(c, text, (176 - logo_width) // 2, 0, 3, GOLD, (105, 21, 33, 255))

    # Two medieval towers and battlements framing the menu title.
    for left in (2, 156):
        c.rect(left, 27, left + 17, 63, INK)
        c.rect(left + 2, 28, left + 15, 62, purple)
        c.rect(left, 24, left + 5, 31, bronze); c.rect(left + 12, 24, left + 17, 31, bronze)
        c.rect(left + 5, 27, left + 12, 33, bright)
        c.line(left + 3, 35, left + 14, 35, tint(GOLD, 12))
        c.line(left + 4, 39, left + 13, 48, tint(bronze, -10))
        c.line(left + 13, 39, left + 4, 48, tint(GOLD, 12))
        c.rect(left + 5, 51, left + 12, 62, (51, 19, 66, 255))
        c.set(left + 7, 53, CYAN); c.set(left + 10, 53, RED)

    # Central palace plaque.
    c.rect(17, 28, 158, 55, INK)
    c.rect(20, 29, 155, 53, bronze)
    c.rect(23, 31, 152, 51, (60, 21, 76, 255))
    c.rect(27, 33, 148, 49, purple)
    c.line(28, 33, 147, 33, bright); c.line(28, 49, 147, 49, (42, 13, 55, 255))
    for x in (23, 34, 141, 152):
        c.set(x, 31, LIGHT); c.set(x, 51, tint(GOLD, -20))

    # Red heraldic ribbon sits precisely behind the normal inventory title baseline.
    c.rect(12, 50, 163, 62, bronze)
    c.rect(16, 51, 159, 61, red)
    c.line(17, 51, 158, 51, (242, 79, 78, 255))
    c.line(17, 61, 158, 61, (71, 8, 24, 255))
    c.line(6, 54, 16, 54, bronze); c.line(159, 54, 169, 54, bronze)
    c.set(8, 53, LIGHT); c.set(167, 53, LIGHT)
    return c.pixels


def generate_gui_textures() -> None:
    minecraft = ROOT.parent / "minecraft" / "textures" / "gui"
    write_png(minecraft / "container" / "generic_54.png", 256, 256, gui_container_pixels())
    slot = gui_slot_pixels()
    write_png(minecraft / "sprites" / "container" / "slot.png", 18, 18, slot)
    # Gold focus ring used by modern container screens where supported.
    focus = Canvas(18)
    for inset, color in ((0, (255, 226, 119, 210)), (1, (218, 157, 51, 180))):
        focus.line(inset, inset, 17 - inset, inset, color)
        focus.line(inset, 17 - inset, 17 - inset, 17 - inset, color)
        focus.line(inset, inset, inset, 17 - inset, color)
        focus.line(17 - inset, inset, 17 - inset, 17 - inset, color)
    write_png(minecraft / "sprites" / "container" / "slot_highlight_front.png", 18, 18, focus.pixels)

    write_png(ROOT / "textures" / "font" / "gui_header.png", 176, 64, gui_header_pixels())
    font_path = ROOT / "font" / "gui.json"
    font_path.parent.mkdir(parents=True, exist_ok=True)
    font_path.write_text(json.dumps({
        "providers": [
            {
                "type": "bitmap",
                "file": "valoriatycoon:font/gui_header.png",
                "height": 64,
                "ascent": 60,
                "chars": ["\ue000"],
            },
            {
                "type": "space",
                "advances": {"\ue001": -177},
            },
        ],
    }, indent=2) + "\n")


def small_container_base(height: int, storage: tuple[int, int, int, int]) -> Canvas:
    c = Canvas(256)
    c.rect(2, 2, 178, height + 3, (4, 3, 8, 180))
    c.rect(0, 0, 175, height - 1, INK)
    c.rect(2, 2, 173, height - 3, (74, 29, 92, 255))
    c.rect(6, 6, 169, height - 7, storage)
    c.line(3, 2, 172, 2, tint(GOLD, 12)); c.line(3, height - 3, 172, height - 3, (76, 40, 98, 255))
    for x in (1, 169):
        c.rect(x, 3, x + 5, height - 4, (101, 39, 124, 255))
        c.line(x + 1, 4, x + 1, height - 5, (196, 81, 199, 255))
        for y in range(10, height - 7, 18): c.set(x + 3, y, GOLD)
    return c


def paste_slot(canvas: Canvas, x: int, y: int, theme: str = "player") -> None:
    slot = gui_slot_pixels(theme)
    for sy in range(18):
        for sx in range(18):
            canvas.set(x + sx, y + sy, slot[sy * 18 + sx])


def draw_player_inventory(canvas: Canvas, first_y: int) -> None:
    for row in range(3):
        for column in range(9):
            paste_slot(canvas, 7 + column * 18, first_y + row * 18)
    for column in range(9):
        paste_slot(canvas, 7 + column * 18, first_y + 58)


def crafting_gui_pixels() -> list[tuple[int, int, int, int]]:
    c = small_container_base(166, (35, 19, 48, 255))
    c.rect(8, 16, 167, 77, (55, 23, 40, 255))
    for row in range(3):
        for column in range(3): paste_slot(c, 29 + column * 18, 16 + row * 18, "storage")
    paste_slot(c, 123, 34, "storage")
    # Forged-gold crafting arrow and small anvil crest.
    c.line(87, 41, 113, 41, tint(GOLD, -10)); c.line(108, 36, 113, 41, GOLD); c.line(108, 46, 113, 41, GOLD)
    c.set(92, 39, LIGHT); c.set(98, 43, (217, 75, 61, 255))
    draw_player_inventory(c, 83)
    return c.pixels


def inventory_gui_pixels() -> list[tuple[int, int, int, int]]:
    c = small_container_base(166, (31, 23, 48, 255))
    # Character preview alcove with armor columns.
    c.rect(25, 7, 79, 78, (12, 10, 20, 255)); c.rect(27, 9, 77, 76, (37, 28, 58, 255))
    c.line(27, 9, 77, 9, (127, 63, 149, 255)); c.line(27, 76, 77, 76, (9, 7, 15, 255))
    for row in range(4): paste_slot(c, 7, 7 + row * 18, "storage")
    paste_slot(c, 79, 61, "storage")
    # Personal 2x2 crafting altar and output.
    for row in range(2):
        for column in range(2): paste_slot(c, 97 + column * 18, 17 + row * 18, "storage")
    paste_slot(c, 151, 26, "storage")
    c.line(134, 34, 146, 34, GOLD); c.line(142, 30, 146, 34, LIGHT); c.line(142, 38, 146, 34, GOLD)
    draw_player_inventory(c, 83)
    return c.pixels


def furnace_gui_pixels() -> list[tuple[int, int, int, int]]:
    c = small_container_base(166, (33, 20, 43, 255))
    c.rect(8, 16, 167, 77, (47, 23, 35, 255))
    paste_slot(c, 55, 16, "storage"); paste_slot(c, 55, 52, "storage"); paste_slot(c, 115, 34, "storage")
    symbol_fire = ((77, 50), (74, 46), (80, 45), (76, 42), (84, 48))
    for x, y in symbol_fire: c.set(x, y, ORANGE)
    c.line(78, 34, 105, 34, GOLD); c.line(100, 30, 105, 34, LIGHT); c.line(100, 38, 105, 34, GOLD)
    draw_player_inventory(c, 83)
    return c.pixels


def hopper_gui_pixels() -> list[tuple[int, int, int, int]]:
    c = small_container_base(133, (31, 23, 48, 255))
    c.rect(25, 17, 151, 42, (48, 22, 58, 255))
    for column in range(5): paste_slot(c, 43 + column * 18, 19, "storage")
    draw_player_inventory(c, 50)
    return c.pixels


def shulker_gui_pixels() -> list[tuple[int, int, int, int]]:
    c = small_container_base(166, (47, 21, 55, 255))
    for row in range(3):
        for column in range(9): paste_slot(c, 7 + column * 18, 17 + row * 18, "storage")
    draw_player_inventory(c, 83)
    return c.pixels


def generate_secondary_gui_textures() -> None:
    minecraft = ROOT.parent / "minecraft" / "textures" / "gui" / "container"
    for name, pixels in (
        ("inventory", inventory_gui_pixels()),
        ("crafting_table", crafting_gui_pixels()),
        ("furnace", furnace_gui_pixels()),
        ("blast_furnace", furnace_gui_pixels()),
        ("smoker", furnace_gui_pixels()),
        ("hopper", hopper_gui_pixels()),
        ("shulker_box", shulker_gui_pixels()),
    ):
        write_png(minecraft / f"{name}.png", 256, 256, pixels)


def pixel_noise(seed: int, x: int, y: int) -> int:
    value = (seed * 1_103_515_245 + x * 73_856_093 + y * 19_349_663 + x * y * 83_492_791)
    return (value ^ value >> 13) & 0xFF


def natural_surface(base: tuple[int, int, int, int], seed: int, strength: int = 14) -> list[tuple[int, int, int, int]]:
    result = []
    for y in range(32):
        for x in range(32):
            amount = (pixel_noise(seed, x, y) % (strength * 2 + 1)) - strength
            # Broad 4px forms plus fine grain produce a detailed but seamless material.
            amount += (pixel_noise(seed + 31, x // 4, y // 4) % 13) - 6
            result.append(tint(base, amount))
    return result


def brick_surface(
    brick: tuple[int, int, int, int],
    mortar: tuple[int, int, int, int],
    seed: int,
    width: int = 8,
    height: int = 6,
) -> list[tuple[int, int, int, int]]:
    c = Canvas(32)
    c.rect(0, 0, 31, 31, mortar)
    for row, y in enumerate(range(0, 32, height)):
        offset = -(width // 2) if row % 2 else 0
        for x in range(offset, 32, width):
            x1, x2 = max(0, x + 1), min(31, x + width - 2)
            y1, y2 = y + 1, min(31, y + height - 2)
            shade = (pixel_noise(seed, x, y) % 21) - 10
            c.rect(x1, y1, x2, y2, tint(brick, shade))
            c.line(x1, y1, x2, y1, tint(brick, 18))
            c.line(x1, y2, x2, y2, tint(brick, -22))
            if x2 - x1 > 3: c.set(x1 + 2, y1 + 2, tint(brick, 28))
    return c.pixels


def plank_surface(base: tuple[int, int, int, int], seed: int) -> list[tuple[int, int, int, int]]:
    c = Canvas(32)
    for y in range(0, 32, 8):
        c.rect(0, y, 31, y + 7, tint(base, (pixel_noise(seed, 0, y) % 15) - 7))
        c.line(0, y, 31, y, tint(base, 24)); c.line(0, y + 7, 31, y + 7, tint(base, -30))
        split = (seed * 7 + y * 3) % 21 + 5
        c.line(split, y + 1, split, y + 6, tint(base, -23))
        for x in range(3 + y % 5, 32, 9): c.set(x, y + 3, tint(base, 14))
    return c.pixels


def log_surface(base: tuple[int, int, int, int], seed: int, top: bool) -> list[tuple[int, int, int, int]]:
    c = Canvas(32)
    if top:
        c.rect(0, 0, 31, 31, tint(base, -28)); c.rect(2, 2, 29, 29, base)
        for inset in (5, 9, 13):
            color = tint(base, 17 if inset % 2 else -17)
            c.line(inset, inset, 31 - inset, inset, color); c.line(inset, 31 - inset, 31 - inset, 31 - inset, color)
            c.line(inset, inset, inset, 31 - inset, color); c.line(31 - inset, inset, 31 - inset, 31 - inset, color)
        c.set(16, 16, tint(base, -35)); c.line(16, 16, 22, 12, tint(base, -24))
    else:
        for x in range(32):
            stripe = (x // 4 + seed) % 3
            c.line(x, 0, x, 31, tint(base, 18 - stripe * 17))
        for y in range(4, 32, 9): c.line(3, y, 12, y + 2, tint(base, -25))
    return c.pixels


def ore_surface(
    stone: tuple[int, int, int, int],
    ore: tuple[int, int, int, int],
    seed: int,
) -> list[tuple[int, int, int, int]]:
    pixels = natural_surface(stone, seed, 11)
    clusters = ((5, 5), (19, 4), (12, 14), (25, 18), (5, 25), (18, 27))
    for index, (cx, cy) in enumerate(clusters):
        for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 1), (1, 2), (2, 1)):
            x, y = cx + dx, cy + dy
            color = tint(ore, 28 if dx + dy <= 0 else -18 if dx + dy >= 2 else 5)
            pixels[y * 32 + x] = color
        if index % 2 == 0: pixels[cy * 32 + cx] = tint(ore, 52)
    return pixels


def leaf_surface(base: tuple[int, int, int, int], seed: int) -> list[tuple[int, int, int, int]]:
    result = natural_surface(base, seed, 20)
    for y in range(32):
        for x in range(32):
            value = pixel_noise(seed, x, y)
            if value % 17 == 0:
                result[y * 32 + x] = TRANSPARENT
            elif value % 11 == 0:
                result[y * 32 + x] = tint(base, 28)
    return result


def crop_stage_pixels(kind: str, stage: int, maximum: int) -> list[tuple[int, int, int, int]]:
    c = Canvas(32)
    height = 6 + stage * 23 // maximum
    green = (51, 139, 65, 255)
    ripe = {"wheat": GOLD, "carrots": ORANGE, "potatoes": (178, 137, 68, 255), "beetroots": RED}[kind]
    for stem_x in (5, 11, 17, 23, 28):
        top = 31 - height + (stem_x * 3 % 4)
        c.line(stem_x, 31, stem_x, top, tint(green, (stem_x % 3) * 8))
        for y in range(30, top, -5):
            c.set(stem_x - 1, y, green); c.set(stem_x + 1, y - 2, tint(green, 16))
        if stage == maximum:
            c.rect(stem_x - 1, top, stem_x + 1, top + 3, ripe); c.set(stem_x, top, LIGHT)
    return c.pixels


def resource_item_pixels(name: str, color: tuple[int, int, int, int]) -> list[tuple[int, int, int, int]]:
    c = Canvas()
    if name in {"diamond", "emerald", "lapis_lazuli", "redstone"}:
        symbol(c, "gem", color)
    elif name in {"coal", "raw_iron", "raw_copper", "raw_gold"}:
        for x, y in ((5, 5), (8, 4), (10, 8), (6, 10)):
            c.rect(x, y, x + 2, y + 2, tint(color, (x + y) % 24 - 8)); c.set(x, y, LIGHT)
    elif name.endswith("_ingot"):
        c.rect(3, 6, 12, 11, INK); c.rect(4, 5, 11, 10, color)
        c.line(4, 5, 11, 5, tint(color, 35)); c.line(5, 9, 10, 9, tint(color, -30))
    elif name == "wheat": symbol(c, "wheat", color)
    elif name == "carrot":
        c.line(6, 5, 8, 13, color); c.line(10, 5, 8, 13, tint(color, -20)); c.line(8, 5, 6, 2, GREEN); c.line(8, 5, 11, 3, GREEN)
    elif name == "potato":
        c.rect(4, 5, 11, 12, color); c.set(6, 6, LIGHT); c.set(10, 9, tint(color, -35))
    elif name == "beetroot":
        c.rect(4, 6, 11, 11, color); c.line(8, 6, 5, 2, GREEN); c.line(8, 6, 11, 3, GREEN)
    else:
        symbol(c, "fish", color)
    return c.pixels


def generate_world_textures() -> None:
    minecraft = ROOT.parent / "minecraft" / "textures"
    blocks = minecraft / "block"
    surfaces = {
        "stone": natural_surface((110, 111, 116, 255), 1),
        "deepslate": natural_surface((50, 52, 59, 255), 2),
        "andesite": natural_surface((125, 128, 130, 255), 3),
        "granite": natural_surface((145, 96, 79, 255), 4),
        "diorite": natural_surface((190, 187, 179, 255), 5),
        "tuff": natural_surface((93, 105, 96, 255), 6),
        "calcite": natural_surface((218, 214, 199, 255), 7, 8),
        "dripstone_block": natural_surface((127, 91, 70, 255), 8),
        "dirt": natural_surface((116, 78, 46, 255), 9),
        "grass_block_top": natural_surface((70, 139, 54, 255), 10),
        "moss_block": natural_surface((73, 113, 44, 255), 11),
        "stone_bricks": brick_surface((104, 108, 111, 255), (48, 50, 54, 255), 12),
        "mossy_stone_bricks": brick_surface((88, 104, 81, 255), (42, 48, 43, 255), 13),
        "deepslate_bricks": brick_surface((54, 52, 62, 255), (22, 20, 27, 255), 14),
        "deepslate_tiles": brick_surface((45, 43, 54, 255), (17, 15, 23, 255), 15, 8, 4),
        "mud_bricks": brick_surface((116, 82, 70, 255), (58, 43, 38, 255), 16),
        "bricks": brick_surface((151, 76, 60, 255), (69, 50, 47, 255), 17),
        "prismarine_bricks": brick_surface((76, 151, 145, 255), (31, 80, 82, 255), 18),
        "cobblestone": brick_surface((102, 104, 105, 255), (45, 46, 48, 255), 19, 7, 7),
        "oak_planks": plank_surface((145, 105, 57, 255), 20),
        "spruce_planks": plank_surface((91, 61, 38, 255), 21),
        "dark_oak_planks": plank_surface((57, 36, 27, 255), 22),
    }
    for name, pixels in surfaces.items(): write_png(blocks / f"{name}.png", 32, 32, pixels)

    # Grass side combines earthy strata and a rich top fringe.
    grass = list(surfaces["dirt"])
    for y in range(8):
        for x in range(32):
            if y < 4 + pixel_noise(23, x, 0) % 4: grass[y * 32 + x] = tint((68, 135, 52, 255), (x % 5) - 2)
    write_png(blocks / "grass_block_side.png", 32, 32, grass)

    stone, deep = (110, 111, 116, 255), (49, 51, 58, 255)
    ores = {
        "coal": (45, 48, 53, 255), "copper": (190, 105, 70, 255), "iron": (211, 194, 171, 255),
        "gold": GOLD, "redstone": RED, "lapis": BLUE, "diamond": CYAN, "emerald": GREEN,
    }
    for index, (name, color) in enumerate(ores.items(), 30):
        write_png(blocks / f"{name}_ore.png", 32, 32, ore_surface(stone, color, index))
        write_png(blocks / f"deepslate_{name}_ore.png", 32, 32, ore_surface(deep, color, index + 40))

    woods = {
        "oak": (128, 88, 46, 255), "birch": (205, 193, 145, 255),
        "spruce": (83, 54, 31, 255), "dark_oak": (54, 34, 24, 255),
    }
    for index, (name, color) in enumerate(woods.items(), 80):
        write_png(blocks / f"{name}_log.png", 32, 32, log_surface(color, index, False))
        write_png(blocks / f"{name}_log_top.png", 32, 32, log_surface(color, index, True))
    write_png(blocks / "stripped_oak_log.png", 32, 32, log_surface((167, 129, 76, 255), 90, False))
    write_png(blocks / "stripped_oak_log_top.png", 32, 32, log_surface((167, 129, 76, 255), 90, True))
    write_png(blocks / "stripped_dark_oak_log.png", 32, 32, log_surface((92, 67, 43, 255), 91, False))
    write_png(blocks / "stripped_dark_oak_log_top.png", 32, 32, log_surface((92, 67, 43, 255), 91, True))

    leaves = {"oak": (53, 126, 53, 255), "birch": (80, 143, 59, 255), "spruce": (43, 94, 62, 255), "dark_oak": (42, 101, 43, 255)}
    for index, (name, color) in enumerate(leaves.items(), 100):
        write_png(blocks / f"{name}_leaves.png", 32, 32, leaf_surface(color, index))

    for kind, maximum in (("wheat", 7), ("carrots", 3), ("potatoes", 3), ("beetroots", 3)):
        for stage in range(maximum + 1):
            write_png(blocks / f"{kind}_stage{stage}.png", 32, 32, crop_stage_pixels(kind, stage, maximum))

    solid_blocks = {
        "iron_block": (185, 192, 193, 255), "gold_block": (224, 172, 43, 255),
        "cut_copper": (180, 93, 61, 255), "white_terracotta": (191, 164, 151, 255),
        "sea_lantern": (166, 220, 207, 255), "quartz_block_side": (222, 218, 207, 255),
        "quartz_block_top": (234, 229, 217, 255), "hay_block_side": (182, 143, 35, 255),
        "hay_block_top": (211, 170, 42, 255),
    }
    for index, (name, color) in enumerate(solid_blocks.items(), 120):
        write_png(blocks / f"{name}.png", 32, 32, natural_surface(color, index, 7))
    write_png(blocks / "chiseled_stone_bricks.png", 32, 32, brick_surface((112, 114, 113, 255), (48, 50, 53, 255), 140, 16, 16))

    wool_colors = {"red": RED, "blue": BLUE, "yellow": GOLD, "green": GREEN, "orange": ORANGE, "purple": PURPLE}
    for index, (name, color) in enumerate(wool_colors.items(), 150):
        write_png(blocks / f"{name}_wool.png", 32, 32, natural_surface(color, index, 9))

    # Workstations use engraved medieval wood and forged stone rather than flat vanilla faces.
    crafting_top = Canvas(32); crafting_top.pixels = plank_surface((116, 72, 38, 255), 170)
    crafting_top.rect(3, 3, 28, 28, (62, 35, 24, 255)); crafting_top.rect(5, 5, 26, 26, (139, 89, 43, 255))
    for line in (12, 19):
        crafting_top.line(5, line, 26, line, tint(GOLD, -15)); crafting_top.line(line, 5, line, 26, tint(GOLD, -15))
    crafting_top.line(5, 5, 26, 5, LIGHT); crafting_top.set(16, 16, RED)
    write_png(blocks / "crafting_table_top.png", 32, 32, crafting_top.pixels)
    crafting_side = Canvas(32); crafting_side.pixels = plank_surface((105, 63, 34, 255), 171)
    crafting_side.rect(3, 4, 28, 25, (67, 37, 25, 255)); crafting_side.line(4, 5, 27, 5, GOLD)
    crafting_side.line(6, 22, 23, 9, (187, 197, 202, 255)); crafting_side.line(20, 8, 27, 15, (203, 118, 58, 255))
    write_png(blocks / "crafting_table_front.png", 32, 32, crafting_side.pixels)
    write_png(blocks / "crafting_table_side.png", 32, 32, crafting_side.pixels)

    furnace_side = brick_surface((94, 96, 101, 255), (38, 39, 44, 255), 172, 8, 8)
    write_png(blocks / "furnace_side.png", 32, 32, furnace_side)
    write_png(blocks / "furnace_top.png", 32, 32, natural_surface((105, 107, 111, 255), 173, 9))
    for active in (False, True):
        front = Canvas(32); front.pixels = list(furnace_side)
        front.rect(6, 11, 25, 27, (19, 16, 19, 255)); front.rect(8, 13, 23, 25, (45, 28, 25, 255))
        front.line(7, 10, 24, 10, (185, 130, 59, 255)); front.set(10, 7, LIGHT); front.set(21, 7, LIGHT)
        if active:
            front.rect(10, 18, 21, 24, ORANGE); front.line(12, 23, 16, 15, GOLD); front.line(19, 23, 16, 15, RED)
        write_png(blocks / ("furnace_front_on.png" if active else "furnace_front.png"), 32, 32, front.pixels)

    barrel = plank_surface((105, 65, 34, 255), 180)
    for x in (3, 27):
        for y in range(32): barrel[y * 32 + x] = tint(GOLD, -25)
    write_png(blocks / "barrel_side.png", 32, 32, barrel)
    write_png(blocks / "barrel_top.png", 32, 32, log_surface((122, 77, 39, 255), 181, True))
    write_png(blocks / "barrel_bottom.png", 32, 32, log_surface((96, 58, 31, 255), 182, True))

    items = minecraft / "item"
    resources = {
        "coal": (48, 52, 59, 255), "raw_iron": (189, 156, 132, 255), "raw_copper": (190, 102, 65, 255),
        "raw_gold": (223, 172, 63, 255), "iron_ingot": (206, 214, 216, 255),
        "copper_ingot": (200, 111, 69, 255), "gold_ingot": GOLD, "redstone": RED,
        "lapis_lazuli": BLUE, "diamond": CYAN, "emerald": GREEN, "wheat": GOLD,
        "carrot": ORANGE, "potato": (177, 137, 68, 255), "beetroot": RED,
        "cod": (116, 171, 183, 255), "salmon": (207, 104, 87, 255),
        "pufferfish": GOLD, "tropical_fish": ORANGE,
    }
    for name, color in resources.items(): png(items / f"{name}.png", resource_item_pixels(name, color))


def medallion(base: tuple[int, int, int, int], border: tuple[int, int, int, int] = GOLD) -> Canvas:
    """Valoria guild crest: obsidian backing, aged-metal rim and faceted colored enamel."""
    c = Canvas()
    outer = {
        1: (5, 10), 2: (3, 12), 3: (2, 13), 4: (1, 14), 5: (1, 14),
        6: (1, 14), 7: (1, 14), 8: (1, 14), 9: (2, 13), 10: (2, 13),
        11: (3, 12), 12: (4, 11), 13: (5, 10), 14: (7, 8),
    }
    mask = {(x, y) for y, bounds in outer.items() for x in range(bounds[0], bounds[1] + 1)}
    for x, y in mask:
        adjacent_void = any(
            (x + dx, y + dy) not in mask
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
        )
        near_edge = any(
            (x + dx * 2, y + dy * 2) not in mask
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
        )
        if adjacent_void:
            color = INK
        elif near_edge:
            color = border if x + y < 17 else tint(border, -24)
        else:
            # Diagonal enamel facets and a dark lower recess create material depth.
            color = tint(base, 12 if x + y < 14 else -13 if x + y > 19 else 0)
        c.set(x, y, color)
    c.line(4, 3, 11, 3, tint(border, 22))
    c.set(3, 5, LIGHT); c.set(12, 5, tint(border, -20))
    c.set(5, 12, tint(border, -10)); c.set(10, 12, tint(border, -28))
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
    elif name == "check":
        c.rect(4, 4, 11, 11, WHITE); c.rect(5, 5, 10, 10, tint(CYAN, -35))
        c.line(5, 8, 7, 10, color); c.line(7, 10, 11, 5, color); c.set(10, 5, LIGHT)
    elif name == "gem":
        c.line(8, 3, 12, 7, color); c.line(12, 7, 8, 12, tint(color, -20))
        c.line(8, 12, 4, 7, tint(color, -35)); c.line(4, 7, 8, 3, tint(color, 30))
        c.line(4, 7, 12, 7, LIGHT); c.line(8, 3, 8, 12, tint(color, 15))
        c.set(7, 5, WHITE)
    elif name == "flame":
        c.line(8, 3, 5, 9, GOLD); c.line(8, 3, 11, 9, color)
        c.line(5, 9, 8, 12, tint(ORANGE, -15)); c.line(11, 9, 8, 12, RED)
        c.line(8, 6, 7, 10, LIGHT); c.line(8, 6, 9, 10, GOLD); c.set(8, 11, WHITE)
    elif name == "shield":
        c.line(4, 4, 11, 4, color); c.line(4, 4, 5, 10, color); c.line(11, 4, 10, 10, tint(color, -25))
        c.line(5, 10, 8, 13, tint(color, -20)); c.line(10, 10, 8, 13, tint(color, -35))
        c.line(8, 4, 8, 12, LIGHT); c.set(6, 6, GOLD); c.set(10, 6, GOLD)
    elif name == "key":
        c.rect(4, 4, 8, 8, GOLD); c.rect(5, 5, 7, 7, INK)
        c.line(8, 8, 12, 12, color); c.set(10, 11, color); c.set(12, 10, color)
        c.set(4, 4, LIGHT)
    elif name == "apple":
        c.rect(5, 6, 10, 11, RED); c.rect(4, 7, 11, 10, RED)
        c.set(5, 6, LIGHT); c.set(8, 5, (105, 65, 34, 255)); c.set(9, 4, GREEN); c.set(10, 4, GREEN)
    elif name == "seed":
        c.line(5, 11, 10, 5, GREEN); c.rect(4, 8, 6, 10, GOLD); c.rect(9, 4, 11, 6, tint(GREEN, 20))
        c.set(8, 8, LIGHT)
    elif name == "ufo":
        c.rect(6, 4, 9, 6, CYAN); c.rect(4, 6, 11, 8, color); c.line(3, 8, 12, 8, GOLD)
        c.set(5, 9, CYAN); c.set(8, 10, CYAN); c.set(11, 9, CYAN)
    elif name == "replant":
        c.line(8, 5, 8, 12, GREEN); c.line(8, 7, 5, 5, GREEN); c.line(8, 9, 11, 7, GREEN)
        c.rect(4, 12, 12, 13, (101, 62, 33, 255)); c.set(8, 4, LIGHT)
    elif name == "grid":
        for x in (4, 8, 12): c.line(x, 4, x, 12, color)
        for y in (4, 8, 12): c.line(4, y, 12, y, color)
        c.set(8, 8, GOLD); c.set(5, 5, LIGHT); c.set(11, 11, tint(color, -30))
    elif name == "medal":
        c.line(6, 3, 8, 7, BLUE); c.line(10, 3, 8, 7, RED)
        c.rect(5, 7, 11, 12, INK); c.rect(6, 7, 10, 11, color)
        c.set(8, 8, LIGHT); c.set(7, 9, GOLD); c.set(9, 9, GOLD)
    elif name == "info":
        c.rect(5, 3, 10, 12, (228, 216, 179, 255)); c.line(6, 5, 9, 5, BLUE)
        c.line(6, 7, 9, 7, BLUE); c.line(6, 9, 8, 9, BLUE); c.set(10, 11, GOLD)
    elif name == "back":
        c.line(4, 8, 12, 8, color); c.line(4, 8, 8, 4, color); c.line(4, 8, 8, 12, color)
    else:
        glyph(c, name, color)


def ui_icon(symbol_name: str, base: tuple[int, int, int, int] = VIOLET, accent: tuple[int, int, int, int] = LIGHT) -> list[tuple[int, int, int, int]]:
    c = medallion(base)
    symbol(c, symbol_name, accent)
    return c.pixels


def zone_icon(name: str, base: tuple[int, int, int, int]) -> list[tuple[int, int, int, int]]:
    c = medallion(base, tint(GOLD, -18))
    if name == "charbon":
        for x, y in ((5, 7), (8, 5), (10, 9)):
            c.rect(x, y, x + 2, y + 2, (44, 48, 57, 255)); c.set(x, y, GRAY)
    elif name == "fer_cuivre":
        c.rect(4, 5, 9, 8, (205, 213, 219, 255)); c.line(4, 5, 9, 5, WHITE)
        c.rect(7, 9, 12, 12, (195, 103, 64, 255)); c.line(7, 9, 12, 9, (244, 157, 108, 255))
    elif name == "or_redstone_lapis":
        for x, color in ((4, GOLD), (8, RED), (11, BLUE)):
            c.rect(x, 6, x + 2, 10, tint(color, -15)); c.set(x + 1, 6, LIGHT)
    elif name == "diamant_emeraude":
        symbol(c, "gem", CYAN); c.set(11, 6, GREEN); c.set(12, 7, tint(GREEN, 30))
    elif name == "ble":
        symbol(c, "wheat", GOLD)
    elif name == "carottes":
        c.line(6, 5, 8, 12, ORANGE); c.line(10, 5, 8, 12, tint(ORANGE, -20))
        c.line(8, 5, 6, 3, GREEN); c.line(8, 5, 10, 3, tint(GREEN, 20))
    elif name == "pommes_de_terre":
        c.rect(5, 5, 11, 11, (166, 124, 63, 255)); c.rect(4, 7, 12, 10, (166, 124, 63, 255))
        c.set(6, 6, LIGHT); c.set(10, 9, (91, 66, 38, 255)); c.set(7, 11, (91, 66, 38, 255))
    elif name == "betteraves":
        c.rect(5, 6, 11, 11, (164, 31, 62, 255)); c.line(6, 7, 9, 11, tint(RED, 18))
        c.line(8, 6, 5, 3, GREEN); c.line(8, 6, 11, 3, tint(GREEN, 16))
    else:
        woods = {
            "chene": ((131, 91, 48, 255), GREEN),
            "bouleau": ((219, 207, 157, 255), WHITE),
            "sapin": ((87, 57, 34, 255), (42, 115, 62, 255)),
            "chene_noir": ((57, 36, 26, 255), PURPLE),
        }
        wood, leaf = woods[name]
        c.rect(4, 5, 11, 11, tint(wood, -25)); c.rect(5, 5, 10, 10, wood)
        c.rect(6, 6, 9, 9, tint(wood, 25)); c.rect(7, 7, 8, 8, tint(wood, -20))
        c.set(4, 4, leaf); c.set(11, 4, tint(leaf, 18)); c.set(12, 6, leaf)
    return c.pixels


def capability_icon(name: str, base: tuple[int, int, int, int]) -> list[tuple[int, int, int, int]]:
    symbols = {
        "efficiency": "speed", "level_boost": "arrow", "money_boost": "coin",
        "coin_boost": "coin", "speed_burst": "speed", "area_mining": "grid",
        "ore_fortune": "gem", "auto_smelt": "flame", "gem_finder": "gem",
        "mine_coin_finder": "coin", "area_harvest": "grid", "harvest_fortune": "spark",
        "auto_replant": "replant", "seed_finder": "seed", "farm_coin_finder": "coin",
        "ufo_harvest": "ufo", "timber": "tree", "wood_fortune": "spark",
        "apple_finder": "apple", "wood_coin_finder": "coin", "double_catch": "fish",
        "treasure_luck": "chest", "rare_catch": "gem", "fish_coin_finder": "coin",
        "farm_key_finder": "key", "crate_key_finder": "key",
    }
    c = medallion(base, GOLD if "key" in name or "fortune" in name else tint(base, 35))
    symbol(c, symbols[name], LIGHT if symbols[name] not in {"coin", "gem", "flame"} else base)
    if name.startswith("area_"):
        c.set(4, 4, GOLD); c.set(12, 4, GOLD); c.set(8, 12, GOLD)
    if name.startswith("double_"):
        c.set(4, 5, CYAN); c.set(11, 10, CYAN)
    if name.endswith("_finder"):
        c.set(12, 4, LIGHT); c.set(11, 5, GOLD)
    return c.pixels


def crate_key_icon(
    kind: str,
    base: tuple[int, int, int, int],
    edge: tuple[int, int, int, int],
) -> list[tuple[int, int, int, int]]:
    marks = {
        "vote": "check", "quest": "scroll", "farm": "wheat", "common": "shield",
        "rare": "gem", "epic": "flame", "legendary": "crown", "valoria": "crown",
        "pets": "paw",
    }
    c = medallion(base, edge)
    symbol(c, marks[kind], edge)
    # Key teeth at the bottom make these visually distinct from menu badges.
    c.line(7, 11, 10, 14, GOLD); c.set(9, 13, LIGHT); c.set(11, 13, GOLD)
    return c.pixels


def pet_egg_icon(chromatic: bool) -> list[tuple[int, int, int, int]]:
    c = Canvas()
    outline = (30, 19, 43, 255)
    shell = (221, 211, 230, 255) if not chromatic else (109, 66, 161, 255)
    glow = (137, 235, 240, 255) if not chromatic else (255, 104, 186, 255)
    for y, bounds in {2: (7, 8), 3: (5, 10), 4: (4, 11), 5: (3, 12), 6: (3, 12),
                      7: (2, 13), 8: (2, 13), 9: (2, 13), 10: (3, 12), 11: (4, 11),
                      12: (5, 10), 13: (7, 8)}.items():
        for x in range(bounds[0], bounds[1] + 1):
            boundary = x in bounds or y in (2, 13)
            c.set(x, y, outline if boundary else tint(shell, 13 if x + y < 15 else -12))
    c.line(5, 6, 8, 9, glow); c.line(8, 9, 11, 5, tint(glow, 22))
    c.set(6, 4, WHITE); c.set(5, 5, LIGHT)
    if chromatic:
        for x, y, color in ((4, 8, CYAN), (11, 9, GOLD), (7, 11, RED), (10, 6, WHITE)):
            c.set(x, y, color)
    else:
        c.set(10, 10, PURPLE); c.set(11, 11, PURPLE)
    return c.pixels


def filler_tile() -> list[tuple[int, int, int, int]]:
    c = Canvas()
    c.rect(0, 0, 15, 15, (16, 12, 26, 255))
    c.rect(1, 1, 14, 14, (31, 19, 48, 255))
    c.line(1, 1, 14, 1, (94, 53, 116, 255)); c.line(1, 14, 14, 14, (8, 7, 14, 255))
    c.line(1, 1, 1, 14, (74, 42, 93, 255)); c.line(14, 1, 14, 14, INK)
    # Subtle royal damask rather than an empty black pane.
    c.line(4, 8, 8, 4, (45, 27, 66, 255)); c.line(8, 4, 12, 8, (45, 27, 66, 255))
    c.line(4, 8, 8, 12, (38, 23, 57, 255)); c.line(12, 8, 8, 12, (38, 23, 57, 255))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        c.set(x, y, tint(GOLD, -22))
    c.set(8, 7, tint(PURPLE, 20)); c.set(7, 8, tint(PURPLE, -8))
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
        symbol(c, "shield", trim)
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
    generate_gui_textures()
    generate_secondary_gui_textures()
    generate_world_textures()
    # Root pet-egg definitions are hand-authored, but their premium textures are deterministic too.
    png(ROOT / "textures" / "item" / "pet_egg.png", pet_egg_icon(False))
    png(ROOT / "textures" / "item" / "pet_egg_chromatic.png", pet_egg_icon(True))
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
        "charbon": GRAY, "fer_cuivre": ORANGE, "or_redstone_lapis": RED,
        "diamant_emeraude": CYAN, "ble": GREEN, "carottes": ORANGE,
        "pommes_de_terre": (161, 117, 63, 255), "betteraves": RED, "chene": GREEN,
        "bouleau": WHITE, "sapin": (44, 108, 60, 255), "chene_noir": VIOLET,
    }
    for name, base in zone_data.items():
        write_model(f"ui/farm/zone/{name}", zone_icon(name, base))
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
        ("gold", "medal", (194, 132, 34, 255)), ("silver", "medal", GRAY),
        ("bronze", "medal", (152, 83, 48, 255)), ("standard", "bars", VIOLET),
    ):
        write_model(f"ui/leaderboard/entry/{name}", ui_icon(mark, base))
    write_model("ui/leaderboard/me", ui_icon("members", CYAN))
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
    write_model("ui/tool/info", ui_icon("info", BLUE))
    capabilities = [
        "efficiency", "level_boost", "money_boost", "coin_boost", "speed_burst", "area_mining",
        "ore_fortune", "auto_smelt", "gem_finder", "mine_coin_finder", "area_harvest",
        "harvest_fortune", "auto_replant", "seed_finder", "farm_coin_finder", "ufo_harvest",
        "timber", "wood_fortune", "apple_finder", "wood_coin_finder", "double_catch",
        "treasure_luck", "rare_catch", "fish_coin_finder", "farm_key_finder", "crate_key_finder",
    ]
    for index, capability in enumerate(capabilities):
        palette = (BLUE, GREEN, ORANGE, PURPLE)[index % 4]
        write_model(
            f"ui/tool/capability/{capability}",
            capability_icon(capability, palette),
        )

    for tool, base in tool_colors.items(): write_model(f"ui/quest/tool/{tool}", tool_icon(tool, base))
    for rarity, mark, base in (
        ("common", "shield", GREEN), ("rare", "gem", BLUE),
        ("epic", "flame", ORANGE), ("legendary", "crown", GOLD),
    ):
        write_model(f"ui/quest/summary/{rarity}", ui_icon(mark, base))

    write_model("item/key/pet_crate", crate_key_icon("pets", (151, 65, 171, 255), CYAN))
    for crate, base, edge in (
        ("vote", (30, 170, 185, 255), WHITE),
        ("quest", (48, 118, 168, 255), CYAN),
        ("farm", (142, 92, 34, 255), GREEN),
        ("common", (39, 145, 72, 255), WHITE),
        ("rare", BLUE, CYAN),
        ("epic", (222, 91, 20, 255), GOLD),
        ("legendary", (228, 178, 26, 255), WHITE),
        ("valoria", (158, 17, 38, 255), GOLD),
    ):
        write_model(f"item/key/crate_{crate}", crate_key_icon(crate, base, edge))
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
        ("key", "key", GOLD),
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
