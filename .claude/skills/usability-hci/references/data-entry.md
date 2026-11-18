# Data entry and forms – filtered Smith & Mosier (1986)

Source: Smith & Mosier, *Guidelines for Designing User Interface Software*, MITRE 1986. The section numbers and titles below were checked against the online edition at https://hcibib.org/sam/ (sections 1 "Data Entry" and 4.3 "Error Feedback", retrieved 2026-11-08).

This is a lookup file, not part of the skill core. It keeps only the guidelines that still apply to today's web, desktop, and mobile forms. Each guideline is paraphrased in our own words and mapped to the core principles K1–K15. Where a guideline conflicts with the core rules, the core rules win. Numeric values from the 1986 text are not taken over; use [platform-params.md](platform-params.md).

Excluded as obsolete or out of scope:
- terminal cursor mechanics (most of 1.1);
- direction entry (1.2);
- text-processor pagination and printing (most of 1.3);
- graphics entry (1.6);
- speech input (1.0/32 ff.);
- single-keystroke and shift rules (1.0/25–26);
- label styling (1.4/16, 1.4/18);
- fixed delays for error messages (4.3/11).

## Contents

1. Enter data once, let the system do the work (K7)
2. Defaults (K7)
3. Tolerant input (K3, K4)
4. Labels and prompting (K2, K8, K9)
5. Form structure and order (K8, K12)
6. Focus and movement between fields (K6, K12)
7. User control of entry (K5, K6)
8. Feedback during and after entry (K1, K11)
9. Validation (K3, K4)
10. Error messages (K4, K14)
11. Destructive and reversible actions (K5, C1)

## 1. Enter data once, let the system do the work (K7)

| S&M | Title | Guideline (paraphrase) |
|---|---|---|
| 1.0/1 | Data Entered Only Once | Ask for each piece of data a single time, and make it available to every later step and task that needs it. |
| 1.8/7 | Automatic Generation of Routine Data | Fill in routine values the system already knows or can produce, such as the current date, the user's identity, or running numbers. |
| 1.8/8 | Automatic Computation of Derived Data | Compute values that follow from other entries, such as totals or age, instead of asking for them. |
| 1.8/9 | User Review of Prior Entries | Let users look up and reuse earlier entries instead of typing them again. |
| 1.8/10 | Automatic Entry of Redundant Data | When one entry determines another, for example a postcode and its town, fill in the dependent value automatically. |
| 1.8/11 | Automatic Cross-File Updating | When the same fact is stored in several places, update all of them so the user never enters it twice. |
| 1.5/9 | Aiding Entry of Duplicative Data | In tabular entry, offer a simple way to copy a value that repeats from row to row. |
| 1.0/17 | Optional Abbreviation | Let experienced users shorten long entries where the short form is unambiguous. Never require the short form. |
| 1.0/23 | Clarifying Unrecognized Abbreviations | If a short form is ambiguous or unknown, ask the user which meaning they intended instead of guessing. |

## 2. Defaults (K7)

Core rule K7 still applies: use a default only when one choice is a clear majority, and make it easy to override.

| S&M | Title | Guideline (paraphrase) |
|---|---|---|
| 1.8/1 | Default Values | Where a likely value exists, prefill it to save typing. |
| 1.8/2 | Defaults for Sequential Entries | In a sequence of prefilled fields, let users accept all defaults at once, or every default up to the next field that needs input. |
| 1.8/3 | User Definition of Default Values | Where the designer cannot predict good defaults, let users (or an administrator) set, change, and clear them. |
| 1.8/4 | Display of Default Values | Show the defaults in their fields from the start; do not make users remember invisible ones. |
| 1.8/5 | Easy Confirmation to Enter Default Values | Make accepting a shown default effortless, for example by simply moving on. |
| 1.8/6 | Temporary Replacement of Default Values | Let users override a default for one entry without changing the default for later entries. |

## 3. Tolerant input (K3, K4)

| S&M | Title | Guideline (paraphrase) |
|---|---|---|
| 1.7/2 | Accepting Correct Entries | Every valid entry must be accepted and processed. Test the validation against the full range of real values, such as names, addresses, and dates. |
| 1.0/27 | Upper and Lower Case Equivalent | Ignore letter case in codes and similar entries. |
| 1.0/28 | Decimal Point Optional | Treat a whole number with or without a trailing decimal point as the same value. |
| 1.0/29 | Leading Zeros Optional | Accept general numbers with or without leading zeros. This does not apply to identifiers whose zeros are significant. |
| 1.0/30 | Single and Multiple Blanks Equivalent | Treat runs of spaces like a single space; never make users count blanks. |
| 1.4/3 | Minimal Use of Delimiters | Do not make users type separators the system can insert or ignore, such as those in phone or card numbers. |
| 1.4/13 | Field Markers Not Entered with Data | Placeholder or mask characters left in a field are never processed as data. |
| 1.4/14 | Automatic Justification of Variable-Length Entries | The system pads, aligns, and normalises entries; the user does not. |
| 1.5/7 | Justification of Numeric Entries | Users type numbers freely; the system aligns them, for example on the decimal point. |
| 1.5/8 | Maintaining Significant Zeros | Keep zeros that carry meaning, such as precision after the decimal point, when the value is displayed again. |
| 1.0/15 | Keeping Data Items Short | Keep codes that users must type short. |
| 1.0/16 | Partitioning Long Data Items | Break long values into short groups for entry and display. This must never prevent pasting the whole value. |
| 1.4/22 | Familiar Units of Measurement | Use units the users know; convert internally if needed. |
| 1.4/23 | Alternative Units of Measurement | Where several units are acceptable, let users say which one they used. |

