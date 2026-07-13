import ComposableArchitecture
import Foundation

struct MediaControllerClient: Sendable {
    var play: @Sendable (_ playlistId: Int, _ startMediaId: Int?, _ items: [MediaControllerItem]) async -> Void
    var pause: @Sendable () async -> Void
    var resume: @Sendable () async -> Void
    var next: @Sendable () async -> Void
    var previous: @Sendable () async -> Void
    var seek: @Sendable (_ seconds: Double) async -> Void
    var snapshots: @Sendable () async -> AsyncStream<MediaControllerSnapshot>
}

extension MediaControllerClient: DependencyKey {
    static let liveValue: MediaControllerClient = .live()
}

extension DependencyValues {
    var mediaControllerClient: MediaControllerClient {
        get { self[MediaControllerClient.self] }
        set { self[MediaControllerClient.self] = newValue }
    }
}

extension MediaControllerClient {
    static func live() -> Self {
        let holder = MediaControllerEngineHolder()
        return Self(
            play: { playlistId, startMediaId, items in
                await MainActor.run {
                    holder.engine.play(playlistId: playlistId, startMediaId: startMediaId, items: items)
                }
            },
            pause: { await MainActor.run { holder.engine.pause() } },
            resume: { await MainActor.run { holder.engine.resume() } },
            next: { await MainActor.run { holder.engine.next() } },
            previous: { await MainActor.run { holder.engine.previous() } },
            seek: { seconds in
                await MainActor.run { holder.engine.seek(seconds: seconds) }
            },
            snapshots: {
                await MainActor.run { holder.engine.snapshotStream() }
            }
        )
    }
}

// AVQueuePlayer는 메인 스레드에서만 만들어야 하므로 첫 사용 시 MainActor 컨텍스트에서 lazy 생성한다.
private final class MediaControllerEngineHolder: @unchecked Sendable {
    @MainActor
    lazy var engine: MediaControllerEngine = MediaControllerEngine()
}
