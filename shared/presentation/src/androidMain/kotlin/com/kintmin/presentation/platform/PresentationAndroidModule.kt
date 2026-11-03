package com.kintmin.presentation.platform

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * presentation 의 플랫폼 탈출구 중 Android 구현이 필요한 것들을 묶는다.
 *
 * Activity 컨텍스트가 필요한 것(파일 피커·권한 등)은 여기 등록하지 않고
 * `@Composable expect fun remember~()` 로 Composition 에서 만든다.
 * 여기에 등록해 두면 누군가 Application 컨텍스트 인스턴스를 집어 와 쓴다.
 */
val presentationAndroidModule: Module = module {
    single<Toaster> { AndroidToaster(androidContext()) }
}