## 4. Labels and prompting (K2, K8, K9)

| S&M | Title | Guideline (paraphrase) |
|---|---|---|
| 1.4/5 | Data Field Labels | Every field has a label that says what to enter. On today's UIs, a placeholder that disappears does not count as a label. |
| 1.4/6 | Consistent Labeling | Use the same label for the same data on every screen (K2). |
| 1.4/8 | Labels Close to Data Fields | Place each label close enough that its field is unmistakable. |
| 1.4/19 | Informative Labels | Use descriptive terms from the users' vocabulary, not internal codes (K8). |
| 1.4/20 | Data Format Cueing in Labels | Where the format is not obvious, show it in or next to the label, for example a date pattern. |
| 1.4/21 | Labeling Units of Measurement | Put a fixed unit in the label, so users do not have to type it. |
| 1.4/11 | Prompting Field Length | Let the size of a field hint at the expected length; state hard maximums. |
| 1.4/12 | Marking Required and Optional Data Fields | Distinguish required and optional fields clearly and in the same way throughout the product. Do not use colour alone (K14). |
| 1.0/24 | Prompting Data Entry | Tell users the expected format and the acceptable values before they type. |
| 1.0/6 | Defined Display Areas for Data Entry | Make it visually clear where input is possible (K9). |
| 1.0/10 | ENTER Key Labeling | Label the submitting control with what it does, for example "Send message", not with the mechanism behind it. |

## 5. Form structure and order (K8, K12)

| S&M | Title | Guideline (paraphrase) |
|---|---|---|
| 1.4/1 | Combined Entry of Related Data | Collect related items in one form that the user submits with a single explicit action. |
| 1.4/27 | Data Items in Logical Order | Order the fields the way users think about the data, not the way the data model stores it (K8). |
| 1.4/25 | Form Compatible with Source Documents | When users type from a document, such as an invoice or an ID card, follow that document's order and grouping. |
| 1.4/24 | Form Compatible for Data Entry and Display | Use the same labels and order for entering data and for reviewing it later. |
| 1.0/31, 1.8/12 | Aids for Entering Hierarchic Data | For nested data, help users see and set where an item belongs in the hierarchy. |
| 1.0/5 | Single Method for Entering Data | Let users complete an entry task with one input method, without switching back and forth between them. |
| 1.4/26 | Minimal Cursor Positioning | Arrange fields so that moving between them needs as few actions as possible (K12). |

## 6. Focus and movement between fields (K6, K12)

| S&M | Title | Guideline (paraphrase) |
|---|---|---|
| 1.1/21, 1.4/28 | Consistent / Automatic Cursor Placement | When a form opens, place the focus at a consistent, useful spot, usually the first field. On touch devices, consider whether opening the keyboard is wanted. |
| 1.1/22 | Easy Cursor Movement to Data Fields | Moving to the next field takes one simple action, and the order is logical. |
| 1.4/15 | Explicit Tabbing to Data Fields | Moving to the next field is the user's decision; do not jump automatically when a field is full (K6). |
| 1.1/24 | Data Entry Independent of Cursor Placement | Submitting sends all fields, no matter which field has focus. |
| 1.1/4 | Explicit Activation | Pointing at something does not activate it; activation needs a separate, deliberate action. Hover never triggers. |

## 7. User control of entry (K5, K6)

| S&M | Title | Guideline (paraphrase) |
|---|---|---|
| 1.0/8 | User-Paced Data Entry | Users set the pace of entry; system processing or timers do not. An expiring session must not discard input (K5). |
| 1.0/9 | Explicit ENTER Action | Processing starts only on an explicit submit action, never as a side effect of something else. For example, there is no auto-submit after the last field (K6). |
| 1.0/11 | Explicit CANCEL Action | Discarding an entry needs an explicit cancel action, never a side effect such as navigating away (K5). |
| 1.4/2 | Flexible Interrupt | Before submitting, users can review, go back, change items, or cancel. |
| 1.3/2 | Editing Capabilities During Text Entry | Users can make simple corrections while typing, without switching into an edit mode (K6). |
| 1.0/7 | Consistent Method for Data Change | Changing earlier entries, including prefilled defaults, works the same way everywhere (K2). |

