---
name: usability-hci
description: 사용성·HCI 원칙(Nielsen, Norman, Shneiderman, Fitts, ISO 9241-110)으로 화면과 플로우를 걸어가며 severity 매긴 지적과 수정안을 뽑는다. UI 를 만들 때도, 기존 화면을 점검할 때도 쓴다.
when_to_use: >-
  화면을 새로 만들거나 기존 화면을 고칠 때, UI 의 형태·배치·흐름을 바꿀 때,
  애니메이션이나 제스처를 새로 들일 때, 그리고 기존 화면에 사용성 문제가 없는지
  점검해 달라고 할 때. 다루는 것은 상호작용·피드백·에러·폼·네비게이션·문구·대비·터치 타깃이다.
  쓰지 않는 경우 — 사용자 접점이 없는 코드, 상호작용이 그대로인 순수 시각 변경(색·아이콘 교체),
  단순 리팩터.
---

# Usability & HCI principles

> 출처: [strzyzewski/hci-ux-skill](https://github.com/strzyzewski/hci-ux-skill) (MIT). 원문 그대로이며
> 프론트매터만 이 저장소에 맞게 고쳤다. 라이선스는 같은 폴더의 `LICENSE` 다.

This skill holds UI work to canonical, research-based usability principles. It has two modes:
**A – Build** (designing and implementing UIs, forms, flows) and **B – Review/Rehaul** (evaluating existing UIs and reworking them).

Talk to the user (questions, findings, reports, `UX-REVIEW.md`) in the user's language; keep principle IDs unchanged.

## Scope

In scope: interaction behaviour, feedback, errors, forms and data entry, flows, navigation, wording of UI text, perception (contrast, colour use) and motor control (target size and placement).

Out of scope, do not argue from this skill about:
- aesthetics and visual style, branding, ISO 9241-110 "user engagement", persuasive design;
- full accessibility/WCAG coverage. Accessibility appears here only where it follows from HCI fundamentals: contrast, information not conveyed by colour alone, target size. WCAG values are floors, not targets. Do not use APCA. For a real accessibility audit, say that it is a separate domain.

## Rule levels

- **MUST** – apply always. Deviate only if you tell the user explicitly which rule you are breaking and why (e.g. a platform constraint or an explicit user decision).
- **SHOULD** – apply by default; deviate when the context justifies it, and say so briefly.
- Testability tags: **C** checkable in code or running UI · **T** partly checkable, needs domain knowledge (ask) · **U** only a user test can settle it. Never present a T or U judgement as certain.

## Rules (K1–K15)

Derivation, sources, and evidence per rule: [references/principles.md](references/principles.md). Numeric values per platform: [Platform parameters](#platform-parameters) and [references/platform-params.md](references/platform-params.md).

**K1 Visible system status, feedback for every action** – MUST · C
- Every click or tap gets a visible acknowledgement within 50 ms (pressed/active state); block duplicate triggering (disable or debounce the control while the action runs).
- Under 0.1 s: no extra feedback needed. Over 1 s: show a working indicator. 2–10 s: spinner or counter. Over 10 s: percent-done progress plus a working Cancel.
- Report normal states inline (status text, toast, changed element), not in a modal dialog.

**K2 Consistency and conventions** – MUST · C
- Same term for the same thing everywhere; same component for the same behaviour, a different component for different behaviour.
- Platform conventions before custom inventions (native controls, standard shortcuts, standard placement).
- What has changed must look changed (dirty state, updated values, moved items).

**K3 Error prevention** – MUST · C
- Exclude invalid choices (disable, filter, constrain) instead of reporting errors afterwards.
- Constrain input by type, format, and range (input types, pickers, masks, min/max).
- Separate destructive actions spatially and visually from frequent ones (never next to Save/OK in the same style).

**K4 Error recognition, diagnosis, recovery** – MUST · C
- Every error message states the problem, the cause, and the fix, in the user's language (no codes or stack traces as the only text).
- An error never alters or discards the user's data; the user corrects only the faulty part and never refills a form.
- Mark the error at its location and move focus there (or to a summary that links to it).

**K5 Reversibility, protect the user's work** – MUST · C
- Offer undo for single steps and for groups of actions.
- No data loss through errors, network failure, validation, navigation, or session expiry (S&M 1.0/8); save continuously or keep drafts.
- Every flow has a visible exit (Cancel, Back, Close) that leaves no half-done state.

**K6 User in control, no hidden modes** – MUST for modes and context changes, SHOULD otherwise · T
- MUST: nothing happens without a user trigger – no automatic redirects, no focus stealing, no auto-submit on the last field (S&M 1.0/9).
- MUST: avoid modes; if unavoidable, make the mode clearly visible or use a quasimode (active only while held).
- SHOULD: keep the layout stable – no self-reordering menus, no content jumping under the pointer (AP perceived stability).

**K7 Recognition rather than recall** – MUST · C
- Never make users enter the same data twice.
- Keep information needed for the next step visible instead of making users carry it across screens.
- Use defaults only where one choice is a clear majority, and make them easy to override.

**K8 Task and mental-model fit** – MUST · T
- Structure screens and flows by the user's task, not by the database or API schema.
- Use the user's vocabulary, not technical jargon or internal names.
- Map controls naturally to their effect (position, direction, grouping).
- Before structuring a UI, know the user and the task (see Mode A, step 1).

**K9 Discoverability and signifiers** – MUST · C
- Interactive elements look interactive; non-interactive ones do not.
- No function reachable only by hover, gesture, or keyboard shortcut; there is always a visible path.
- Current location and navigation are always visible; do not hide needed controls to look simple.

**K10 Show only what is relevant** – SHOULD · T
- Disclose rarely needed options progressively; no decoration that pretends to carry information. Mind the conflict with K9 (C3).

**K11 Closure** – SHOULD · C
- Multi-step flows have a clear start, visible progress, and an explicit completion confirmation.

**K12 Efficiency and expert paths** – SHOULD · C
- Shortcuts accelerate the *same* command as the visible path; no parallel workflows.
- Minimise steps for the main tasks (count them, KLM-style); optimise total human time, not machine time.

**K13 Learnability and help** – SHOULD · T
- Help is contextual, task-oriented, and searchable; the goal is a UI that needs no explanation.

**K14 Perception: legibility, contrast, colour** – MUST · C
- Never convey information by colour alone; add text, icon, shape, or position.
- Contrast floors: text ≥ 4.5:1; large text (about 24 px, or about 19 px bold) ≥ 3:1 – near the threshold, treat text as body text; UI components and meaningful graphics ≥ 3:1.
- Guidance (no fixed value): for thin typefaces aim above the floor or use a heavier weight.

**K15 Motor control: target size and distance (Fitts)** – MUST · C
- Guidance (T): frequent targets are large and close to where the pointer or thumb already is; use screen edges and corners for pointer UIs.
- Guidance (T): dangerous targets are smaller and farther away (see K3).
- Checkable: every interactive target meets the minimum size for the platform (table below).

## Platform parameters

Principles are device-independent; values are not. Details and derivations: [references/platform-params.md](references/platform-params.md).

| Parameter | Pointer / desktop web | Touch |
|---|---|---|
| Minimum target (K15) | 24×24 CSS px, or 24 CSS px spacing (WCAG 2.2 SC 2.5.8, floor) | 44×44 pt (Apple HIG), 48×48 dp (Material); 44×44 CSS px on the web (WCAG 2.5.5 AAA) |
| Edge advantage (K15) | Screen edges and corners act as infinitely large targets | Largely not applicable |
| Response-time limits (K1) | 0.1 s / 1 s / 10 s | same |
| Contrast (K14) | WCAG 2 contrast formula is the normative measure; values are floors | same |

## Conflict rules (C1–C5)

The resolutions are proposals derived from the research, not statements of any single source. Say so when you rely on one.

| ID | Conflict | Resolution |
|---|---|---|
| C1 | Confirmation before consequential actions vs. undo instead of asking | Undo first. Confirm only actions that are irreversible **and** rare; name the concrete object and consequence in the dialog, not a generic Yes/No. |
| C2 | Several ways to do a task vs. one way per task (monotony) | One interaction model; shortcuts only accelerate the same command, no parallel workflows. |
| C3 | Minimalism vs. discoverability | Core functions stay visible; rare ones are disclosed progressively; never hide things to look simple. |
| C4 | Separate novice/expert interfaces vs. one learning curve | One interface that carries users from beginner to intermediate; no separate modes. |
| C5 | Customisability vs. stability | User-controlled customisation yes; automatic rearrangement by the system no. |

## Mode A – Build

Use when designing or implementing UIs, forms, dialogs, or flows.

1. **Clarify context (K8).** Determine from code, docs, tickets, or the request: primary users and how often they use this; main tasks in the users' words; platform and input modality (pointer, touch, keyboard); consequences of errors (annoying vs. costly vs. irreversible). If you cannot derive it, ask **at most 3** targeted questions, then proceed and state your assumptions. Never derive the UI structure from the database or API schema.
2. **Apply K1–K15** while designing and coding. For forms and data entry also use [references/data-entry.md](references/data-entry.md).
3. **Resolve conflicts** with C1–C5 and name the conflict rule you applied.
4. **Self-check before you finish.** Go through every MUST rule against your own diff (the checklist below). Fix violations; report any remaining deviation with rule ID and reason. Mention which T rules (K6, K8, K10, K13) would need a user test.

Self-check list (MUST, against the diff):
- K1: pressed state, double-trigger guard (including secondary controls such as toast actions and Undo), indicator for > 1 s, % progress and Cancel for > 10 s, no modal for normal status?
- K2: same terms and components as the rest of the product, platform controls?
- K3: invalid input prevented by constraints; destructive action separated?
- K4: message says problem, cause, fix; input preserved; focus at error?
- K5: undo or recoverable state; no loss on error, reload, offline; visible exit?
- K6: no auto-redirect, no auto-submit, no focus theft; modes visible?
- K7: no re-entry of known data; defaults justified?
- K8: task-structured, user vocabulary? (ask if unsure)
- K9: everything reachable without hover/gesture/shortcut; affordances visible?
- K14: no colour-only meaning; contrast floors met?
- K15: target sizes for the platform; dangerous targets away from frequent ones?

## Mode B – Review / Rehaul

Use for UX reviews, usability audits, heuristic evaluations, and interaction rehauls. Full procedure, severity details, merge rules, and the `UX-REVIEW.md` template: [references/review-procedure.md](references/review-procedure.md).

1. **Scope.** Fix the screens/flows under review, 2–5 main tasks (in user words), and the platform. Ask if unclear (max 3 questions).
2. **Walk each main task** step by step through the code or the running UI, and check every step against K1–K15.
3. **Independent passes (optional).** If your runtime has subagents, run 2–3 independent review passes with no shared context (each gets only the scope, the tasks, and this skill), then merge: unite duplicate findings, average the severity. Fallback without subagents: one pass, then a second pass per principle cluster; state the reduced reliability in the report.
4. **Record findings** in this format:
   `Principle ID · Location (file/component/screen) · Observation · Severity 0–4 · Fix proposal · [needs user validation]`
5. **Output.** For a larger rehaul (several screens or flows, or fixes planned across sessions) also write `UX-REVIEW.md` in the repository root using the template; otherwise report in chat only. If unclear, ask.
6. **Rehaul order.** Fix severity 4 → 1, MUST before SHOULD within the same severity; re-check the affected tasks after the fixes and update the findings.
7. **State the limits.** K6, K8, K10, and K13 can be judged only partly without a user test; mark such findings "needs user validation".

Severity (Nielsen 1994), rated from frequency, impact, and persistence:
**0** not a usability problem · **1** cosmetic, fix if time allows · **2** minor, low priority · **3** major, high priority · **4** catastrophe, must be fixed before release.

## References

- [references/principles.md](references/principles.md) – full principle table with sources, evidence type, strictness criteria, testability, conflicts, and literature.
- [references/platform-params.md](references/platform-params.md) – target sizes, response times, contrast, with derivations and caveats.
- [references/review-procedure.md](references/review-procedure.md) – review method, severity, finding format, multi-pass merge, `UX-REVIEW.md` template.
- [references/data-entry.md](references/data-entry.md) – filtered Smith & Mosier (1986) guidelines for forms and data entry.
