<!--firebender-plan
name: Android 모듈 재구성
overview: `app`, `presentation`, `platform` 세 모듈을 `android/` 하위 디렉토리로 이동하고 Gradle 모듈 경로를 `:android:app`, `:android:presentation`, `:android:platform`으로 변경합니다. `buildSrc`는 루트에 유지합니다.
todos:
  - id: move-dirs
    content: "android/ 폴더 생성 후 app, presentation, platform 디렉토리 이동"
  - id: update-settings
    content: "settings.gradle.kts의 include 경로 3개 수정"
  - id: update-app-deps
    content: "android/app/build.gradle.kts의 project() 참조 수정"
  - id: update-presentation-deps
    content: "android/presentation/build.gradle.kts의 project() 참조 수정"
  - id: sync-gradle
    content: "Gradle sync 후 빌드 확인"
-->

# Android 모듈 재구성

## 작업 범위

- `app/` → `android/app/` (모듈 경로: `:app` → `:android:app`)
- `presentation/` → `android/presentation/` (모듈 경로: `:presentation` → `:android:presentation`)
- `platform/` → `android/platform/` (모듈 경로: `:platform` → `:android:platform`)
- `buildSrc/` → 루트 유지 (이동 없음)

## 변경 파일 목록

### 1. 디렉토리 이동
- `app/` → `android/app/`
- `presentation/` → `android/presentation/`
- `platform/` → `android/platform/`

### 2. [`settings.gradle.kts`](settings.gradle.kts)
```kotlin
// before
include(":app")
include(":presentation")
include(":platform")

// after
include(":android:app")
include(":android:presentation")
include(":android:platform")
```

### 3. [`android/app/build.gradle.kts`](app/build.gradle.kts) — project 참조 수정
- `project(":presentation")` → `project(":android:presentation")`
- `project(":platform")` → `project(":android:platform")`

### 4. [`android/presentation/build.gradle.kts`](presentation/build.gradle.kts) — project 참조 수정
- `project(":platform")` → `project(":android:platform")`

### 5. `android/platform/build.gradle.kts` — 변경 없음
- 참조하는 모듈이 `:shared:*`만 있으므로 수정 불필요

## 주의 사항
- Gradle 네스티드 모듈은 `include(":android:app")` 선언만으로 `android/app/` 경로를 자동 인식하므로 별도 `android/settings.gradle.kts` 불필요
- `buildSrc`는 Gradle 컨벤션 제약으로 루트에 유지
