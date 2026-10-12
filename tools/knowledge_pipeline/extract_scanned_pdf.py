#!/usr/bin/env python3
"""OCR selected, taxonomy-mapped pages from a scanned PDF into review staging JSON.

The script renders one page at a time and deletes page images immediately. It never
marks text as approved, verification-eligible, or training-eligible, and stores only
one canonical text value per section.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
import subprocess
import tempfile
import unicodedata
from pathlib import Path

from pypdf import PdfReader


SCHEMA_VERSION = 1
EXTRACTION_METHOD = "TESSERACT_PAGE_OCR"
EXTRACTION_VERSION = "3"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--mapping", required=True, type=Path)
    parser.add_argument("--pdftoppm", required=True, type=Path)
    parser.add_argument("--tesseract", required=True, type=Path)
    parser.add_argument("--tessdata-dir", required=True, type=Path)
    parser.add_argument("--work-dir", type=Path, default=Path("tmp/pdfs"))
    parser.add_argument("--chunk-chars", type=int, default=3500)
    parser.add_argument("--max-sections", type=int, default=500)
    parser.add_argument("--max-total-chars", type=int, default=1_500_000)
    parser.add_argument("--max-section-utf8-bytes", type=int, default=16_000)
    return parser.parse_args()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest().upper()


def sha256_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest().upper()


def normalize_text(value: str) -> str:
    value = unicodedata.normalize("NFKC", value or "").replace("\u00ad", "")
    retained: list[str] = []
    for raw_line in value.splitlines():
        line = re.sub(r"\s+", " ", raw_line).strip()
        if not line or re.fullmatch(r"[০-৯0-9]+", line):
            continue
        if line == "বাংলা ভাষার ব্যাকরণ ও নির্মিতি":
            continue
        if "বাংলা ভাষার ব্যাকরণ ও নির্মিতি, ৯ম-১০ম শ্রেণি" in line:
            continue
        retained.append(line)
    return " ".join(retained).strip()


def chunks(value: str, maximum: int) -> list[str]:
    if maximum < 500 or maximum > 4500:
        raise ValueError("chunk-chars must be between 500 and 4500")
    if len(value) <= maximum:
        return [value] if value else []
    result: list[str] = []
    start = 0
    while start < len(value):
        end = min(start + maximum, len(value))
        if end < len(value):
            boundary = value.rfind("।", start, end)
            if boundary > start + maximum // 2:
                end = boundary + 1
        result.append(value[start:end].strip())
        start = end
    return [part for part in result if part]


def mean_word_confidence(tsv_path: Path) -> float:
    confidences: list[float] = []
    with tsv_path.open("r", encoding="utf-8", errors="replace", newline="") as stream:
        for row in csv.DictReader(stream, delimiter="\t"):
            text = (row.get("text") or "").strip()
            try:
                confidence = float(row.get("conf") or -1)
            except ValueError:
                continue
            if text and confidence >= 0:
                confidences.append(confidence)
    return round(sum(confidences) / max(1, len(confidences)) / 100, 4)


def read_mapping(path: Path, page_count: int) -> tuple[dict, dict[int, dict]]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    page_map: dict[int, dict] = {}
    for item in payload.get("ranges", []):
        first = int(item["pageStart"])
        last = int(item["pageEnd"])
        if first < 1 or last < first or last > page_count:
            raise ValueError(f"Invalid mapped PDF page range: {first}-{last}")
        for page in range(first, last + 1):
            if page in page_map:
                raise ValueError(f"Mapped PDF page appears more than once: {page}")
            page_map[page] = item
    if not page_map:
        raise ValueError("mapping must select at least one PDF page")
    return payload, page_map


def ocr_with_psm(args: argparse.Namespace, image_path: Path, page: int,
                 language: str, psm: int, temp_dir: Path) -> tuple[str, float]:
    output_base = temp_dir / f"ocr-{page:03d}-psm-{psm}"
    subprocess.run([
        str(args.tesseract), str(image_path), str(output_base), "-l", language,
        "--tessdata-dir", str(args.tessdata_dir), "--psm", str(psm)
    ], check=True, capture_output=True)
    confidence_base = temp_dir / f"confidence-{page:03d}-psm-{psm}"
    subprocess.run([
        str(args.tesseract), str(image_path), str(confidence_base), "-l", language,
        "--tessdata-dir", str(args.tessdata_dir), "--psm", str(psm), "tsv"
    ], check=True, capture_output=True)
    text = output_base.with_suffix(".txt").read_text(encoding="utf-8", errors="replace")
    quality = mean_word_confidence(confidence_base.with_suffix(".tsv"))
    return normalize_text(text), quality


def choose_ocr_candidate(candidates: list[tuple[str, float, int]]) -> tuple[str, float, int]:
    """Prefer confidence unless it hides a materially more complete page reading.

    Tesseract can report high mean confidence for a tiny fragment of a table. A
    candidate with substantially more text is safer when its confidence remains
    close to the shorter candidate. PSM 3 preserves table rows far better than
    PSM 6 on the NCTB scans while PSM 6 generally remains best for prose.
    """
    selected = max(candidates, key=lambda candidate: candidate[1])
    for candidate in candidates:
        selected_length = max(1, len(selected[0]))
        materially_more_complete = len(candidate[0]) >= selected_length * 1.35
        comparable_confidence = candidate[1] >= selected[1] - 0.12
        severe_coverage_gap = len(candidate[0]) >= selected_length * 3
        usable_table_confidence = candidate[1] >= selected[1] - 0.25
        if (materially_more_complete and comparable_confidence) or (
                severe_coverage_gap and usable_table_confidence):
            selected = candidate
    return selected


def run_ocr(args: argparse.Namespace, pdf: Path, page: int, language: str,
            dpi: int, temp_dir: Path) -> tuple[str, float, int]:
    prefix = temp_dir / f"page-{page:03d}"
    subprocess.run([
        str(args.pdftoppm), "-png", "-r", str(dpi), "-f", str(page),
        "-l", str(page), "-singlefile", str(pdf), str(prefix)
    ], check=True, capture_output=True)
    image_path = prefix.with_suffix(".png")
    candidates = [
        (*ocr_with_psm(args, image_path, page, language, 6, temp_dir), 6),
        (*ocr_with_psm(args, image_path, page, language, 3, temp_dir), 3),
    ]
    if max(candidate[1] for candidate in candidates) < 0.70:
        candidates.append(
            (*ocr_with_psm(args, image_path, page, language, 4, temp_dir), 4))
    return choose_ocr_candidate(candidates)


def main() -> None:
    args = parse_args()
    pdf = args.input.resolve(strict=True)
    mapping_path = args.mapping.resolve(strict=True)
    for executable in (args.pdftoppm, args.tesseract):
        if not executable.resolve().is_file():
            raise FileNotFoundError(executable)
    tessdata_dir = args.tessdata_dir.resolve(strict=True)
    args.tessdata_dir = tessdata_dir
    reader = PdfReader(pdf)
    mapping, page_map = read_mapping(mapping_path, len(reader.pages))
    language = mapping["ocrLanguage"]
    dpi = int(mapping.get("dpi", 300))
    sections: list[dict[str, object]] = []
    warnings: list[str] = []
    total_characters = 0
    ordinal = 1

    args.work_dir.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="bcs-ocr-", dir=args.work_dir) as temporary:
        temp_dir = Path(temporary)
        for page in sorted(page_map):
            labels = page_map[page]
            text, quality, psm = run_ocr(args, pdf, page, language, dpi, temp_dir)
            if not text:
                warnings.append(f"PDF page {page}: OCR produced no text")
                continue
            if quality < 0.70:
                warnings.append(f"PDF page {page}: low OCR confidence {quality}")
            for part in chunks(text, args.chunk_chars):
                if len(part.encode("utf-8")) > args.max_section_utf8_bytes:
                    raise ValueError(f"PDF page {page} chunk exceeds UTF-8 byte limit")
                if len(sections) + 1 > args.max_sections:
                    raise ValueError("OCR output exceeds section limit")
                total_characters += len(part)
                if total_characters > args.max_total_chars:
                    raise ValueError("OCR output exceeds total character limit")
                sections.append({
                    "ordinal": ordinal,
                    "chapter": labels["chapter"],
                    "heading": labels["heading"],
                    "pageStart": page,
                    "pageEnd": page,
                    "language": mapping["language"],
                    "normalizedText": part,
                    "sha256": sha256_text(part),
                    "subjectCode": labels["subjectCode"],
                    "topicCode": labels["topicCode"],
                    "subTopicCode": labels.get("subTopicCode"),
                    "conceptCodes": labels.get("conceptCodes", []),
                    "extractionQuality": quality,
                    "ocrPageSegmentationMode": psm,
                    "requiresOcr": False,
                })
                ordinal += 1

    payload = {
        "schemaVersion": SCHEMA_VERSION,
        "sourceId": mapping["sourceId"],
        "documentSha256": sha256_file(pdf),
        "extractionMethod": EXTRACTION_METHOD,
        "extractionVersion": EXTRACTION_VERSION,
        "pageCount": len(reader.pages),
        "selectedPageCount": len(page_map),
        "totalCharacters": total_characters,
        "warnings": warnings,
        "sections": sections,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({
        "sourceId": payload["sourceId"],
        "selectedPages": len(page_map),
        "sections": len(sections),
        "characters": total_characters,
        "warnings": len(warnings),
        "minimumQuality": min((item["extractionQuality"] for item in sections), default=0),
        "output": str(args.output.resolve()),
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
