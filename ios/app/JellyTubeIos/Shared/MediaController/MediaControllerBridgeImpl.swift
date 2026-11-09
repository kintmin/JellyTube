import Foundation
import shared

/// Kotlin 의 `IosMediaControllerBridge` 를 `MediaControllerEngine` 에 잇는다.
///
/// 이 창구는 **원시 조작만** 받는다. A-B 구간반복과 현재 플레이리스트 추적은 Kotlin 쪽
/// `IosMediaControllerManager` 가 쥐고 있다 — 두 플랫폼이 같은 답을 내야 하는 로직이라서다.
///
/// **호출은 전부 메인 스레드에서 온다.** 화면 인텐트도 재생 상태 폴링도 Compose 의 메인
/// 디스패처에서 돈다. 엔진이 `@MainActor` 라 `assumeIsolated` 로 그 약속을 코드에 박아 둔다 —
/// 다른 스레드에서 부르면 조용히 어긋나지 않고 바로 죽는다.
final class MediaControllerBridgeImpl: IosMediaControllerBridge {

    private let engine: MediaControllerEngine

    @MainActor
    init() {
        engine = MediaControllerEngine()
    }

    func initialize() {
        // 엔진은 생성 시점에 오디오 그래프와 세션 관찰을 세운다. 여기서 할 일이 따로 없다.
    }

    func play(playlistId: Int32, startMediaId: KotlinInt?, items: [IosMediaItem]) {
        onMain {
            $0.play(
                playlistId: Int(playlistId),
                startMediaId: startMediaId?.intValue,
                items: items.map { $0.toEngineItem() }
            )
        }
    }

    func pause() { onMain { $0.pause() } }

    func resume() { onMain { $0.resume() } }

    func playPrevious() { onMain { $0.previous() } }

    func playNext() { onMain { $0.next() } }

    func seek(positionMillis: Int64) {
        onMain { $0.seek(seconds: Double(positionMillis) / 1000.0) }
    }

    func isPlaying() -> Bool { onMain { $0.isPlayingNow } }

    func playingItem() -> IosMediaItem? { onMain { $0.currentItem?.toSharedItem() } }

    func currentPositionMillis() -> Int64 { onMain { $0.positionMillis } }

    func durationMillis() -> Int64 { onMain { $0.durationMillis } }

    func setShuffleMode(isShuffle: Bool) { onMain { $0.setShuffle(isShuffle) } }

    func setRepeatMode(isRepeat: Bool) { onMain { $0.setRepeatAll(isRepeat) } }

    func setPlaybackSpeed(speed: Float) { onMain { $0.setSpeed(speed) } }

    func setPlaybackPitchSemitone(semitone: Int32) {
        onMain { $0.setPitchSemitone(Int(semitone)) }
    }

    func addItem(item: IosMediaItem, atStart: Bool) {
        onMain { $0.addItem(item.toEngineItem(), atStart: atStart) }
    }

    func removeItem(mediaId: String) {
        guard let id = Int(mediaId) else { return }
        onMain { $0.removeItem(mediaId: id) }
    }

    func replaceItem(mediaId: String, item: IosMediaItem) {
        guard let id = Int(mediaId) else { return }
        onMain { $0.replaceItem(mediaId: id, with: item.toEngineItem()) }
    }

    private func onMain<T>(_ body: @MainActor (MediaControllerEngine) -> T) -> T {
        MainActor.assumeIsolated { body(engine) }
    }
}

private extension IosMediaItem {
    /// `fileFullPath` 는 URL 이 아니라 파일 경로다 — 원본(`playbackFileFullPath`)이 그렇다.
    func toEngineItem() -> MediaControllerItem {
        MediaControllerItem(
            id: Int(mediaId) ?? 0,
            title: title,
            description: description,
            artist: artist,
            fileURL: URL(fileURLWithPath: fileFullPath),
            artworkURL: artworkFileFullPath.map { URL(fileURLWithPath: $0) },
            durationSeconds: Int(durationMillis / 1000)
        )
    }
}

private extension MediaControllerItem {
    func toSharedItem() -> IosMediaItem {
        IosMediaItem(
            mediaId: String(id),
            fileFullPath: fileURL.path,
            title: title,
            description: description,
            artist: artist,
            durationMillis: Int64((durationSeconds ?? 0) * 1000),
            artworkFileFullPath: artworkURL?.path
        )
    }
}
