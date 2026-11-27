package com.kintmin.presentation.secret_mode

import com.kintmin.domain.secret_mode.usecase.FetchSecretAudioMediaIdListFlowUseCase
import kotlinx.coroutines.flow.first

/**
 * 비밀 곡의 제목을 밖으로 내보내기 전에 한 번 묻는다. **한 화면만 막으면 다른 화면으로 같은
 * 제목이 나가므로** 외부 검색을 거는 화면이 모두 이것을 쓴다.
 *
 * **화면마다 하나씩 갖는다** — 고른 것은 그 화면에 머무는 동안만 유효하다.
 */
class ExternalSearchConsent(
    private val fetchSecretAudioMediaIdListFlowUseCase: FetchSecretAudioMediaIdListFlowUseCase,
) {
    private var isRequired = false
    private var queryAwaitingConsent: String? = null

    /** 물을지 정하기 전에는 검색을 시작할 수 없다. 화면이 뜰 때 한 번 부른다. */
    suspend fun prepare(audioMediaId: Int) {
        isRequired = audioMediaId in fetchSecretAudioMediaIdListFlowUseCase().first()
    }

    /** true 면 [query] 를 쥐고 기다린다. 부르는 쪽은 묻는 화면을 띄운다. */
    fun shouldAsk(query: String): Boolean {
        if (!isRequired) return false
        queryAwaitingConsent = query
        return true
    }

    /** 고른 뒤 이어서 돌릴 검색어. 쥔 것이 없으면 null 이다. */
    fun grant(): String? {
        isRequired = false
        return queryAwaitingConsent
    }
}
