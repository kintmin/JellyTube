# Principles – full table with sources and derivation

Source of truth: the research matrix "Usability-/HCI-Prinzipien-Matrix" (as of 2026-11-08). All principle texts below are paraphrases in our own words; short principle names are used as identifiers only. No passages from the books are reproduced.

## Contents

1. Source abbreviations
2. Strictness and testability legend
3. Principle table K1–K15
4. Conflicts C1–C5
5. Review-mode basis
6. Deliberately excluded
7. Open items [verify]
8. Literature and links

## 1. Source abbreviations

| Abbr. | Source | Evidence type |
|---|---|---|
| SH | Shneiderman et al., *Designing the User Interface*, 6th ed. 2016, sect. 3.3.4 (Eight Golden Rules; first version 1985) | expert consensus |
| NI | Nielsen, 10 usability heuristics (1994, factor analysis of 249 usability problems; wording smoothed in 2020) | empirically derived |
| NO | Norman, *The Design of Everyday Things*, revised ed. 2013 (reference; first ed. 1988) | cognitive psychology |
| ISO | DIN EN ISO 9241-110:2020, 7 interaction principles, 65 recommendations | standards consensus |
| TOG | Tognazzini, *First Principles of Interaction Design* (revised 2014), 19 principles | expert consensus |
| AF | Cooper / Reimann / Cronin (/ Noessel), *About Face*, 3rd ed. 2007 / 4th ed. 2014, appendix A "Design Principles" | practice |
| RA | Raskin, *The Humane Interface* (2000), plus Raskin's summary of his rules (e-mail, 2002) | cognitive psychology |
| AP | Apple Human Interface Guidelines 1987 / Macintosh HIG 1992 | practice / consensus |
| SM | Smith & Mosier, *Guidelines for Designing User Interface Software*, MITRE 1986 (944 guidelines) | literature compilation |
| JO | Johnson, *GUI Bloopers 2.0* (2007), 9 basic principles plus anti-patterns | practice |
| GP | Gerhardt-Powals, cognitive engineering principles (IJHCI 1996) | experimentally validated |
| RT | Response-time limits: Miller 1968, Card et al. 1991, Nielsen 1993 (*Usability Engineering*, ch. 5) | empirical |
| FI | Fitts' law (1954); Hick–Hyman (1952/53); Card / Moran / Newell 1983 (KLM / GOMS) | empirical |
| SEV | Nielsen, severity ratings (1994) | method |

## 2. Strictness and testability legend

Strictness (research matrix): **MUST** = supported by three or more canonical sources and/or an empirical basis · **SHOULD** = broad consensus, context-dependent · (**MAY** = single source; no rule in this skill is at this level).

Testability for an agent: **C** checkable or enforceable in code · **T** partly checkable, needs domain knowledge (the agent has to ask) · **U** only a user test can establish it.

## 3. Principle table K1–K15

