---
name: research-quality-measurement
description: Use this skill to assess the quality of codebase research documents.
  Defines five quality levels (EXCELLENT to UNACCEPTABLE) and a four-dimension
  weighted scoring rubric for verifying file:line accuracy, snippet correctness,
  bug coverage completeness, and discrepancy rate.
---

# Skill: Research Quality Measurement

Use this skill to assess the quality of codebase research. Apply it by checking every claim against the source files and computing a quality level from the scoring criteria below.

---

## Quality Levels

| Level | Score | Criteria |
|---|---|---|
| **EXCELLENT** | 95–100% | All `file:line` references verified; snippets match exactly; no discrepancies; complete coverage of the bug and its root cause |
| **GOOD** | 80–94% | Nearly all references verified; minor discrepancies (e.g., off-by-one line numbers) that do not affect correctness; no missing critical facts |
| **ADEQUATE** | 60–79% | Most references verified; some discrepancies documented and explained; bug coverage is present but may lack depth |
| **POOR** | 30–59% | Many references unverified or incorrect; significant gaps in bug coverage; discrepancies undermine the conclusions |
| **UNACCEPTABLE** | <30% | Major failures in accuracy; file/line references are wrong or missing; research cannot be trusted as a basis for implementation |

---

## Scoring Criteria

Score is computed as the weighted average of the four dimensions below. Each dimension is scored 0–100.

### 1. `file:line` Accuracy (weight: 35%)
- Every `file:line` reference opens to the exact line cited — **+full score**
- Reference exists but is off by 1–3 lines — **−10 points each**
- Reference points to wrong file or nonexistent line — **−25 points each**
- Reference is missing entirely where one was needed — **−15 points each**

### 2. Code Snippet Correctness (weight: 30%)
- Snippet matches the file exactly (whitespace allowed to differ) — **+full score**
- Snippet is paraphrased but semantically correct — **−10 points**
- Snippet contains errors that would mislead implementation — **−30 points**
- Snippet is fabricated / not present in the file — **−50 points**

### 3. Bug Coverage Completeness (weight: 25%)
- Root cause identified with precise location — **+15 points**
- All affected code paths documented — **+10 points**
- Impact / symptom explained — **+10 points**
- Fix direction described (even briefly) — **+5 points**
- Deduct for each missing critical element — **−10 points each**

### 4. Discrepancy Rate (weight: 10%)
- 0 discrepancies — **100 points**
- 1–2 minor discrepancies — **70 points**
- 3–5 discrepancies — **40 points**
- >5 discrepancies — **0 points**

---

## How to Apply This Skill

1. Open every file and line cited in the research document.
2. Compare the quoted snippet to the actual file content.
3. Score each dimension using the rubric above.
4. Compute: `score = 0.35 × accuracy + 0.30 × snippets + 0.25 × coverage + 0.10 × (100 − discrepancy_penalty)`
5. Map the final score to a quality level from the table.
6. Write the level, numeric score, and per-dimension breakdown in your output.
