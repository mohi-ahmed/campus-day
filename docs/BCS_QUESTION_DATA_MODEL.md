# BCS question data foundation

This module stores normalized question occurrences for fast delivery and safe AI exports.

## Collections

| Collection | Purpose |
|---|---|
| `bcs_questions` | Compact normalized runtime and training records |
| `bcs_question_import_batches` | Idempotent import manifests and counters |
| `bcs_question_raw_imports` | Immutable source payloads and normalization errors |
| `bcs_taxonomy_nodes` | Version-labelled official subject/topic/subtopic/concept hierarchy |
| `bcs_question_classifications` | Auditable machine classification proposals awaiting review |
| `bcs_question_answer_verifications` | Claimed answers and evidence-based verification state |
| `bcs_knowledge_sources` | Book/document metadata, provenance, checksum, rights, and ingestion state |
| `bcs_knowledge_sections` | Small BCS-topic-labelled text sections retained for review and evidence |

Raw books, PDFs, and question images do not belong in MongoDB. Store files in object storage and keep only URLs and metadata in MongoDB.

## Book knowledge storage boundary

The book pipeline is deliberately two-stage:

```text
PDF in object storage -> temporary page extraction -> topic classification/filtering
                      -> curated section JSON -> MongoDB review collection
```

- Never store a PDF, page image, or embedding vector in `bcs_knowledge_sections`.
- Store one canonical `normalizedText` value per section; raw extracted text remains in disposable staging instead of being duplicated in MongoDB.
- Raw extraction files stay under git-ignored staging storage and are disposable.
- A section must have a valid BCS subject/topic, extraction quality of at least `0.70`, and no unresolved OCR flag before import.
- Each section is limited to 4,500 characters and 16,000 UTF-8 bytes.
- Each source is limited to 500 retained sections and 1,500,000 normalized characters.
- The import JSON is limited to 25 MiB.
- `(sourceId, sha256)` prevents storing the same normalized passage twice for one source.
- Source metadata tracks retained section and character counts for storage audits.
- Rights remain `REVIEW_REQUIRED` and eligibility remains false until an authorized review explicitly changes them.

Register the metadata-only source catalogue with `BCS_KNOWLEDGE_REGISTRY_ENABLED=true`.
Extract only relevant page ranges into staging, for example:

```text
python tools/knowledge_pipeline/extract_pdf.py --input book.pdf --output data/import/staging/knowledge/book.json --source-id SOURCE_ID --language BANGLA --subject-code BANGLA --pages 12-40,75-90
```

The extractor intentionally leaves `topicCode` empty. A classification/review step must remove irrelevant chunks and assign valid taxonomy topics before `BCS_KNOWLEDGE_SECTIONS_ENABLED=true` can import the curated JSON. This prevents a raw whole-book dump from entering MongoDB.

For scanned books, use `tools/knowledge_pipeline/extract_scanned_pdf.py` with an
explicit page-to-taxonomy mapping. It renders one selected page at a time, runs
Tesseract OCR with multiple page-segmentation modes, considers both confidence and
text coverage so a high-confidence fragment cannot hide a missed table, and deletes
rendered images automatically. The first mapping is
`src/main/resources/data/bcs/nctb-2026-bangla-grammar-pilot-mapping.json`: 17 PDF pages covering
উপসর্গ, প্রত্যয়, সমাস, সন্ধি, and শব্দদ্বিত্ব. OCR output remains review-only even
when it passes the `0.70` extraction-quality gate. The pilot's machine/visual audit
is recorded in `data/import/staging/knowledge/nctb-2026-bangla-grammar-pilot-ocr-review.json`;
it explicitly does not represent human text verification.

For the table-heavy pilot pages, reviewed page text is kept as a small declarative
correction file and applied with `tools/knowledge_pipeline/apply_visual_corrections.py`.
This restores table row relationships, recalculates section checksums atomically,
and records AI visual review without claiming human verification.

## Core rules

- One historical question occurrence is one `bcs_questions` document.
- `questionId` uniquely identifies an exam occurrence.
- `sourceKey` makes imports idempotent.
- `fingerprint` detects equal or near-equal content but is intentionally not unique: a repeated question in another exam is valuable prediction data.
- Imported records always begin in `NEEDS_REVIEW`.
- Only `CLEARED` content can become `VERIFIED` or `PUBLISHED`.
- Training eligibility requires verified/published status, cleared rights, non-AI source, and answer/label confidence of at least `0.80`.
- Prediction eligibility is stricter: it requires a historical/official source, exam identity, question number, and answer/label confidence of at least `0.90`.
- Delivery APIs never return correct answers, explanations, provenance internals, or eligibility flags.

## Import workflow

