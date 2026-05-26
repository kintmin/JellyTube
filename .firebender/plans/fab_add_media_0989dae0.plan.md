<!--firebender-plan
name: FAB add media
overview: 메인 검색 탭 FAB의 길게 누름 확장 메뉴에 미디어 파일 가져오기와 데스크톱 파일 공유 화면 이동 버튼을 추가합니다. Quick Share의 파일 선택/가져오기 흐름을 재사용하고, 미디어 세부정보 출처 클릭은 http/https URL만 WebView로 이동하도록 제한합니다.
todos:
  - id: fab-menu-buttons
    content: "Add two expanded FAB actions for media import and desktop file-share navigation"
  - id: main-media-import
    content: "Wire media picker result into shared audio import use case and user feedback"
  - id: desktop-navigation
    content: "Expose main-screen navigation callback to file-share receive screen"
  - id: source-http-only
    content: "Guard audio media detail source click to only open http/https URLs"
  - id: verify-build
    content: "Run targeted Kotlin/Gradle verification after edits"
-->

# 메인 FAB 미디어 추가 및 출처 딥링크 개선

## 변경 범위
- [presentation/src/main/java/com/kintmin/presentation/ui/main/floating_action/MainFloatingActionButton.kt](presentation/src/main/java/com/kintmin/presentation/ui/main/floating_action/MainFloatingActionButton.kt)
  - 길게 눌렀을 때 보이는 확장 영역에 기존 `현재 재생목���` 버튼 외에 다음 버튼 추가:
    - `미디어에서 추가하기`
    - `데스크톱에서 추가하기`
  - 확장 버튼 클릭 후 메뉴를 접도록 처리합니다.
- [presentation/src/main/java/com/kintmin/presentation/ui/main/MainScreen.kt](presentation/src/main/java/com/kintmin/presentation/ui/main/MainScreen.kt)
  - `rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments())`로 `audio/*` 파일 선택 실행.
  - 선택된 URI 목록을 ViewModel로 전달하고 Toast로 결과 표시.
  - `데스크톱에서 추가하기` 클릭 시 상위 내비게이션 콜백을 호출.
- [presentation/src/main/java/com/kintmin/presentation/ui/main/MainViewModel.kt](presentation/src/main/java/com/kintmin/presentation/ui/main/MainViewModel.kt)
  - `ImportSharedAudioMediaUseCase`를 주입받아 Quick Share 설정 화면의 `onFilesSelected` 로직과 같은 방식으로 파일 URI를 가져옵니다.
  - 성공 개수/중복/오류 메시지를 이벤트로 노출합니다.
- [presentation/src/main/java/com/kintmin/presentation/ui/main/navigation/MainScreenNavigation.kt](presentation/src/main/java/com/kintmin/presentation/ui/main/navigation/MainScreenNavigation.kt) 및 [app/src/main/java/com/kintmin/jellytube/MainNavHost.kt](app/src/main/java/com/kintmin/jellytube/MainNavHost.kt)
  - 메인 화면에서 `navigateToSettingFileShareReceiveScreen(navOptions)`로 직접 이동하는 콜백을 추가합니다.
- [presentation/src/main/java/com/kintmin/presentation/ui/audio_media_detail/AudioMediaDetailScreen.kt](presentation/src/main/java/com/kintmin/presentation/ui/audio_media_detail/AudioMediaDetailScreen.kt)
  - 출처 클릭 시 `data.source`가 `http://` 또는 `https://`로 시작할 때만 `navigateToMainSearchTab(data.source)` 호출.
  - `quickShare://...`, `fileShare://...`, 빈 값 등은 클릭해도 무시합니다.

## 검증
- 메인 검색 탭 FAB를 길게 눌렀을 때 세 버튼이 보이는지 확인합니다.
- `미디어에서 추가하기`가 음원 파일 선택기를 열고, 선택한 파일을 라이브러리에 가져오는지 확인합니다.
- `데스크톱에서 추가하기`가 `파일 공유 받기` 화면으로 이동하는지 확인합니다.
- 미디어 세부정보에서 `http://`/`https://` 출처만 WebView 검색 탭으로 이동하고, `quickShare://`/`fileShare://`는 무시되는지 확인합니다.
- 가능하면 `:presentation:compileDebugKotlin` 또는 사용 가능한 Gradle 컴파일 태스크로 타입 오류를 확인합니다.
