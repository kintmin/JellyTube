# Review procedure (Mode B)

Method basis: heuristic evaluation after Nielsen / Molich (1990), using K1–K15 as the heuristics and Nielsen's severity scale (1994).

## Contents

1. Scope
2. Task walkthrough
3. Severity
4. Finding format
5. Independent passes and merging
6. Output: chat or `UX-REVIEW.md`
7. Rehaul
8. Limits
9. `UX-REVIEW.md` template

## 1. Scope

Record the scope before you review:
- **Screens and flows** under review, as files, routes, or components.
- **2–5 main tasks** in the users' words, for example "change the delivery address of an open order". Do not phrase them in system terms such as "PATCH /orders".
- **Users and frequency.** Who uses this, and how often: first-time, occasional, or daily.
- **Platform and input modality.** Pointer, touch, keyboard, or a mix.
- **Consequences of errors.** Annoying, costly, or irreversible.

If you cannot derive the scope from the code, the docs, or the request, ask at most 3 targeted questions. Then state your assumptions.

## 2. Task walkthrough

For each main task:
1. Identify the entry point and every step to completion, including error branches, cancel, and back.
2. At each step, check K1–K15. The questions to ask:
   - Does the user know what to do next? (K9, K8)
   - Does the system show what happened? (K1, K11)
   - Can the step go wrong, is the failure prevented, and if not, is it recoverable? (K3, K4, K5)
   - Must the user remember or retype something? (K7)
   - Does anything happen without the user triggering it? (K6)
3. Check the cross-cutting rules once for the whole scope: K2 (terms and components), K14 (colour and contrast), K15 (target sizes for the platform).
4. In code reviews, look in the actual handlers, not just the markup:
   - submit and error paths (is the state reset on error?);
   - loading flags (is the button disabled while busy?);
   - delete handlers (undo, or a confirmation, and does C1 justify it?);
   - `:hover`-only CSS and `onMouseEnter`-only logic;
   - colour-only status classes;
   - the dimensions of interactive elements.

## 3. Severity

Nielsen's scale (1994). Rate each finding from three factors:
- **Frequency** – how often the problem occurs;
- **Impact** – how hard it is to overcome;
- **Persistence** – one-off, or it keeps bothering users who already know about it.

Market impact can raise the rating.

| Score | Meaning | Action |
|---|---|---|
| 0 | Not a usability problem | – |
| 1 | Cosmetic | Fix only if time allows |
| 2 | Minor | Low priority |
| 3 | Major | High priority |
| 4 | Catastrophe | Must be fixed before release |

Typical anchors: data loss, an irreversible action without protection, or a main task that cannot be completed → 4. Retyping a whole form after an error → 3–4. A function reachable only on hover → 3 on touch, 2–3 on pointer. Treat these anchors as guidance, not as rules.

## 4. Finding format

One finding per line or block:

```
<Principle ID> · <Location: file:line / component / screen> · <Observation> · Severity <0–4> · Fix: <proposal> · [needs user validation]
```

- **Principle ID.** Use one or more of K1–K15. If a conflict rule decided the case, add it, for example `K5/C1`.
- **Location.** It must be specific enough to act on.
- **Observation.** Describe what happens, not a judgement. "After a server error the form is reset and all fields are empty" is an observation; "bad error handling" is not.
- **Fix.** Make it concrete and minimal, and name the principle it satisfies.
- **`[needs user validation]`.** Add it for findings that rest on K6, K8, K10, or K13, and for any other finding that depends on assumptions about users or tasks.

Example:

```
K4 · src/components/ContactForm.tsx:42 (onSubmit catch) · On a server error, setValues(initialValues) clears every field · Severity 4 · Fix: keep the values, show the message above the form with the cause and next step, and move focus to it
```

## 5. Independent passes and merging

According to Nielsen, a single evaluator finds only part of the problems. The mean of about three independent evaluators is sufficient for many purposes.

**With subagents** (for example the Agent tool in Claude Code, or subagents in Codex), the passes are optional but recommended for larger reviews:
1. Start 2–3 review passes in parallel. Each pass gets only the scope, the main tasks, and the path to this skill. A pass must not see another pass's findings.
2. Each pass returns findings in the finding format.
3. Merge the results:
   - Unite findings that have the same root cause at the same location, and keep the most precise wording.
   - Set severity to the mean of the passes, rounded to the nearest integer.
   - Note how many passes found each finding (for example `2/3`).
   - Do not drop a finding that only one pass found, but check it yourself.
4. Report the number of passes in the summary.

**Without subagents** (fallback):
1. Do one complete pass.
2. Do a second pass per principle cluster, looking only for that cluster across the whole scope:
   - feedback and status: K1, K11;
   - errors: K3, K4, K5;
   - control and memory: K6, K7;
   - model and discoverability: K8, K9, K10;
   - perception and motor control: K14, K15;
   - consistency and efficiency: K2, K12, K13.
3. State in the report: "Single evaluator with cluster re-pass – reduced reliability compared with independent passes."

## 6. Output: chat or `UX-REVIEW.md`

- **Chat only** for focused reviews: one screen or flow, a quick check, or findings to be fixed right now.
- **Also write `UX-REVIEW.md` in the repository root** for a larger rehaul, meaning several screens or flows, or fixes planned across sessions or people. If unclear, ask the user.
- Write the findings in the user's language. Principle IDs stay unchanged.

## 7. Rehaul

1. Sort findings by severity, 4 → 1. Within the same severity, fix MUST before SHOULD.
2. Fix in small, reviewable steps, and give each step its principle ID.
3. After each group of fixes, walk the affected main tasks again (§2). Update the findings: mark them fixed, and add new findings the fix introduced.
4. Apply Mode A's MUST self-check to every changed file.
5. Do not include visual or brand redesign in an interaction rehaul unless the user asks for it.

## 8. Limits

- **K6, K8, K10, K13.** Without a user test these can be judged only partly. Mark them `[needs user validation]` and suggest what to test, for example "five users perform task X; observe whether they find Y".
- **No real data.** A heuristic review does not replace user testing or analytics, so it gives no frequency data. Severity for frequency is an estimate.
- **Accessibility.** Contrast, colour-only information, and target sizes are covered. Everything else in accessibility (screen readers, keyboard order beyond K9, and so on) is out of scope; recommend a dedicated accessibility audit.

## 9. `UX-REVIEW.md` template

```markdown
# UX review: <product / area>

- Date: <YYYY-MM-DD>
- Reviewer: <agent/runtime>, <number> independent passes | single evaluator with cluster re-pass (reduced reliability)
- Basis: usability-hci skill, principles K1–K15, conflict rules C1–C5, severity 0–4 (Nielsen 1994)

## Scope

- Screens / flows: <list with paths>
- Main tasks:
  1. <task in user words>
  2. ...
- Users / frequency: <...>
- Platform / input: <pointer | touch | keyboard | mixed>
- Assumptions: <what was assumed instead of asked>

## Summary

| Severity | Count |
|---|---|
| 4 | n |
| 3 | n |
| 2 | n |
| 1 | n |

Key problems: <2–4 sentences>

## Findings

| # | ID | Location | Observation | Sev. | Fix | Passes | Status |
|---|---|---|---|---|---|---|---|
| 1 | K4 | `src/...:42` | ... | 4 | ... | 3/3 | open |

Findings that need user validation: <#numbers>, with suggested tests.

## Rehaul plan

1. Severity 4: <#numbers>
2. Severity 3: <#numbers>
3. ...

## Limits of this review

- K6, K8, K10, and K13 were judged without a user test.
- <other limits>

## Change log

- <date>: <fixes applied, findings re-checked>
```
