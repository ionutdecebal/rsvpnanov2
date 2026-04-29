#!/usr/bin/env python3

from __future__ import annotations

import argparse
import math
import os
import pathlib
import subprocess
import tempfile


DEFAULT_FONT_NAME = "NotoSans"
DEFAULT_FONT_SEARCH_PATHS = [
    "/usr/share/fonts/truetype/noto",
    "/usr/share/fonts/truetype/dejavu",
    "/usr/share/fonts/truetype/liberation",
]
DEFAULT_POINT_SIZE = 52
CANVAS_WIDTH = 112
CANVAS_HEIGHT = 128
ORIGIN_X = 10
BASELINE_Y = 76
ALPHA_THRESHOLD = 16
FONT_TOP_PADDING = 4
FONT_BOTTOM_PADDING = 2
DEFAULT_FIRST_CHAR = 32
DEFAULT_LAST_CHAR = 126
SPACE_ADVANCE = 6
DEFAULT_OUTPUT_PATH = pathlib.Path("src/display/EmbeddedSerifFont.h")
DEFAULT_SYMBOL_PREFIX = "EmbeddedSerif"

# Keep this slot map in sync with src/text/LatinText.h.
CUSTOM_GLYPH_CODEPOINTS = {
    0x01: 0x010E,  # Dcaron
    0x02: 0x010F,  # dcaron
    0x03: 0x011A,  # Ecaron
    0x04: 0x011B,  # ecaron
    0x05: 0x0147,  # Ncaron
    0x06: 0x0148,  # ncaron
    0x07: 0x0158,  # Rcaron
    0x08: 0x0159,  # rcaron
    0x0E: 0x0164,  # Tcaron
    0x0F: 0x0165,  # tcaron
    0x10: 0x016E,  # Uring
    0x11: 0x016F,  # uring
    0x12: 0x0150,  # Odblac
    0x13: 0x0151,  # odblac
    0x14: 0x0170,  # Udblac
    0x15: 0x0171,  # udblac
    0x80: 0x0152,  # OE
    0x81: 0x0153,  # oe
    0x82: 0x0141,  # Lslash
    0x83: 0x0142,  # lslash
    0x84: 0x010C,  # Ccaron
    0x85: 0x010D,  # ccaron
    0x86: 0x0160,  # Scaron
    0x87: 0x0161,  # scaron
    0x88: 0x017D,  # Zcaron
    0x89: 0x017E,  # zcaron
    0x8A: 0x0102,  # Abreve
    0x8B: 0x0103,  # abreve
    0x8C: 0x0218,  # Scommaaccent
    0x8D: 0x0219,  # scommaaccent
    0x8E: 0x021A,  # Tcommaaccent
    0x8F: 0x021B,  # tcommaaccent
    0x90: 0x011E,  # Gbreve
    0x91: 0x011F,  # gbreve
    0x92: 0x015E,  # Scedilla
    0x93: 0x015F,  # scedilla
    0x94: 0x0130,  # Idotaccent
    0x95: 0x0131,  # dotlessi
    0x96: 0x0104,  # Aogonek
    0x97: 0x0105,  # aogonek
    0x98: 0x0118,  # Eogonek
    0x99: 0x0119,  # eogonek
    0x9A: 0x0106,  # Cacute
    0x9B: 0x0107,  # cacute
    0x9C: 0x0143,  # Nacute
    0x9D: 0x0144,  # nacute
    0x9E: 0x015A,  # Sacute
    0x9F: 0x015B,  # sacute
    0xB2: 0x0179,  # Zacute
    0xB3: 0x017A,  # zacute
    0xB4: 0x017B,  # Zdotaccent
    0xB5: 0x017C,  # zdotaccent
    0xA1: 0x0100,  # Amacron
    0xA2: 0x0101,  # amacron
    0xA3: 0x0112,  # Emacron
    0xA4: 0x0113,  # emacron
    0xA5: 0x0122,  # Gcommaaccent
    0xA6: 0x0123,  # gcommaaccent
    0xA7: 0x012A,  # Imacron
    0xA8: 0x012B,  # imacron
    0xA9: 0x0136,  # Kcommaaccent
    0xAA: 0x0137,  # kcommaaccent
    0xAB: 0x013B,  # Lcommaaccent
    0xAC: 0x013C,  # lcommaaccent
    0xAE: 0x0145,  # Ncommaaccent
    0xAF: 0x0146,  # ncommaaccent
    0xB0: 0x0116,  # Edotaccent
    0xB1: 0x0117,  # edotaccent
    0xB6: 0x012E,  # Iogonek
    0xB7: 0x012F,  # iogonek
    0xB8: 0x0172,  # Uogonek
    0xB9: 0x0173,  # uogonek
    0xBA: 0x016A,  # Umacron
    0xBB: 0x016B,  # umacron
    0xBC: 0x0110,  # Dcroat
    0xBD: 0x0111,  # dcroat
    0xBE: 0x014A,  # Eng
    0xBF: 0x014B,  # eng
    0xD7: 0x0166,  # Tbar
    0xF7: 0x0167,  # tbar
}

