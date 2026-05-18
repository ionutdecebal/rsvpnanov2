#!/usr/bin/env python3
from __future__ import annotations

import argparse
import importlib.util
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
VECTORS = ROOT / "RSVPNanoCompanion" / "testdata" / "conversion"
LOCAL_GRADLE = ROOT / ".local" / "run_local_gradle.ps1"

TEXT_CASES = [
    ("basic-text-input.txt", "basic-text-expected.rsvp", "Basic Text Vector"),
    ("basic-md-input.md", "basic-md-expected.rsvp", "Basic Markdown Vector"),
    (
        "basic-markdown-input.markdown",
        "basic-markdown-expected.rsvp",
        "Basic Markdown Extension Vector",
    ),
]

HTML_CASES = [
    ("basic-html-input.html", "basic-html-expected.rsvp", "Basic HTML Vector"),
    ("basic-htm-input.htm", "basic-htm-expected.rsvp", "Basic HTM Vector"),
    ("basic-xhtml-input.xhtml", "basic-xhtml-expected.rsvp", "Basic XHTML Vector"),
]


def run(command: list[str], cwd: Path = ROOT) -> None:
    print("+", " ".join(command))
    subprocess.run(command, cwd=cwd, check=True)


def assert_same(expected: Path, actual: Path, label: str) -> None:
    expected_text = expected.read_text(encoding="utf-8").replace("\r\n", "\n")
    actual_text = actual.read_text(encoding="utf-8").replace("\r\n", "\n")
    if expected_text != actual_text:
        raise AssertionError(f"{label} output differed from {expected}")


def run_kotlin() -> None:
    if not LOCAL_GRADLE.is_file():
        print(f"Skipping Kotlin parity: {LOCAL_GRADLE} not found")
        return
    run(
        [
            "powershell",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            str(LOCAL_GRADLE),
            ":shared:testDebugUnitTest",
            "--no-daemon",
            "--no-configuration-cache",
        ]
    )


def load_python_converter():
    converter_path = ROOT / "RSVPNanoCompanion" / "tools" / "sd_card_converter" / "convert_books.py"
    spec = importlib.util.spec_from_file_location("rsvp_sd_card_converter", converter_path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Could not load {converter_path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def run_python_vector(tmp: Path, input_name: str, expected_name: str, title: str, label: str) -> None:
    module = load_python_converter()
    input_path = VECTORS / input_name
    output_path = tmp / f"python-{input_path.stem}.rsvp"
    _title, author, events = module.events_for_file(input_path)
    writer = module.RsvpWriter(
        title=title,
        author=author,
        source=input_path.name,
        max_words=0,
    )
    for kind, value in events:
        if kind == "chapter":
            writer.add_chapter(value)
        else:
            writer.begin_paragraph()
            writer.add_text(value)
    writer.write_to(output_path, fallback_chapter=title)
    assert_same(VECTORS / expected_name, output_path, label)


def run_python_text(tmp: Path) -> None:
    for input_name, expected_name, title in TEXT_CASES:
        run_python_vector(tmp, input_name, expected_name, title, f"Python {Path(input_name).suffix}")


def run_python_html(tmp: Path) -> None:
    for input_name, expected_name, title in HTML_CASES:
        run_python_vector(tmp, input_name, expected_name, title, f"Python {Path(input_name).suffix}")


def run_web_vector(tmp: Path, command: str, input_name: str, expected_name: str, title: str, label: str) -> None:
    node = shutil.which("node")
    if node is None:
        print("Skipping web parity: node not found")
        return
    output = tmp / f"web-{Path(input_name).stem}.rsvp"
    run(
        [
            node,
            str(ROOT / "RSVPNanoCompanion" / "web" / "converter_cli.cjs"),
            command,
            str(VECTORS / input_name),
            str(output),
            title,
        ]
    )
    assert_same(VECTORS / expected_name, output, label)


def run_web_text(tmp: Path) -> None:
    for input_name, expected_name, title in TEXT_CASES:
        run_web_vector(tmp, "text", input_name, expected_name, title, f"Web {Path(input_name).suffix}")


def run_web_html(tmp: Path) -> None:
    for input_name, expected_name, title in HTML_CASES:
        run_web_vector(tmp, "html", input_name, expected_name, title, f"Web {Path(input_name).suffix}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Run cross-runtime RSVP converter parity checks.")
    parser.add_argument("--skip-kotlin", action="store_true", help="Skip Gradle/Kotlin tests.")
    args = parser.parse_args()

    with tempfile.TemporaryDirectory(prefix="rsvpnano-conversion-") as temp:
        tmp = Path(temp)
        if not args.skip_kotlin:
            run_kotlin()
        run_python_text(tmp)
        run_python_html(tmp)
        run_web_text(tmp)
        run_web_html(tmp)

    print("Conversion parity checks passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