1. Register the approved taxonomy using `docs/examples/bcs-taxonomy.example.json` and `POST /api/internal/career-prep/taxonomy/nodes`.
2. Convert an authorized source to the canonical request in `docs/examples/bcs-question-import.example.json`.
3. POST it to `/api/internal/career-prep/question-imports`.
4. Inspect the returned import counts and any item-level errors.
5. Review a question with `PUT /api/internal/career-prep/questions/{questionId}/review`.
6. Move an approved record from `NEEDS_REVIEW` to `VERIFIED`, then to `PUBLISHED`.

Import batches are idempotent by `batchKey`. Reposting a completed batch returns its existing result instead of inserting duplicates.

Question imports validate every subject, topic, subtopic, and concept against the active global taxonomy. This prevents label drift from entering the AI training dataset.

## Query APIs

Published delivery queries use cursor pagination:

```text
GET /api/career-prep/questions?subjectCode=BANGLA&topicCode=GRAMMAR&limit=20
GET /api/career-prep/questions?examType=GENERAL&examNumber=46&limit=100
GET /api/career-prep/questions/{questionId}
```

The next request supplies the returned `nextCursor` as `afterId`. Large offset/skip pagination is intentionally not used.

The internal AI export is also cursor-based:

```text
GET /api/internal/career-prep/training/questions?limit=500
GET /api/internal/career-prep/training/questions?predictionOnly=true&limit=500
```

`predictionOnly=true` excludes licensed/curated practice questions that may be useful for language or generation training but are not valid historical forecasting observations.

## Index strategy

The runtime indexes support these bounded queries:

- published questions by subject and cursor;
- published questions by subject/topic and cursor;
- published questions by exam and cursor;
- exact exam sequence lookup;
- concept lookup;
- training/prediction exports by cursor;
- source identity, content fingerprint, and import batch diagnostics.

Do not add an index for every field. Add a new compound index only when a measured production query needs it and verify it with MongoDB `explain("executionStats")`.

The BCS module explicitly ensures only its own indexes at startup. This also allows a one-time importer to run with global Mongo auto-index creation disabled when an unrelated legacy collection has an index-definition conflict.

The prediction export cursor uses `question_prediction_cursor` on
`{predictionEligible: 1, _id: 1}`. Before `MongoTemplate` initializes annotated
indexes, startup removes the obsolete `question_prediction_export` index. The current
index is then created by normal Spring Data initialization. This narrowly scoped,
idempotent migration handles deployments that still have either the older
`{predictionEligible: 1, examNumber: 1, _id: 1}` definition or the superseded index
name on the current key pattern.

## AI training boundary

Training must not scan MongoDB on a student request. Page through the eligible internal export to create a versioned JSONL/Parquet snapshot, record the cutoff and checksums, and train from that snapshot. The deployed model should load an approved artifact into memory or read precomputed predictions.

## Selected GitHub staging source

The local staging adapter supports `SangbitDas/bcs-dataset`. It deliberately imports the dataset with `REVIEW_REQUIRED` rights, `NEEDS_REVIEW` status, unknown difficulty, and no training/prediction eligibility. The source has subject labels but no topic/subtopic labels, so records enter an explicit `<SUBJECT>_UNCLASSIFIED` topic instead of receiving invented labels.

Enable the idempotent one-time importer with:

```text
BCS_SANGBITDAS_IMPORT_ENABLED=true
```

The downloaded third-party source is kept under the git-ignored `data/import/staging/` directory. Image paths and all original fields remain in the raw import payload; images are not treated as runtime asset URLs.

## Official taxonomy and enrichment

The packaged `BPSC_50_PRELIMINARY_2025` taxonomy is transcribed from the official
50th BCS preliminary syllabus published by BPSC on 2025-12-09. It preserves the
current 200-mark distribution separately from historical question occurrence data.

Run the one-time enrichment job with:

```text
BCS_ENRICHMENT_ENABLED=true
```

The job is idempotent. It upserts the official taxonomy, creates one conservative
classification proposal per question, and creates an answer-verification record in
`PENDING_EVIDENCE` state. It does not mutate the canonical question label, accept a
proposal, clear content rights, or claim that an imported answer is verified.

Classification rules are subject-scoped and require a minimum score and margin.
Ambiguous questions remain under the subject's explicit `UNCLASSIFIED` topic. The
proposal records retain the matched signals, method, taxonomy version, and confidence
so reviewers and later ML classifiers can be compared without destroying provenance.

An answer may move to `SOURCE_SUPPORTED` only after an approved knowledge-source
section is attached as evidence and supports the selected option. Book ingestion must
therefore preserve source identity, edition, rights, section locator, and text checksum.