CUSTOM_GLYPH_NAMES = {
    0x010E: "Dcaron",
    0x010F: "dcaron",
    0x011A: "Ecaron",
    0x011B: "ecaron",
    0x0147: "Ncaron",
    0x0148: "ncaron",
    0x0158: "Rcaron",
    0x0159: "rcaron",
    0x0164: "Tcaron",
    0x0165: "tcaron",
    0x016E: "Uring",
    0x016F: "uring",
    0x0150: "Odblac",
    0x0151: "odblac",
    0x0170: "Udblac",
    0x0171: "udblac",
    0x0152: "OE",
    0x0153: "oe",
    0x0141: "Lslash",
    0x0142: "lslash",
    0x010C: "Ccaron",
    0x010D: "ccaron",
    0x0160: "Scaron",
    0x0161: "scaron",
    0x017D: "Zcaron",
    0x017E: "zcaron",
    0x0102: "Abreve",
    0x0103: "abreve",
    0x0218: "Scommaaccent",
    0x0219: "scommaaccent",
    0x021A: "Tcommaaccent",
    0x021B: "tcommaaccent",
    0x011E: "Gbreve",
    0x011F: "gbreve",
    0x015E: "Scedilla",
    0x015F: "scedilla",
    0x0130: "Idotaccent",
    0x0131: "dotlessi",
    0x0104: "Aogonek",
    0x0105: "aogonek",
    0x0118: "Eogonek",
    0x0119: "eogonek",
    0x0106: "Cacute",
    0x0107: "cacute",
    0x0143: "Nacute",
    0x0144: "nacute",
    0x015A: "Sacute",
    0x015B: "sacute",
    0x0179: "Zacute",
    0x017A: "zacute",
    0x017B: "Zdotaccent",
    0x017C: "zdotaccent",
    0x0100: "Amacron",
    0x0101: "amacron",
    0x0112: "Emacron",
    0x0113: "emacron",
    0x0122: "Gcommaaccent",
    0x0123: "gcommaaccent",
    0x012A: "Imacron",
    0x012B: "imacron",
    0x0136: "Kcommaaccent",
    0x0137: "kcommaaccent",
    0x013B: "Lcommaaccent",
    0x013C: "lcommaaccent",
    0x0145: "Ncommaaccent",
    0x0146: "ncommaaccent",
    0x0116: "Edotaccent",
    0x0117: "edotaccent",
    0x012E: "Iogonek",
    0x012F: "iogonek",
    0x0172: "Uogonek",
    0x0173: "uogonek",
    0x016A: "Umacron",
    0x016B: "umacron",
    0x0110: "Dcroat",
    0x0111: "dcroat",
    0x014A: "Eng",
    0x014B: "eng",
    0x0166: "Tbar",
    0x0167: "tbar",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate an embedded font header for the display renderer."
    )
    parser.add_argument(
        "--point-size",
        type=int,
        default=DEFAULT_POINT_SIZE,
        help=f"Source font point size. Default: {DEFAULT_POINT_SIZE}",
    )
    parser.add_argument(
        "--font-name",
        default=DEFAULT_FONT_NAME,
        help=f"PostScript font name. Default: {DEFAULT_FONT_NAME}",
    )
    parser.add_argument(
        "--output",
        type=pathlib.Path,
        default=DEFAULT_OUTPUT_PATH,
        help=f"Output header path. Default: {DEFAULT_OUTPUT_PATH}",
    )
    parser.add_argument(
        "--symbol-prefix",
        default=DEFAULT_SYMBOL_PREFIX,
        help=f"Prefix for generated struct/constants. Default: {DEFAULT_SYMBOL_PREFIX}",
    )
    parser.add_argument(
        "--font-search-path",
        action="append",
        default=[],
        help="Additional directory to search for the source font. May be passed multiple times.",
    )
    parser.add_argument(
        "--first-char",
        type=int,
        default=DEFAULT_FIRST_CHAR,
        help=f"First character code to embed. Default: {DEFAULT_FIRST_CHAR}",
    )
    parser.add_argument(
        "--last-char",
        type=int,
        default=DEFAULT_LAST_CHAR,
        help=f"Last character code to embed. Default: {DEFAULT_LAST_CHAR}",
    )
    return parser.parse_args()


