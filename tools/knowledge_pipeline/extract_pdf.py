#!/usr/bin/env python3
"""Extract an auditable, page-bounded knowledge-section import file from a PDF.

This is an offline data-preparation tool. It never marks output as approved,
verification-eligible, or training-eligible. Scanned/poor-text pages are flagged for
OCR or human review instead of silently inventing text.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import unicodedata
from pathlib import Path

from pypdf import PdfReader


SCHEMA_VERSION = 1
EXTRACTION_METHOD = "PYPDF_PAGE_TEXT"
EXTRACTION_VERSION = "1"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--source-id", required=True)
    parser.add_argument("--language", required=True,
                        choices=("BANGLA", "ENGLISH", "BILINGUAL"))
    parser.add_argument("--subject-code", required=True)
    parser.add_argument(
        "--pages",
        help="Selected 1-based pages, for example 12-30,44,51-60. Omit only for a small document.",
    )
    parser.add_argument("--chunk-chars", type=int, default=3500)
    parser.add_argument("--overlap-chars", type=int, default=250)
    parser.add_argument("--max-sections", type=int, default=500)
    parser.add_argument("--max-total-chars", type=int, default=1_500_000)
    parser.add_argument("--max-section-utf8-bytes", type=int, default=16_000)
    return parser.parse_args()


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest().upper()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest().upper()


def selected_pages(specification: str | None, page_count: int) -> set[int]:
    if not specification:
        return set(range(1, page_count + 1))
    result: set[int] = set()
    for token in specification.split(","):
        token = token.strip()
        if not token:
            continue
        if "-" in token:
            first_text, last_text = token.split("-", maxsplit=1)
            first, last = int(first_text), int(last_text)
        else:
            first = last = int(token)
        if first < 1 or last < first or last > page_count:
            raise ValueError(f"Invalid page selection: {token}")
        result.update(range(first, last + 1))
    if not result:
        raise ValueError("pages must select at least one page")
    return result


def normalize_text(value: str) -> str:
    value = unicodedata.normalize("NFKC", value or "")
    value = value.replace("\u00ad", "")
    value = re.sub(r"(?<=\w)-\s*\n\s*(?=\w)", "", value)
    value = re.sub(r"[\t\r ]+", " ", value)
    value = re.sub(r"\n{3,}", "\n\n", value)
    return value.strip()


def extraction_quality(text: str, language: str) -> tuple[float, bool]:
    if len(text) < 80:
        return 0.0, True
    visible = [char for char in text if not char.isspace()]
    if not visible:
        return 0.0, True
    useful = sum(char.isalnum() or char in "।.,:;!?()[]{}'\"/-+%=৬৭৮৯০১২৩৪৫" for char in visible)
    base = useful / len(visible)
    replacement_penalty = min(0.5, text.count("�") / max(1, len(visible)) * 10)
    bangla_ratio = sum("\u0980" <= char <= "\u09ff" for char in visible) / len(visible)
    language_penalty = 0.0
    if language == "BANGLA" and bangla_ratio < 0.20:
        language_penalty = 0.35
    quality = max(0.0, min(1.0, base - replacement_penalty - language_penalty))
    return round(quality, 4), quality < 0.55


def chunks(text: str, maximum: int, overlap: int) -> list[str]:
    if maximum < 500:
        raise ValueError("chunk-chars must be at least 500")
    if overlap < 0 or overlap >= maximum:
        raise ValueError("overlap-chars must be between 0 and chunk-chars")
    paragraphs = [part.strip() for part in re.split(r"\n\s*\n", text) if part.strip()]
    if not paragraphs:
        return []
    result: list[str] = []
    current = ""
    for paragraph in paragraphs:
        if len(paragraph) > maximum:
            if current:
                result.append(current)
                current = ""
            start = 0
            while start < len(paragraph):
                result.append(paragraph[start:start + maximum].strip())
                start += maximum - overlap
            continue
        candidate = f"{current}\n\n{paragraph}".strip() if current else paragraph
        if len(candidate) <= maximum:
            current = candidate
        else:
            result.append(current)
            prefix = current[-overlap:].strip() if overlap else ""
            current = f"{prefix}\n\n{paragraph}".strip() if prefix else paragraph
    if current:
        result.append(current)
    return [part for part in result if part]


def main() -> None:
    args = parse_args()
    input_path = args.input.resolve(strict=True)
    if args.chunk_chars > 4500:
        raise ValueError("chunk-chars cannot exceed the Mongo import limit of 4500")
    if args.max_sections < 1 or args.max_total_chars < 1:
        raise ValueError("extraction limits must be positive")
    reader = PdfReader(input_path)
    page_selection = selected_pages(args.pages, len(reader.pages))
    sections: list[dict[str, object]] = []
    warnings: list[str] = []
    ordinal = 1
    total_characters = 0

    for page_number, page in enumerate(reader.pages, start=1):
        if page_number not in page_selection:
            continue
        text = normalize_text(page.extract_text() or "")
        quality, requires_ocr = extraction_quality(text, args.language)
        if not text:
            warnings.append(f"Page {page_number}: no extractable text; OCR required")
            continue
        if requires_ocr:
            warnings.append(f"Page {page_number}: extraction quality {quality}; review/OCR required")
        for part in chunks(text, args.chunk_chars, args.overlap_chars):
            normalized = re.sub(r"\s+", " ", part).strip()
            normalized_bytes = normalized.encode("utf-8")
            if len(normalized_bytes) > args.max_section_utf8_bytes:
                raise ValueError(
                    f"Page {page_number} chunk exceeds {args.max_section_utf8_bytes} UTF-8 bytes; "
                    "use a smaller --chunk-chars value"
                )
            if len(sections) + 1 > args.max_sections:
                raise ValueError(
                    "Selected pages exceed the section limit; choose only BCS-relevant pages "
                    "with --pages"
                )
            total_characters += len(normalized)
            if total_characters > args.max_total_chars:
                raise ValueError(
                    "Selected pages exceed the character limit; split or narrow --pages"
                )
            sections.append({
                "ordinal": ordinal,
                "pageStart": page_number,
                "pageEnd": page_number,
                "language": args.language,
                "normalizedText": normalized,
                "sha256": sha256_bytes(normalized_bytes),
                "subjectCode": args.subject_code,
                "topicCode": None,
                "subTopicCode": None,
                "conceptCodes": [],
                "extractionQuality": quality,
                "requiresOcr": requires_ocr,
            })
            ordinal += 1

    payload = {
        "schemaVersion": SCHEMA_VERSION,
        "sourceId": args.source_id,
        "documentSha256": sha256_file(input_path),
        "extractionMethod": EXTRACTION_METHOD,
        "extractionVersion": EXTRACTION_VERSION,
        "pageCount": len(reader.pages),
        "selectedPageCount": len(page_selection),
        "totalCharacters": total_characters,
        "warnings": warnings,
        "sections": sections,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({
        "sourceId": args.source_id,
        "pages": len(reader.pages),
        "selectedPages": len(page_selection),
        "sections": len(sections),
        "characters": total_characters,
        "warnings": len(warnings),
        "output": str(args.output.resolve()),
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
