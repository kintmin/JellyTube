## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

## 5. File Encoding

**Always use UTF-8 (without BOM) when writing or moving files.**

- Every file write (`write`, `edit`, move, copy) must use UTF-8 encoding.
- Never use EUC-KR, CP949, UTF-16, or any other encoding — even on Windows.
- Korean comments, strings, and identifiers must be preserved as valid UTF-8.
- If you see "The file was loaded in a wrong encoding" or garbled Korean text, the root cause is a non-UTF-8 write. Fix by rewriting the file explicitly as UTF-8.
- Do not let the OS default encoding (e.g., Windows CP949) influence file output.

### Mandatory post-write verification (this is what makes the rule effective)

"Save as UTF-8" alone does NOT prevent corruption. There are two distinct failure modes:
1. **Wrong save encoding** (CP949/UTF-16) → fixable by re-saving as UTF-8.
2. **Upstream character loss**: a multi-byte sequence is broken during generation/transport and arrives as the Unicode replacement character **U+FFFD** (the black-diamond "?" glyph). Saving as UTF-8 just faithfully stores the already-broken U+FFFD — it cannot recover the lost glyph.

Because mode 2 cannot be prevented by the save step, every file write that contains non-ASCII text (e.g. Korean) MUST be verified and repaired:

- **After every `write`/`edit` of a file containing Korean, grep the file for U+FFFD** using the regex `\x{FFFD}` (ripgrep) or `\uFFFD`. A file is NOT "done" while any U+FFFD remains.
- If U+FFFD is found, the text was corrupted in transit. Rewrite the affected lines from the intended source and re-grep until the count is zero. Do not just re-save.
- **Never copy text that already contains U+FFFD into another file** (commit, new file, edit). Fix the source first, or the corruption propagates.
- When unsure what the original glyph was, reconstruct from context/meaning rather than leaving a U+FFFD placeholder.

## 6. Comments

**Write the constraint, not the story.** Default to one line; a KDoc over three lines needs a reason.

Keep a comment only when someone changing this code would otherwise undo something:
- Platform/API traps (`CMTimeGetSeconds` misreads on Kotlin/Native; an interop view takes touches before the Compose content drawn over it).
- Non-obvious lifetime or ownership (the document-picker delegate is held weakly).
- Why an absence exists (an empty override, a stub binding that Koin would otherwise fail on).
- Magic numbers whose provenance isn't derivable from the code.

Delete:
- Anything the name or signature already says.
- Decision records, alternatives considered, what the code used to be, how it was measured — that belongs in the PR.
- Restatements of what a test already pins.
- Cross-references to tests. The test references the type; that is the link.

**A rule that must hold is a test, not a comment.** If the rule lives in platform code, pull the pure part out so it can be tested (`StepSlotSchedule`, `CMTimeMillis`, `RepeatRange`). iOS-only logic has `iosTest` source sets.

**"Both platforms must do X" is never a comment.** Put X in one shared place so it cannot drift. If it genuinely cannot be shared and cannot be tested, drop the claim rather than writing it down.

## 7. Module Map

**Before touching any file, read the AGENTS.md of every module involved.**

| Path prefix | Read this AGENTS.md |
|---|---|
| `android/app/` | `android/app/AGENTS.md` |
| `shared/presentation/` | `shared/presentation/AGENTS.md` |
| `shared/domain/` | `shared/domain/AGENTS.md` |
| `shared/data/` | `shared/data/AGENTS.md` |
| `shared/log/` | `shared/log/AGENTS.md` |
| `shared/file-share/` | `shared/file-share/AGENTS.md` |
| `ios/app/`, `ios/shared/` | `ios/app/AGENTS.md` |
| `desktop/` | `desktop/AGENTS.md` |

If a task spans multiple modules (e.g., adding a deep link, adding navigation, adding a new screen), read ALL relevant AGENTS.md files before writing a single line of code.