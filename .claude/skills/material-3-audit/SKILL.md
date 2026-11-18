---
name: material-3-audit
description: Compose 소스를 Material Design 3 기준으로 감사해 토큰·타이포·셰이프·레이아웃·접근성 위반을 카테고리별 점수와 함께 뽑는다.
when_to_use: >-
  Compose 화면이나 뷰를 새로 만들거나 고친 뒤 MD3 규약을 지켰는지 대조할 때,
  색·타이포·모서리·여백이 테마에서 오는지 점검할 때, 태블릿·폴더블·데스크톱 대응을
  확인할 때. 사용성·흐름 문제는 이 스킬이 아니라 usability-hci 다 — 둘은 겹치지 않는다.
---

# Material Design 3 준수 감사 (Compose)

> 출처: [hamen/material-3-skill](https://github.com/hamen/material-3-skill) (MIT). 원문의 audit 절과
> 토큰 표만 추렸고 `@material/web`·Flutter·CSS 부분은 뺐다. 라이선스는 같은 폴더의 `LICENSE` 다.

이 저장소는 Compose Multiplatform 이다. **Android·iOS·Desktop 이 같은 `shared/presentation`
코드를 쓴다** — 그래서 "Android 에서만 맞는 값" 은 답이 아니다. 데스크톱 창 크기와 iOS 터치
관례를 같이 생각한다.

## 감사 절차

1. 대상 파일을 읽는다. 범위가 넓으면 화면 단위로 끊는다.
2. 아래 10개 카테고리를 각각 0–10 으로 매긴다.
3. 보고서를 낸다. **모든 지적에 `file:line` 과 고칠 코드를 붙인다.**

| 카테고리 | Compose 에서 보는 것 |
|---|---|
| Color tokens | `MaterialTheme.colorScheme` 역할을 쓰는가. 근거 없는 `Color(0x…)` 하드코딩. `onX` 를 `X` 위에 쓰는 짝 맞춤. 다크 테마 |
| Typography | `MaterialTheme.typography` 를 쓰는가. 날 `fontSize`/`lineHeight` 지정. 역할(Display·Headline·Title·Body·Label) 선택 |
| Shape | `MaterialTheme.shapes` 또는 컴포넌트 `shape`. 매직넘버 모서리 |
| Elevation | 그림자가 아니라 톤으로 올리는가(`Surface` tonal) |
| Components | Material3 컴포저블(`Button`, `Scaffold`, `NavigationBar` …)과 올바른 variant |
| Layout | window size class / 적응형 API. 넓은 창에서 읽기 가능한 최대 폭. 폴더블 힌지 |
| Navigation | 크기별 bar / rail / drawer 선택. `NavHost` 패턴 |
| Motion | `MotionScheme` 또는 전환 easing·duration |
| Accessibility | 터치 타깃 48dp. 대비 — 본문 4.5:1, 큰 글씨·UI 요소 3:1. `Modifier.semantics` |
| Theming | `MaterialTheme` 와 light/dark 구성 |

## 이 저장소에서 실제로 걸렸던 함정

새로 감사할 때 **먼저 이 둘을 찾는다.** 둘 다 컴파일이 통과하고 프리뷰에서도 잘 안 보인다.

**`RoundedCornerShape(4)` 는 4dp 가 아니라 4% 다.** `Int` 오버로드가 퍼센트다. `4.dp` 를 쓰려면
`RoundedCornerShape(4.dp)` 라고 써야 한다. 애초에 `MaterialTheme.shapes.small` 을 쓰면 안 생긴다.

**`lineHeight` 가 `fontSize` 보다 작으면 글자가 잘린다.** 한글은 영문보다 세로로 꽉 차서 더
위험하다. `fontSize = 12.sp, lineHeight = 10.sp` 같은 조합은 전부 지적한다.
`MaterialTheme.typography.bodySmall` 을 쓰면 안 생긴다.

## 빠른 탐색

```powershell
# 하드코딩된 색
rg -n 'Color\(0x' --glob '**/*.kt'

# 날 폰트 지정 (typography 역할을 안 쓴 자리)
rg -n 'fontSize\s*=|lineHeight\s*=' --glob '**/*.kt'

# 퍼센트로 해석되는 모서리
rg -n 'RoundedCornerShape\([0-9]+\)' --glob '**/*.kt'

# 고정 열 수 (적응형 미적용)
rg -n 'GridCells\.Fixed' --glob '**/*.kt'
```

`rg` 결과는 출발점일 뿐이다. **근거가 있는 하드코딩도 있으므로** 맥락을 보고 판단한다.

## 토큰 기준값

### 색 역할

| 역할 | 쓰임 |
|---|---|
| `primary` / `onPrimary` | 강조 채움과 그 위의 글자·아이콘 |
| `primaryContainer` / `onPrimaryContainer` | 눈에 띄어야 하는 컨테이너(FAB 등) |
| `secondary` / `tertiary` 계열 | 덜 강한 강조, 대비되는 강조 |
| `error` / `errorContainer` | 에러 상태. **파괴적 동작은 여기서 색을 가져온다** |
| `surface` / `onSurface` | 기본 배경과 그 위 글자 |
| `onSurfaceVariant` | 낮은 강조의 글자·아이콘 |
| `surfaceContainerLowest` … `surfaceContainerHighest` | 컨테이너 단계. `Card` 기본은 `surfaceContainerLow` 다 |
| `outline` | 중요한 경계(텍스트필드 테두리) |
| `outlineVariant` | 장식용 경계(구분선). **구분선에 `outline` 을 쓰지 않는다** |

**짝을 어긋나게 쓰지 않는다.** `onPrimary` 를 `surface` 위에 올리면 다크 모드와 고대비 모드에서
대비가 깨진다.

### 타입 스케일

| 스케일 | 쓰임 |
|---|---|
| Display L/M/S | 히어로 텍스트, 큰 숫자 |
| Headline L/M/S | 섹션 제목 |
| Title L/M/S | 작은 제목, 카드 제목 |
| Body L/M/S | 본문, 설명 |
| Label L/M/S | 버튼, 칩, 캡션 |

### 모서리

| 토큰 | 값 | 쓰는 곳 |
|---|---|---|
| extraSmall | 4dp | 칩, 스낵바 |
| small | 8dp | 텍스트필드, 메뉴 |
| medium | 12dp | 카드 |
| large | 16dp | FAB, 드로어 |
| extraLarge | 28dp | 다이얼로그, 바텀시트 |
| full | 완전 둥글게 | 버튼, 칩, 배지 |

### 고도

| 레벨 | dp | 쓰는 곳 |
|---|---|---|
| 0 | 0 | 평평한 면. 대부분의 컴포넌트 기본 |
| 1 | 1 | 올라간 카드, 모달 시트 |
| 2 | 3 | 메뉴, 내비게이션 바, 스크롤된 앱바 |
| 3 | 6 | FAB, 다이얼로그, 검색, 피커 |

MD3 는 고도를 **그림자가 아니라 톤**으로 표현한다. 복잡한 배경에서 분리가 더 필요할 때만
그림자를 더한다.

### 전환

| easing | duration | 전환 종류 |
|---|---|---|
| Emphasized | 500ms | 화면 안에서 시작하고 끝남 |
| Emphasized decelerate | 400ms | 화면에 들어옴 |
| Emphasized accelerate | 200ms | 화면에서 나감 |
| Standard | 300ms | 유틸리티성 전환 |

## 안티패턴

- **색 하드코딩** — 다이내믹 테마·다크 모드·고대비 모드가 전부 깨진다
- **`outline` 을 구분선에** — 구분선은 `outlineVariant` 다
- **`border-radius` 대신 매직넘버 모서리** — shape 토큰을 쓴다
- **기본값으로 그림자 고도** — 톤이 먼저다
- **폰 전용 레이아웃** — 600dp 이상에서 다중 페인, 힌지 위에 조작 요소를 두지 않는다
- **넓은 창에서 내용을 끝까지 늘리기** — 1200dp 이상에서는 최대 폭(840–1040dp)으로 묶는다.
  끝없이 긴 글줄은 읽히지 않는다

## 보고서 형식

```
# MD3 준수 감사

대상: <경로>
총점: <X/100>

| 카테고리 | 점수 | 상태 |
|---|---|---|
| Color tokens | X/10 | pass/warn/fail |
| … | | |

## 치명 (0–3점 항목)
- `file:line` · 관찰 · 고칠 코드

## 경고 (4–6점 항목)

## 통과 (7–10점 항목)

## 고칠 순서
1. 가장 영향이 큰 것부터
```

점수 기준: **9–10** 완전 준수 · **7–8** 사소한 위반 몇 개 · **4–6** 부분 준수, 빈틈이 큼 ·
**1–3** 중대한 위반 · **0** 해당 없음. 상태는 pass(7+) / warn(4–6) / fail(0–3).
