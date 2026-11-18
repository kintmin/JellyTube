# Platform parameters

A principle stays the same on every device; the numeric value can differ by platform. Treat every value below as a **floor**, not a target.

## Target size (K15)

| Platform | Minimum | Source |
|---|---|---|
| Pointer / desktop web | 24×24 CSS px, or 24 CSS px spacing to neighbouring targets | WCAG 2.2 SC 2.5.8 (AA) |
| Touch – iOS / iPadOS | 44×44 pt | Apple HIG |
| Touch – Android | 48×48 dp | Material Design |
| Touch – web | 44×44 CSS px | WCAG 2.2 SC 2.5.5 (AAA) |

How to apply it:
- Measure the hit area, not the visible icon. Padding counts as long as it is clickable.
- For responsive web UIs that also run on touch devices, use the touch value for touch layouts. A pointer-only floor on a phone violates K15.
- Fitts' law cuts both ways. Make frequent targets large and close; make dangerous targets smaller or farther away, and never put them right next to a frequent target (K3).

## Edge advantage (Fitts)

| Platform | Value | Source |
|---|---|---|
| Pointer | The pointer stops at screen edges and corners, so a target there behaves as if it were infinitely deep. | TOG |
| Touch | Largely not applicable. | TOG |

## Response times (K1)

The limits are the same on all platforms (RT: Miller 1968, Card et al. 1991, Nielsen 1993).

| Duration | Perception | Required feedback |
|---|---|---|
| ≤ 50 ms | – | Visible acknowledgement of the click or tap, for example a pressed state (TOG) |
| < 0.1 s | Feels instantaneous | No extra feedback needed |
| > 1 s | The flow of thought breaks | Working indicator |
| 2–10 s | – | Spinner or counter |
| > 10 s | The limit of attention is exceeded | Percent-done progress plus Cancel |

How to apply it:
- If the duration varies, design for the realistic worst case, for example a slow network or a large file.
- Block repeated triggering while an action is running (TOG).
- Report the normal outcome inline. Never confirm normal completion with a modal dialog (AF).

## Contrast (K14)

| Element | Floor | Source |
|---|---|---|
| Body text | ≥ 4.5:1 | WCAG SC 1.4.3 |
| Large text (18 pt ≈ 24 px, or 14 pt bold ≈ 19 px) | ≥ 3:1 | WCAG SC 1.4.3 |
| UI components (borders of inputs, focus indicators, control states) and meaningful graphics | ≥ 3:1 | WCAG SC 1.4.11 |

The values are the same on all platforms.

- **Measure.** The WCAG 2 contrast formula (relative luminance) remains the normative measure. APCA is not part of the WCAG 3 Working Draft (as of 2026, Roselli 04/2026); do not use it.
- **Derivation of 3:1.** It comes from ISO 9241-3 / ANSI-HFES-100 for normal vision. Multiplying it by 1.5 compensates for 20/40 visual acuity, which is typical for people around 80; that gives 4.5:1.
- **Caution with thin typefaces.** Because of anti-aliasing, the WCAG 2 formula can pass thin fonts that look clearly weaker in reality; the W3C describes this itself. With thin weights, aim clearly above the floor or choose a heavier weight.
- **Large-text threshold, pragmatically.** The pt-to-px conversion is approximate, and the threshold is a floor, not a design target. Near the threshold, apply the body-text value of 4.5:1. Do not argue over fractions of a pixel.
- **Colour alone.** Never convey information by colour alone (status dots, red and green values, link text without an underline inside running text, chart series). Add text, an icon, a shape, a pattern, or a position.
