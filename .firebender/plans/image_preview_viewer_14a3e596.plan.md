<!--firebender-plan
name: Image Preview Viewer
overview: `ui/common`에 재사용 가능한 전체화면 이미지 뷰어를 추가하고, 플레이리스트 헤더 이미지와 오디오 상세 이미지 클릭 시 이를 표시하도록 연결합니다.
todos:
  - id: add-common-viewer
    content: "Create reusable full-screen image viewer in ui/common using existing ZoomableView"
  - id: wire-playlist-header
    content: "Open image viewer from PlaylistDetailHeaderView image click"
  - id: wire-audio-detail
    content: "Open image viewer from AudioMediaDetailScreen image click"
  - id: verify-build
    content: "Run a focused Gradle or lint check for the touched code"
-->

# Image Preview Viewer

## Assumptions
- "해당 뷰를 보여줘"는 새 Navigation destination이 아니라, 현재 화면 위에 뜨는 **전체화면 오버레이/Dialog**로 구현합니다.
- 기존 `ZoomableView`의 `ZoomLimitMode.Auto`를 사용해 이미지가 가로로 길면 가로 fit, 세로로 길면 세로 fit 되도록 합니다.
- X 아이콘은 화면을 탭하면 토글되고, 마지막 터치 후 짧은 시간이 지나면 자동으로 숨깁니다.

## Changes
- Add a reusable composable in [`presentation/src/main/java/com/kintmin/presentation/ui/common`](presentation/src/main/java/com/kintmin/presentation/ui/common), e.g. `FullScreenImageViewer.kt`.
  - `Dialog(properties = DialogProperties(usePlatformDefaultWidth = false))` 기반 전체화면 표시
  - black background
  - top-right close `IconButton`
  - `ZoomableView(zoomLimitMode = ZoomLimitMode.Auto, contentAlignment = ZoomContentAlignment.Middle)` wrapping an `AsyncImage`
  - `ContentScale.Fit` + image intrinsic size measurement path so `ZoomableView` can determine portrait/landscape fit correctly
- Update [`PlaylistDetailHeaderView.kt`](presentation/src/main/java/com/kintmin/presentation/ui/playlist_detail/header/PlaylistDetailHeaderView.kt).
  - image path가 있을 때만 클릭 가능하게 하고, 클릭 시 common viewer 표시
  - 기존 `AsyncImage` 카드 스타일은 유지
- Update [`AudioMediaDetailScreen.kt`](presentation/src/main/java/com/kintmin/presentation/ui/audio_media_detail/AudioMediaDetailScreen.kt).
  - image path가 있을 때만 클릭 가능하게 하고, 클릭 시 common viewer 표시
  - 기존 상세 화면 레이아웃과 이미지 스타일은 유지

## Verification
- Run focused lint/compile check if available, preferably `./gradlew :presentation:compileDebugKotlin` or nearest project-supported Gradle task.
- Confirm no new Compose/lint diagnostics in the touched Kotlin files.