| ID | Principle | Sources | Level | Test | Concrete, checkable rules |
|---|---|---|---|---|---|
| K1 | Visible system status, feedback for every action | SH3, NI1, NO (feedback), ISO self-descriptiveness, TOG latency / autonomy, AP feedback & dialog, AF modeless feedback, RT | MUST | C | < 0.1 s no extra feedback · > 1 s working indicator · 2–10 s spinner or counter · > 10 s percent-done progress with Cancel · click acknowledgement ≤ 50 ms and block double triggering (TOG) · do not report normal states in a dialog (AF), report them inline |
| K2 | Consistency and conventions | SH1, NI4, ISO conformity with user expectations, AP consistency, TOG consistency, SM (user guidance: consistency as the main principle), RA monotony | MUST | C | same terms for the same things · same components for the same behaviour, different component for different behaviour (TOG human-interface objects) · platform conventions before own inventions · what has changed must look changed |
| K3 | Error prevention | SH5, NI5, ISO robustness (avoidance), NO constraints, AF "make errors impossible", AF ejector-seat levers | MUST | C | disable invalid options instead of reporting errors · input constraints (type, format, range) · separate destructive actions spatially and visually from frequent ones (inverse Fitts) |
| K4 | Error recognition, diagnosis, recovery | SH5, NI9, ISO robustness (tolerance / recovery), TOG efficiency, JO textual bloopers | MUST | C | message names problem, cause, and solution in user language · an error state leaves data unchanged · only the faulty part is corrected, never refill the form (SH) · focus / marking at the error location |
| K5 | Reversibility and protection of the user's work | SH6, NI3, AP forgiveness, TOG explorable interfaces / protect users' work, AF "make all actions reversible", RA | MUST | C | undo for single steps and groups of actions · never data loss through errors, network failure, validation · continuous saving (TOG) · every flow has an exit |
| K6 | User in control, no hidden modes | SH7, NI3, ISO controllability (incl. individualisation since 2020), AP user control / perceived stability / modelessness, RA modelessness / quasimodes, TOG autonomy | MUST (modes, context changes) / SHOULD (rest) | T | no action without a user trigger (no auto-redirect, no focus theft) · avoid modes; if necessary make them visible or use a quasimode (held) · stable layout (no self-sorting menus) |
| K7 | Relieve memory: recognition rather than recall | SH8, NI6, AP see-and-point, NO knowledge in the world, TOG anticipation, GP "automate unwanted workload" | MUST | C | never let users enter data twice · keep information for the next step visible instead of making users carry it across screens · sensible, easily overridable defaults (TOG defaults) · no default without a clear majority choice |
| K8 | Task and mental-model fit | NI2, ISO suitability for the task, NO conceptual model / mapping, AF mental vs. implementation model, JO BP1–3, GP "names related to function", AP metaphors | MUST | T | structure follows the task, not the data model · user language instead of technical jargon · natural mapping control → effect · the agent must ask about task and users before structuring |
| K9 | Discoverability and signifiers | NO discoverability / affordances / signifiers, ISO self-descriptiveness, TOG discoverability / visible navigation, AP see-and-point | MUST | C | interactive elements recognisable as such · no function only via hover / gesture / keyboard shortcut · location and navigation always visible · do not hide necessary controls for apparent simplicity |
| K10 | Focus: show only what is relevant | NI8, AP aesthetic integrity / simplicity, GP "only show information currently needed", TOG simplicity | SHOULD | T | disclose rarely needed things progressively · no decoration that claims to carry information · limit: mind the conflict with K9 |
| K11 | Closure | SH4, AP feedback | SHOULD | C | multi-step flows with a clear start, progress, and completion confirmation |
| K12 | Efficiency and expert paths | NI7, SH2 (formerly: shortcuts), TOG efficiency of the user, AF "optimize for intermediates", FI / KLM | SHOULD | C | shortcuts as accelerators of the *same* command · minimise steps / clicks for main tasks (KLM as the measure) · optimise total human time, not machine time |
| K13 | Learnability and help | NI10, ISO learnability, TOG learnability, JO BP6, AF "nobody wants to remain a beginner" | SHOULD | T | help is contextual, task-related, searchable · goal: no explanation needed |
| K14 | Perception: legibility, contrast, colour | TOG color / readability, GP multiple coding, AP 1987 (designing for disabled people), WCAG derivation | MUST | C | information never by colour alone · text contrast ≥ 4.5:1 (large text ≥ 3:1; large = 18 pt ≈ 24 px or 14 pt bold ≈ 19 px; near the threshold, use 4.5:1) · UI components and graphics ≥ 3:1 (WCAG SC 1.4.11) · derivation: 3:1 from ISO 9241-3 / ANSI-HFES-100 for normal vision × factor 1.5 for 20/40 visual acuity (typical for people around 80) |
| K15 | Motor control: target size and target distance | TOG Fitts's law, FI, AF ejector seat | MUST | C | frequent targets large and close, use edges / corners (pointer) · dangerous targets small / far away · minimum sizes: see platform-params.md |

Notes on application:

- **K1** The time bands come from RT (0.1 s feels instantaneous, 1 s keeps the flow of thought, 10 s is the limit of attention). The 50 ms acknowledgement and the double-trigger guard come from TOG.
- **K3 / K15** A destructive action placed right next to a frequent one is an "ejector-seat lever" (AF). Fitts' law, applied in reverse, says to make such targets harder to hit.
- **K4** "Refill the form" is the canonical anti-pattern. SH asks that only the faulty part be corrected.
- **K6** A quasimode is a mode that lasts only while the user physically holds it, for example a held modifier key (RA). The user's muscles keep it in awareness, so it does not trap the user.
- **K7** A default value is only justified when most users would choose it. Otherwise it creates silent errors.
- **K8** The skill cannot verify task fit from code alone. Ask about the users and the tasks, and mark the result for user validation.
- **K12** KLM means counting keystrokes, pointing, homing, and mental-preparation operators for a main task, then comparing designs by their predicted time.

