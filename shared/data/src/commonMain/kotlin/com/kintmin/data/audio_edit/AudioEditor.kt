package com.kintmin.data.audio_edit

import com.kintmin.domain.audio_edit.model.AudioEditSegment

/**
 * 음원 파형 추출과 구간 편집(자르기 + 이어붙이기)을 담당한다.
 *
 * 오디오 디코딩·인코딩은 플랫폼 코덱이 필요해 구현은 각 플랫폼 소스셋에 있다.
 */
interface AudioEditor {

    /**
     * 파형 표시용 진폭 배열을 뽑는다. 음원을 [bucketCount]개 구간으로 나눠 구간별 최대 진폭을 담는다.
     *
     * 반환값의 크기는 [bucketCount]와 같고, 각 값은 0 이상이다. 상대적인 크기만 의미가 있어
     * 화면에 그릴 때는 최대값으로 정규화해서 쓴다.
     */
    suspend fun extractWaveform(audioFileFullPath: String, bucketCount: Int): Result<List<Int>>

    /**
     * [segments]를 순서대로 이어붙인 편집본을 [outputFileFullPath]에 만든다.
     *
     * @return 만들어진 파일의 **실제 측정 길이(ms)**. 구간 길이의 합은 코덱 프레임 경계 때문에
     *   실제 파일과 수십 ms 차이가 날 수 있는데, 플레이어 탐색바가 이 값을 쓰므로 실측값이어야 한다.
     */
    suspend fun render(
        sourceFileFullPath: String,
        segments: List<AudioEditSegment>,
        outputFileFullPath: String,
    ): Result<Long>
}