def escape_postscript_char(ch: str) -> str:
    if ch in ("\\", "(", ")"):
        return "\\" + ch
    code = ord(ch)
    if code < 32 or code > 126:
        return f"\\{code:03o}"
    return ch


def latin1_font_setup(font_name: str, point_size: int) -> str:
    return (
        f"/CodexLatin1Font /{font_name} findfont dup length dict begin "
        "{1 index /FID ne {def} {pop pop} ifelse} forall "
        "/Encoding ISOLatin1Encoding def "
        "currentdict end definefont pop "
        f"/CodexLatin1Font findfont {point_size} scalefont setfont "
    )


def glyph_name_for_codepoint(codepoint: int) -> str:
    return CUSTOM_GLYPH_NAMES.get(codepoint, f"uni{codepoint:04X}")


def glyph_script_for_codepoint(codepoint: int) -> str:
    if codepoint <= 0xFF:
        escaped = escape_postscript_char(chr(codepoint))
        return f"({escaped}) show"
    return f"/{glyph_name_for_codepoint(codepoint)} glyphshow"


def display_codepoint_for_slot(slot: int) -> int:
    return CUSTOM_GLYPH_CODEPOINTS.get(slot, slot)


def glyph_comment_for_slot(slot: int) -> str:
    mapped_codepoint = CUSTOM_GLYPH_CODEPOINTS.get(slot)
    if mapped_codepoint is None:
        return ascii(chr(slot))
    return f"slot 0x{slot:02X} -> U+{mapped_codepoint:04X}"


def render_glyph(
    tmp_dir: pathlib.Path, slot: int, font_name: str, point_size: int, font_search_paths: list[str]
) -> pathlib.Path:
    output = tmp_dir / f"{slot:03d}.pgm"
    codepoint = display_codepoint_for_slot(slot)
    program = (
        "1 setgray clippath fill "
        "0 setgray "
        f"{latin1_font_setup(font_name, point_size)}"
        f"{ORIGIN_X} {BASELINE_Y} moveto "
        f"{glyph_script_for_codepoint(codepoint)} showpage"
    )
    command = [
        "gs",
        "-q",
        "-dNOPAUSE",
        "-dBATCH",
        "-dTextAlphaBits=4",
        "-dGraphicsAlphaBits=4",
        "-sDEVICE=pgmraw",
        "-r72",
        f"-g{CANVAS_WIDTH}x{CANVAS_HEIGHT}",
        f"-sOutputFile={output}",
    ]

    existing_paths = [font_path for font_path in font_search_paths if pathlib.Path(font_path).is_dir()]
    if existing_paths:
        command.append(f"-sFONTPATH={os.pathsep.join(existing_paths)}")

    command += [
        "-c",
        program,
    ]

    subprocess.run(
        command,
        check=True,
        capture_output=True,
        text=True,
    )
    return output


def advance_width_for_glyph(slot: int, font_name: str, point_size: int, font_search_paths: list[str]) -> int:
    codepoint = display_codepoint_for_slot(slot)
    command = [
        "gs",
        "-q",
        "-dNODISPLAY",
    ]

    existing_paths = [font_path for font_path in font_search_paths if pathlib.Path(font_path).is_dir()]
    if existing_paths:
        command.append(f"-sFONTPATH={os.pathsep.join(existing_paths)}")

    command += [
        "-c",
        (
            f"{latin1_font_setup(font_name, point_size)}"
            "0 0 moveto "
            f"{glyph_script_for_codepoint(codepoint)} "
            "currentpoint pop == quit"
        ),
    ]

    result = subprocess.run(
        command,
        check=True,
        capture_output=True,
        text=True,
    )
    lines = [line.strip() for line in result.stdout.splitlines() if line.strip()]
    if not lines:
        raise RuntimeError(f"Failed to determine advance width for slot 0x{slot:02X}")

    return max(1, int(math.floor(float(lines[-1]) + 0.5)))


def parse_pgm(path: pathlib.Path) -> tuple[int, int, bytes]:
    data = path.read_bytes()
    if not data.startswith(b"P5\n"):
        raise ValueError(f"Unexpected PGM header in {path}")

    parts = data.split(b"\n")
    index = 1
    while parts[index].startswith(b"#"):
        index += 1

    width, height = map(int, parts[index].split())
    max_value = int(parts[index + 1])
    if max_value != 255:
        raise ValueError(f"Unexpected max value {max_value} in {path}")

    raster = b"\n".join(parts[index + 2 :])
    expected_length = width * height
    if len(raster) != expected_length:
        raise ValueError(f"Unexpected raster length in {path}: {len(raster)} != {expected_length}")

    return width, height, raster


