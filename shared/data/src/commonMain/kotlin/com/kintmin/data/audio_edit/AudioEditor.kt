package com.kintmin.data.audio_edit

import com.kintmin.domain.audio_edit.model.AudioEditPart
import com.kintmin.domain.audio_edit.model.AudioWaveform
import kotlinx.coroutines.flow.Flow

/**
 * 음원 파형 추출과 구간 편집(자르기 + 이어붙이기)을 담당한다.
 *
 * 오디오 디코딩·인코딩은 플랫폼 코덱이 필요해 구현은 각 플랫폼 소스셋에 있다.
 */
interface AudioEditor {

    /**
     * 파형 표시용 진폭을 뽑는다. 음원을 [bucketCount]개 구간으로 나눠 구간별 최대 진폭을 담는다.
     *
     * 긴 음원도 바로 그리기 시작할 수 있게, 읽는 도중 앞에서부터 채워진 파형을 주기적으로 내보내고
     * 다 읽으면 완료된 파형을 마지막으로 내보낸다. 읽지 못하면 예외로 끝난다.
     */
    fun extractWaveform(audioFileFullPath: String, bucketCount: Int): Flow<AudioWaveform>

    /**
     * [parts]를 순서대로 이어붙인 편집본을 [outputFileFullPath]에 만든다.
     *
     * @return 만들어진 파일의 **실제 측정 길이(ms)**. 조각 길이의 합은 코덱 프레임 경계 때문에
     *   실제 파일과 수십 ms 차이가 날 수 있는데, 플레이어 탐색바가 이 값을 쓰므로 실측값이어야 한다.
     */
    suspend fun render(
        sourceFileFullPath: String,
        parts: List<AudioEditPart>,
        outputFileFullPath: String,
    ): Result<Long>
}
