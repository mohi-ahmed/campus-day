# Campus Day repository instructions

## General

- Preserve existing module boundaries and conventions.
- Do not modify unrelated features while implementing a scoped change.
- Add focused tests for domain rules and persistence indexes.

## BCS question data

- Treat imported source data as immutable evidence. Store it separately from normalized runtime questions.
- Keep one normalized question occurrence per MongoDB document. Repeated questions across exams are valid observations and must not be deleted solely because their content fingerprints match.
- Preserve source, rights status, import batch, schema version, and verification metadata.
- Never mark content as training-eligible or prediction-eligible unless its rights and verification rules pass.
- Never use confidential, leaked, or unverifiable examination material.
- Use chronological splits for prediction evaluation; never use random train/test splits for next-exam forecasting.
- Do not expose correct answers, explanations, source internals, or model-only fields from question-delivery APIs.
- Keep AI training and bulk analytics off the live request path. Export or page through versioned, eligible data.