## 4. Conflicts C1–C5

The resolutions are proposals from the research, not statements of a source.

| ID | Conflict | Proposed resolution |
|---|---|---|
| C1 | Confirmation dialog before a consequential action (NI5) vs. "do, don't ask" / undo instead of asking (AF) and habituation to confirmations (RA) | Undo first. Confirm only irreversible **and** rare actions, with the concrete object in the text, not as a standard Yes/No. |
| C2 | Several ways / shortcuts (NI7, SH) vs. monotony: exactly one way per task (RA) | One interaction model; shortcuts only as accelerators of the same command, no parallel workflows. |
| C3 | Minimalism (NI8, AP) vs. discoverability (NO, TOG) | Core functions visible, rare things disclosed progressively; never hide things to look simple. |
| C4 | Serve novices and experts separately (SH, NI) vs. the dichotomy is false (RA) vs. optimise for intermediates (AF) | One interface for the way from beginner to intermediate; no separate modes. |
| C5 | Customisability (ISO controllability) vs. adaptive interfaces prevent habituation (RA), perceived stability (AP) | User-controlled customisation yes; autonomous rearrangement by the system no. |

## 5. Review-mode basis

- Heuristic evaluation after Nielsen / Molich (1990) with the 0–4 severity scale (SEV). Factors: frequency, impact, persistence, plus market impact.
- A single evaluator is unreliable according to Nielsen; the mean of three independent evaluators suffices for many purposes. For agents this means several independent review passes (for example subagents without shared context), then merge the results.
- K6, K8, K10, and K13 can be assessed only partly without a user test (RA; JO BP9: try it out on users).

## 6. Deliberately excluded

- Aesthetics and visual design (TOG refers this to graphic designers himself).
- ISO 9241-110 "user engagement" (close to marketing).
- Persuasive design.
- Full WCAG coverage. Accessibility is its own domain; this skill uses WCAG values only as floors.

## 7. Open items [verify]

These are not blocking. Until they are resolved, the research matrix applies as written.

Check in this order (highest risk first):

1. **[verify]** About Face principles (appendix A, 3rd / 4th ed.). They currently come from notes and third-party quotations and are cited in many rules.
2. **[verify]** Gerhardt-Powals: 9 vs. 10 principles, depending on the source (IJHCI 1996 original). Cited only, carries no rule alone.
3. **[verify]** Older Shneiderman wordings (for example on simple error handling, on an internal locus of control, and on shortcuts for frequent users). The current Eight Golden Rules page is used as the primary source.

Resolved (2026-11-08, owner decision):
- Norman: the 2013 revised edition is the reference (signifiers, K9); 1988 is the first edition.
- Large-text threshold: interpreted pragmatically as about 24 px, or about 19 px bold; near the threshold, use the body-text value (4.5:1).
- The FAA Human Factors Design Standard (HFDS) is not used as a source.

## 8. Literature and links

- Shneiderman – Eight Golden Rules: https://www.cs.umd.edu/~ben/goldenrules.html
- Nielsen – 10 Usability Heuristics: https://www.nngroup.com/articles/ten-usability-heuristics/
- Nielsen – Response Times: The 3 Important Limits: https://www.nngroup.com/articles/response-times-3-important-limits/
- Nielsen – Severity Ratings: https://www.nngroup.com/articles/how-to-rate-the-severity-of-usability-problems/
- Tognazzini – First Principles of Interaction Design (revised): https://asktog.com/atc/principles-of-interaction-design/
- DIN EN ISO 9241-110:2020 (product page): https://www.dinmedia.de/de/norm/din-en-iso-9241-110/320862700
- Smith & Mosier 1986: https://hcibib.org/sam/
- Apple Computer – Human Interface Guidelines: The Apple Desktop Interface. Addison-Wesley, 1987 (no official online edition).
- Raskin – design rules (e-mail 2002): https://mprove.de/script/02/raskin/designrules.html
- Cooper, Reimann, Cronin – About Face 3: The Essentials of Interaction Design. Wiley, 2007, appendix A (design principles).
- Johnson – GUI Bloopers 2.0 (table of contents): https://www.oreilly.com/library/view/gui-bloopers-20/9780123706430/
- W3C – Understanding SC 1.4.3 (contrast minimum): https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum
- Roselli – WCAG3 contrast as of April 2026: https://adrianroselli.com/2026/04/wcag3-contrast-as-of-april-2026.html
