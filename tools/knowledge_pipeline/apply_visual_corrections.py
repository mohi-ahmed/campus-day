#!/usr/bin/env python3
"""Apply page-level visual corrections to a review-only knowledge dataset."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dataset", required=True, type=Path)
    parser.add_argument("--corrections", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--max-section-characters", type=int, default=4500)
    parser.add_argument("--max-section-utf8-bytes", type=int, default=16000)
    return parser.parse_args()


def normalized_lines(lines: list[str]) -> str:
    cleaned = [re.sub(r"\s+", " ", line).strip() for line in lines]
    return " ".join(line for line in cleaned if line)


def sha256_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest().upper()


def main() -> None:
    args = parse_args()
    dataset = json.loads(args.dataset.read_text(encoding="utf-8"))
    corrections = json.loads(args.corrections.read_text(encoding="utf-8"))
    if dataset["sourceId"] != corrections["sourceId"]:
        raise ValueError("Correction sourceId does not match dataset sourceId")
    if corrections.get("humanVerified") is not False:
        raise ValueError("Visual corrections must not claim human verification")

    sections_by_page: dict[int, dict] = {}
    for section in dataset["sections"]:
        page = int(section["pageStart"])
        if page != int(section["pageEnd"]) or page in sections_by_page:
            raise ValueError(f"Expected exactly one single-page section for page {page}")
        sections_by_page[page] = section

    seen: set[int] = set()
    for correction in corrections["pages"]:
        page = int(correction["page"])
        if page in seen or page not in sections_by_page:
            raise ValueError(f"Invalid or duplicate correction page: {page}")
        seen.add(page)
        text = normalized_lines(correction["lines"])
        if not text:
            raise ValueError(f"Correction page {page} has no text")
        if len(text) > args.max_section_characters:
            raise ValueError(f"Correction page {page} exceeds character limit")
        if len(text.encode("utf-8")) > args.max_section_utf8_bytes:
            raise ValueError(f"Correction page {page} exceeds UTF-8 byte limit")
        section = sections_by_page[page]
        section["normalizedText"] = text
        section["sha256"] = sha256_text(text)
        section["contentReviewMethod"] = corrections["reviewMethod"]
        section["humanVerified"] = False

    dataset["totalCharacters"] = sum(
        len(section["normalizedText"]) for section in dataset["sections"])
    dataset["contentReviewMethod"] = corrections["reviewMethod"]
    dataset["humanVerified"] = False
    dataset["warnings"] = [
        "PDF pages 34-40 were visually corrected by AI; independent human review is required before approval or ML use."
    ]

    output = args.output.resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = output.with_suffix(output.suffix + ".tmp")
    temporary.write_text(
        json.dumps(dataset, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    temporary.replace(output)
    print(json.dumps({
        "sourceId": dataset["sourceId"],
        "correctedPages": sorted(seen),
        "sections": len(dataset["sections"]),
        "characters": dataset["totalCharacters"],
        "humanVerified": dataset["humanVerified"],
        "output": str(output),
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