def alpha_at(raster: bytes, width: int, x: int, y: int) -> int:
    return 255 - raster[y * width + x]


def main() -> None:
    args = parse_args()
    if not (0 <= args.first_char <= args.last_char <= 255):
        raise ValueError("Character range must satisfy 0 <= first-char <= last-char <= 255")
    font_search_paths = list(DEFAULT_FONT_SEARCH_PATHS)
    font_search_paths.extend(args.font_search_path)
    glyph_images: dict[int, tuple[int, int, bytes]] = {}
    global_top = CANVAS_HEIGHT
    global_bottom = -1

    with tempfile.TemporaryDirectory(prefix="serif_font_") as tmp:
        tmp_dir = pathlib.Path(tmp)

        for code in range(args.first_char, args.last_char + 1):
            pgm_path = render_glyph(
                tmp_dir, code, args.font_name, args.point_size, font_search_paths
            )
            width, height, raster = parse_pgm(pgm_path)
            glyph_images[code] = (width, height, raster)

            for y in range(height):
                found = False
                for x in range(width):
                    if alpha_at(raster, width, x, y) > ALPHA_THRESHOLD:
                        global_top = min(global_top, y)
                        global_bottom = max(global_bottom, y)
                        found = True
                if found:
                    continue

    if global_bottom < global_top:
        raise RuntimeError("Failed to detect any font pixels")

    crop_top = max(0, global_top - FONT_TOP_PADDING)
    crop_bottom = min(CANVAS_HEIGHT - 1, global_bottom + FONT_BOTTOM_PADDING)
    font_height = crop_bottom - crop_top + 1
    bitmap_bytes: list[int] = []
    glyph_entries: list[str] = []

    for code in range(args.first_char, args.last_char + 1):
        width, height, raster = glyph_images[code]

        min_x = width
        max_x = -1
        for y in range(crop_top, crop_bottom + 1):
            for x in range(width):
                if alpha_at(raster, width, x, y) > ALPHA_THRESHOLD:
                    min_x = min(min_x, x)
                    max_x = max(max_x, x)

        bitmap_offset = len(bitmap_bytes)

        if max_x >= min_x:
            glyph_width = max_x - min_x + 1
            for y in range(crop_top, crop_bottom + 1):
                for x in range(min_x, max_x + 1):
                    alpha = alpha_at(raster, width, x, y)
                    if alpha <= ALPHA_THRESHOLD:
                        alpha = 0
                    bitmap_bytes.append(alpha)
            x_offset = min_x - ORIGIN_X
            x_advance = advance_width_for_glyph(code, args.font_name, args.point_size, font_search_paths)
        else:
            x_offset = 0
            glyph_width = 0
            x_advance = advance_width_for_glyph(code, args.font_name, args.point_size, font_search_paths)

        glyph_entries.append(
            "    "
            + "{"
            + f"{bitmap_offset}, {x_offset}, {glyph_width}, {x_advance}"
            + "}, "
            + f"// {glyph_comment_for_slot(code)}"
        )

    lines: list[str] = [
        "#pragma once",
        "",
        "#include <Arduino.h>",
        "",
        "// Generated from a real serif font at build time and embedded as glyph data.",
        f"// Source font: {args.font_name} at {args.point_size} pt",
        "",
        f"struct {args.symbol_prefix}Glyph " + "{",
        "  uint32_t bitmapOffset;",
        "  int8_t xOffset;",
        "  uint8_t width;",
        "  uint8_t xAdvance;",
        "};",
        "",
        f"constexpr uint8_t k{args.symbol_prefix}FirstChar = {args.first_char};",
        f"constexpr uint8_t k{args.symbol_prefix}LastChar = {args.last_char};",
        f"constexpr uint8_t k{args.symbol_prefix}Height = {font_height};",
        "",
        f"static const uint8_t k{args.symbol_prefix}Bitmaps[] PROGMEM = " + "{",
    ]

    for offset in range(0, len(bitmap_bytes), 16):
        chunk = bitmap_bytes[offset : offset + 16]
        lines.append("    " + ", ".join(f"{value:3d}" for value in chunk) + ",")

    lines += [
        "};",
        "",
        f"static const {args.symbol_prefix}Glyph k{args.symbol_prefix}Glyphs[] PROGMEM = " + "{",
        *glyph_entries,
        "};",
        "",
    ]

    args.output.write_text("\n".join(lines) + "\n", encoding="ascii")


if __name__ == "__main__":
    main()
