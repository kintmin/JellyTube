package com.kintmin.domain.secret_mode.model

enum class SecretModeEndReason {
    USER,
    TIMER,

    /** 기기가 잠들어 타이머가 늦는 사이 종료 시각이 지나, 앱이 다시 보일 때 끈다. */
    EXPIRED_ON_RETURN,

    /** 앞 실행이 비밀모드 중에 죽었을 때의 뒷정리. 켜져 있던 상태가 메모리와 함께 사라져 끔 로그가 남지 않는다. */
    APP_LAUNCH,
}
