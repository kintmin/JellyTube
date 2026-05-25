<!--firebender-plan
name: common dialog refactor
overview: PlaylistAddDialog의 커스텀 scrim/surface 패턴을 공통 Dialog 컴포저블로 추출하고, 삭제 Dialog 2개 및 PlayerDetail의 속도/피치 메뉴에 동일한 Dialog 기반 UI를 적용합니다.
todos:
  - id: create-common-dialog
    content: "Add a reusable custom dialog composable with scrim, surface, and outside-dismiss option"
  - id: apply-playlist-dialogs
    content: "Refactor PlaylistAddDialog, PlaylistDeleteDialog, and DeleteFullAudioMediaListDialog to use the common dialog"
  - id: split-player-dialogs
    content: "Move playback speed and pitch menu UI into player_detail/dialog dialog files and wire PlayerDetailScreen to them"
  - id: verify-build-lints
    content: "Run compile or lint checks and fix issues introduced by the refactor"
-->

# 공통 Dialog 적용 계획

## 판단

- **공통 Dialog로 분리하는 방향에 동의합니다.** 현재 [PlaylistAddDialog.kt](presentation/src/main/java/com/kintmin/presentation/ui/main/playlist/dialog/PlaylistAddDialog.kt)는 `DialogProperties(usePlatformDefaultWidth = false)`, `FLAG_DIM_BEHIND` 제거, `MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)`, `MaterialTheme.colorScheme.surface`를 직접 조합하고 있습니다.
- [PlaylistDeleteDialog.kt](presentation/src/main/java/com/kintmin/presentation/ui/main/playlist/dialog/PlaylistDeleteDialog.kt)와 [DeleteFullAudioMediaListDialog.kt](presentation/src/main/java/com/kintmin/presentation/ui/playlist_edit/dialog/DeleteFullAudioMediaListDialog.kt)는 기본 `Dialog + Surface`라서 배경/클릭-dismiss/Surface 색상 정책이 다릅니다.
- [PlayerDetailScreen.kt](presentation/src/main/java/com/kintmin/presentation/ui/player_detail/PlayerDetailScreen.kt)의 `PlaybackSpeedMenu`, `PlaybackPitchMenu`는 이미 비슷한 scrim overlay를 직접 구현하고 있으므로 Dialog 파일로 분리하면서 공통 Dialog를 쓰는 게 적절합니다.

## 구현 방향

- [ui/common](presentation/src/main/java/com/kintmin/presentation/ui/common/) 아래에 공통 컴포저블을 추가합니다.
  - 이름 후보: `JellyTubeDialog`
  - 역할: `showDialog` 가 false면 return, true면 전체 화면 커스텀 scrim과 surface container 제공
  - 옵션: `dismissOnClickOutside: Boolean = true`
  - 기본값: scrim은 `MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)`, surface는 `MaterialTheme.colorScheme.surface`, shape는 `RoundedCornerShape(16.dp)`
- [PlaylistAddDialog.kt](presentation/src/main/java/com/kintmin/presentation/ui/main/playlist/dialog/PlaylistAddDialog.kt)를 공통 Dialog 사용으로 교체합니다.
  - 현재 동작과 레이아웃은 유지하고, 중복된 `Dialog`, `SideEffect`, window flag, outer `Box`, `Surface` 코드를 제거합니다.
- [PlaylistDeleteDialog.kt](presentation/src/main/java/com/kintmin/presentation/ui/main/playlist/dialog/PlaylistDeleteDialog.kt)에 같은 공통 Dialog를 적용합니다.
  - 기존 `Dialog(onDismissRequest = {})`는 사용자가 원하는 “뒤 배경 누르면 꺼짐”과 맞지 않으므로 `onDismiss`가 호출되도록 변경합니다.
  - Dialog background를 `MaterialTheme.colorScheme.surface`로 통일합니다.
- [DeleteFullAudioMediaListDialog.kt](presentation/src/main/java/com/kintmin/presentation/ui/playlist_edit/dialog/DeleteFullAudioMediaListDialog.kt)에 같은 공통 Dialog를 적용합니다.
  - 기존 문구/버튼 동작은 유지합니다.
  - shape는 공통 기본 16dp로 맞추되, 꼭 기존 4dp를 유지해야 한다면 공통 Dialog의 `shape` 옵션으로 조정할 수 있게 합니다.
- [player_detail/dialog](presentation/src/main/java/com/kintmin/presentation/ui/player_detail/dialog/) 폴더를 만들고 아래 파일을 분리합니다.
  - `PlaybackSpeedDialog.kt`: 기존 `PlaybackSpeedMenu` UI 이동 및 Dialog 적용
  - `PlaybackPitchDialog.kt`: 기존 `PlaybackPitchMenu` UI 이동 및 Dialog 적용
  - 필요 시 공통 helper/chip 함수는 두 파일 중복을 피하기 위해 같은 패키지의 작은 private/internal 파일로 분리하거나, 최소 변경을 위해 각 Dialog 파일에 필요한 범위만 이동합니다.
- [PlayerDetailScreen.kt](presentation/src/main/java/com/kintmin/presentation/ui/player_detail/PlayerDetailScreen.kt)는 새 Dialog 컴포저블 호출만 남기고, 이동된 private 함수/상수/확장함수 및 사용하지 않는 import를 정리합니다.

## 검증

- Android/Kotlin 컴파일 대상 변경이므로 우선 `presentation` 모듈 빌드 또는 가능한 Gradle compile task를 실행합니다.
- 변경 파일 대상 lints를 확인하고, unused import/private symbol 오류가 있으면 정리합니다.
- Preview는 기존 `PlaylistAddDialogPreview`, `PlaylistDeleteDialogPreview`, `DeleteFullAudioMediaListDialogPreview`가 유지되도록 합니다.
