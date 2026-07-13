import ComposableArchitecture
import Foundation
import shared

struct PlaylistDetailClient: Sendable {
    var fetch: @Sendable (_ playlistId: Int) -> AsyncThrowingStream<[PlaylistDetailItem], Error>
}

extension PlaylistDetailClient: DependencyKey {
    static let liveValue = Self.kmp()
}

extension DependencyValues {
    var playlistDetailClient: PlaylistDetailClient {
        get { self[PlaylistDetailClient.self] }
        set { self[PlaylistDetailClient.self] = newValue }
    }
}

extension PlaylistDetailClient {
    static func kmp(
        fetchBridge: IosFetchAudioMediaListFlowUseCaseBridge = IosAudioTrackUseCaseBridgeKt.createIosFetchAudioMediaListFlowUseCaseBridge()
    ) -> Self {
        Self(
            fetch: { playlistId in
                AsyncThrowingStream { continuation in
                    let task = Task {
                        do {
                            for try await aggregates in fetchBridge.invoke(playlistId: Int32(playlistId)) {
                                continuation.yield(
                                    aggregates.map { aggregate in
                                        let audioMedia = aggregate.audioMedia
                                        let seconds = IosAudioTrackUseCaseBridgeKt
                                            .audioMediaAudioDurationSeconds(audioMedia: audioMedia)
                                            .map { Int(truncating: $0) }
                                        return PlaylistDetailItem(
                                            id: Int(audioMedia.id),
                                            title: audioMedia.name,
                                            artist: audioMedia.artist,
                                            durationSeconds: seconds,
                                            coverImageURL: audioMedia.imageFileFullPath.flatMap { path in
                                                path.isEmpty ? nil : URL(fileURLWithPath: path)
                                            },
                                            audioFileURL: URL(fileURLWithPath: audioMedia.audioFileFullPath)
                                        )
                                    }
                                )
                            }
                            continuation.finish()
                        } catch {
                            continuation.finish(throwing: error)
                        }
                    }
                    continuation.onTermination = { _ in task.cancel() }
                }
            }
        )
    }
}
