package com.kintmin.presentation.platform

/**
 * 플랫폼이 **제공하지 않기로 한** 기능.
 *
 * 포트가 미지원 스텁으로 막혀 있어도 진입점이 보이면 사용자는 누른다. 그러면 아무 일도
 * 일어나지 않는 화면이 뜨거나 버튼이 먹통이 된다 — 막는 것과 **숨기는 것은 별개 일**이다.
 *
 * `expect val` 인 이유: 값이 컴파일 시점에 정해지고 런타임 상태가 아니다. 포트(Koin)로
 * 만들면 런타임에 터질 수 있고, 화면마다 인자로 흘리면 `PlaylistDetailListItemView` 처럼
 * 다섯 단계 아래 있는 자리까지 시그니처가 오염된다. 커스텀 `CompositionLocal` 은
 * `shared/presentation/AGENTS.md` 가 금지한다.
 *
 * **이 값을 "기능 플래그" 로 쓰지 않는다.** 플랫폼이 그 기능을 아예 제공하지 않을 때만
 * false 다. 켜고 끄는 설정은 DataStore 에 둔다.
 */
expect val isAudioEditSupported: Boolean

/** PC(데스크톱)에서 파일을 받는 기능. */
expect val isFileShareReceiveSupported: Boolean

/** 음원을 기기 공용 저장소(벨소리/알람/알림음)에 내보내는 기능. */
expect val isDeviceSaveSupported: Boolean