## 8. Feedback during and after entry (K1, K11)

| S&M | Title | Guideline (paraphrase) |
|---|---|---|
| 1.0/3 | Feedback During Data Entry | Every input action shows a visible result as it happens. Masked fields such as passwords are the exception. |
| 1.0/4, 1.1/5 | Fast Response / Fast Acknowledgement of Entry | Acknowledge input quickly enough that users are never slowed down. Use the K1 time limits. |
| 1.0/12 | Feedback for Completion of Data Entry | After submitting, tell users clearly whether the entry succeeded or failed (K11). |
| 1.0/13 | Feedback for Repetitive Data Entries | In repeated entry, such as adding many items, signal success by presenting a fresh, empty form, and keep values that carry over sensibly. |
| 1.0/14 | Feedback when Changing Data | When users change or delete a value they cannot currently see, let them see the old value before they confirm. |

## 9. Validation (K3, K4)

| S&M | Title | Guideline (paraphrase) |
|---|---|---|
| 1.7/1 | Automatic Data Validation | The system checks entries for format and content. On the web: in the client for speed, and on the server as the authority. |
| 1.7/3, 4.3/10 | Non-Disruptive Error Messages | Report a probable error once the user has finished the entry, not while they are still typing. In practice this means on leaving the field or on submit. |
| 1.7/4 | Deferral of Required Data Entry | Let users postpone a required item they cannot answer yet, for example by saving a draft, without losing the rest. |
| 1.7/5 | Reminder of Deferred Entry | When a submission still has postponed items, point them out and let users complete them or postpone them again. |
| 1.7/6 | Timely Validation of Sequential Transactions | In repeated or multi-step entry, validate each step and allow corrections before the next one begins. |
| 1.7/7 | Optional Item-by-Item Validation | For novices, item-by-item checking can help. Offer it in a way that does not slow down experienced users. |

## 10. Error messages (K4, K14)

| S&M | Title | Guideline (paraphrase) |
|---|---|---|
| 4.3/1 | Informative Error Messages | Say what is wrong and what the user can do about it. |
| 4.3/2 | Specific Error Messages | Be as specific as possible about which item failed and why. |
| 4.3/3 | Task-Oriented Error Messages | Phrase the message in terms of the user's task, not system internals or codes (K8). |
| 4.3/4 | Advisory Error Messages | If only a few values are allowed, list them in the message. |
| 4.3/5 | Brief Error Messages | Keep messages short, but never at the cost of the information needed to fix the error. |
| 4.3/6 | Neutral Wording for Error Messages | Do not blame the user, do not let the computer speak as a person, and do not joke. |
| 4.3/7 | Multilevel Error Messages | After the short message, let users ask for a fuller explanation. |
| 4.3/8 | Multiple Error Messages | When an entry has several errors, tell the user about all of them, for example in a summary plus a message at each field. |
| 4.3/9 | Indicating Repeated Errors | If the same error happens again, change the display noticeably, so the user can see the new attempt was processed. |
| 4.3/13 | Cursor Placement Following Error | Besides the message, mark the error location and put the focus there. |
| 4.3/14 | Displaying Erroneous Entries | Keep the faulty entry visible next to the message until it is corrected. Never clear it. |
| 4.3/15 | User Editing of Entry Errors | Users re-enter only the faulty part, never the whole entry or form. |
| 4.3/16 | Removing Error Messages | Remove the message as soon as the error is corrected. |
| 4.3/17 | Cautionary Messages | For an entry that looks doubtful but may be valid, warn and let the user confirm or change it instead of blocking it. |
| 4.3/19 | Alarm Coding | Make warnings and errors distinctive. Under K14, use text and an icon or shape, not colour alone. |

## 11. Destructive and reversible actions (K5, C1)

Smith & Mosier favour confirmation for destructive actions. The core rules prefer undo (C1): confirm only actions that are irreversible and rare, and name the concrete object.

| S&M | Title | Guideline (paraphrase) |
|---|---|---|
| 1.3/33 | Reversible Actions | Every editing action can be undone right away (K5). |
| 4.3/18 | User Confirmation of Destructive Entries | A destructive entry needs an explicit confirming action before it runs. Apply this only within C1: undo first, and confirm only what is irreversible and rare. |
| 1.3/32 | Confirming Actions in DELETE Mode | Show exactly what will be deleted before the deletion happens. The "delete mode" itself is obsolete (K6: avoid modes). |
| 1.3/34 | User Confirmation of Editing Changes | When editing is finished, let users decide whether the changes replace the original or are kept separately. Today, continuous saving with version history satisfies this. |
