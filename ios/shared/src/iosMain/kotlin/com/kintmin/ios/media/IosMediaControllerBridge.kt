package com.kintmin.ios.media

/**
 * Swift 가 구현하는 오디오 재생 창구. 구현은 `MediaControllerEngine`(AVAudioEngine) 이다.
 *
 * **포트(`MediaControllerManager`)를 Swift 가 직접 구현하지 않는 이유는 둘이다.**
 * 그 포트가 쓰는 타입이 `shared:presentation` 에 있고 presentation 은 프레임워크에
 * export 되지 않아 Swift 가 볼 수 없다. 그리고 A-B 구간반복처럼 **앱이 직접 쓴 로직**은
 * 두 플랫폼이 같은 답을 내야 해서 Kotlin 에 한 벌만 둬야 한다.
 *
 * 그래서 여기는 **원시 조작만** 받는다. 큐·셔플·반복은 플레이어가 원래 들고 있는 기능이라
 * Swift 에 맡기고(Android 도 ExoPlayer 에 맡긴다), 구간반복만 [IosMediaControllerManager]
 * 가 쥔다.
 */
interface IosMediaControllerBridge {

    /** 오디오 세션과 엔진을 준비한다. 여러 번 불려도 안전해야 한다. */
    fun initialize()

    /**
     * [items] 로 큐를 세우고 [startMediaId] 부터 재생한다.
     * 같은 목록이면 현재 재생을 끊지 않는 쪽이 자연스럽다.
     */
    fun play(playlistId: Int, startMediaId: Int?, items: List<IosMediaItem>)

    fun pause()
    fun resume()
    fun playPrevious()
    fun playNext()
    fun seek(positionMillis: Long)

    fun isPlaying(): Boolean

    /** 지금 걸려 있는 곡. 큐의 진실은 Swift 가 쥐므로 Kotlin 은 여기서만 읽는다. */
    fun playingItem(): IosMediaItem?

    fun currentPositionMillis(): Long
    fun durationMillis(): Long

    fun setShuffleMode(isShuffle: Boolean)
    fun setRepeatMode(isRepeat: Boolean)
    fun setPlaybackSpeed(speed: Float)
    fun setPlaybackPitchSemitone(semitone: Int)

    /** [atStart] 면 큐 맨 앞에, 아니면 맨 뒤에 넣는다. */
    fun addItem(item: IosMediaItem, atStart: Boolean)

    fun removeItem(mediaId: String)

    fun replaceItem(mediaId: String, item: IosMediaItem)
}

/**
 * `MediaControlData` 의 거울.
 *
 * 필드를 중복 정의하는 것은 presentation 이 프레임워크에 export 되지 않아서다. 원본을
 * export 하면 화면·ViewModel 전체가 Obj-C 헤더로 쏟아진다.
 *
 * [fileFullPath] 와 [artworkFileFullPath] 는 **URL 이 아니라 파일 경로**다. 원본이 그렇다
 * (`playbackFileFullPath`). Swift 가 `URL(fileURLWithPath:)` 로 바꾼다.
 */
data class IosMediaItem(
    val mediaId: String,
    val fileFullPath: String,
    val title: String,
    val description: String,
    val artist: String,
    val durationMillis: Long,
    val artworkFileFullPath: String?,
)
