# JellyTube

**음원을 모아 가사와 함께 듣는 음악 앱.**

Android 와 iOS 에서 같은 화면으로 동작하고, PC 의 음악 파일은 함께 들어 있는 데스크톱 앱으로 폰에 보낼 수 있습니다.

## 주요 기능

- **어디서든 모으기** — 링크, 폰에 있는 파일, 다른 앱의 공유, PC 에서 보내기로 곡을 모읍니다.
- **가사와 함께** — 가사를 자동으로 찾아 재생에 맞춰 보여 주고, 외국어 가사는 번역과 읽는 법을 함께 보여 줍니다.
- **듣는 방식 그대로** — 플레이리스트, 구간 반복, 속도·키 조절, 잠들기 타이머.
- **기기가 바뀌어도** — 매일 자동 백업하고, Android 와 iPhone 사이에서도 데이터를 통째로 옮깁니다.
- **나만 보는 공간** — 숨은 입구로만 들어가는 비밀 플레이리스트.

## 설치

| 플랫폼 | 받는 곳 | 방법 |
|---|---|---|
| Android | [최신 모바일 릴리스](https://github.com/kintmin/JellyTube/releases/latest) | APK 를 받아 설치합니다. 앱이 새 버전을 알려 줍니다. |
| iOS | [최신 모바일 릴리스](https://github.com/kintmin/JellyTube/releases/latest) | [SideStore](https://sidestore.io/) 로 IPA 를 설치합니다. 아래 소스를 추가하면 새 버전이 SideStore 에 뜹니다. 무료 Apple ID 로 서명하므로 7일마다 SideStore 에서 갱신해야 합니다. |
| Windows | [릴리스 목록](https://github.com/kintmin/JellyTube/releases) | `desktop/v…` 릴리스 중 가장 위의 MSI 나 EXE 로 설치합니다. |
| macOS | [릴리스 목록](https://github.com/kintmin/JellyTube/releases) | `desktop/v…` 릴리스 중 가장 위의 DMG 로 설치합니다. Apple Silicon 전용입니다. 서명하지 않아 처음 열 때 막히면 시스템 설정 > 개인정보 보호 및 보안에서 "그래도 열기" 를 누릅니다. |

SideStore 소스: `https://github.com/kintmin/JellyTube/releases/latest/download/source.json`

앱 스토어에는 배포하지 않습니다.

## 업데이트 소식 받기

새 버전이 나오면 메일로 알려 드립니다. **kintmin.ci@gmail.com** 으로 받고 싶은 쪽을 적어 보내 주세요.

- **모바일** — Android·iOS 앱
- **PC** — Windows·macOS 데스크톱 앱
- 둘 다 받으셔도 됩니다

그만 받으려면 같은 주소로 "해지" 라고 보내 주세요.

메일이 오지 않으면 스팸함을 확인해 주세요. Gmail 이라면 검색창 오른쪽의 검색 옵션에서 보낸사람에 `kintmin.ci@gmail.com` 을 넣고 **필터 만들기 → 스팸으로 분류하지 않음** 을 고르면 다음부터는 받은편지함으로 옵니다.

## 구조

Kotlin Multiplatform 으로, 화면을 포함한 코드 대부분을 Android 와 iOS 가 함께 씁니다.
화살표는 의존하는 방향입니다.

```
  android/app       ios            desktop
       │             │                │
       └──────┬──────┘                │
         ┌────┴────┐                  │
         ▼         ▼                  │
  presentation    data ────────┐      │
         │         │           ▼      ▼
         └────┬────┘          file-share
              ▼
            domain
              │
              ▼
             log
```

| 모듈 | 경로 | 내용 |
|---|---|---|
| android/app | `android/app` | Android 앱 |
| ios | `ios/shared`, `ios/app` | iOS 앱 (Kotlin 조립과 Xcode 프로젝트) |
| desktop | `desktop` | Windows·macOS 앱 |
| presentation | `shared/presentation` | 화면 (Compose Multiplatform) |
| data | `shared/data` | 로컬 DB, 네트워크, 파일, 기기 간 파일 공유 |
| domain | `shared/domain` | 앱의 규칙과 동작 |
| log | `shared/log` | 로그 |
| file-share | `shared/file-share` | 데스크톱 앱과 주고받는 공유 프로토콜 |

자세한 구조와 작업 규칙은 [AGENTS.md](AGENTS.md) 에 있습니다.

## 빌드

**준비물**: JDK 17, Android SDK, Python 3.11(Android 빌드가 파이썬 패키지를 받을 때 씁니다).
iOS 는 macOS 와 Xcode 가 필요합니다.

```bash
./gradlew :android:app:assembleDebug   # Android 개발용 앱 (Windows 는 gradlew.bat)
./gradlew :desktop:run                 # 데스크톱 앱 실행
./scripts/setup-ios-vendor.sh          # iOS: 클론 뒤 한 번. 그다음 Xcode 로 ios/app 을 연다
```

- 릴리스 서명 키는 저장소에 없습니다. 없으면 release 빌드는 서명 없이 나오고, 나머지 빌드는 그대로 됩니다.
- 브랜치는 `develop` 에서 따서 PR 을 엽니다. 브랜치 이름과 릴리스 규칙은 AGENTS.md 의 "브랜치와 릴리스" 절을 보세요.

## 라이선스

[MIT](LICENSE)
