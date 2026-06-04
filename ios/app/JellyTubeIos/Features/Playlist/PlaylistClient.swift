import ComposableArchitecture
import Foundation
import shared

struct PlaylistClient: Sendable {
    var fetchAll: @Sendable () -> AsyncThrowingStream<[PlaylistItem], Error>
    var add: @Sendable (_ title: String) async throws -> Void
    var delete: @Sendable (_ id: Int) async throws -> Void
}

extension PlaylistClient: DependencyKey {
    static let liveValue = Self.kmp()
}

extension DependencyValues {
    var playlistClient: PlaylistClient {
        get { self[PlaylistClient.self] }
        set { self[PlaylistClient.self] = newValue }
    }
}

extension PlaylistClient {
    static func kmp(
        fetchAllBridge: IosFetchAllPlaylistFlowUseCaseBridge = IosPlaylistUseCaseBridgeKt.createIosFetchAllPlaylistFlowUseCaseBridge(),
        addBridge: IosAddNewPlaylistUseCaseBridge = IosPlaylistUseCaseBridgeKt.createIosAddNewPlaylistUseCaseBridge(),
        deleteBridge: IosDeletePlaylistUseCaseBridge = IosPlaylistUseCaseBridgeKt.createIosDeletePlaylistUseCaseBridge()
    ) -> Self {
        Self(
            fetchAll: {
                AsyncThrowingStream { continuation in
                    let task = Task {
                        do {
                            for try await playlists in fetchAllBridge.invoke() {
                                continuation.yield(
                                    playlists.map { playlist in
                                        PlaylistItem(
                                            id: Int(playlist.id),
                                            title: playlist.name,
                                            description: playlist.description_,
                                            audioMediaCount: Int(playlist.audioMediaCount)
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
            },
            add: { title in
                _ = try await addBridge.invoke(title: title)
            },
            delete: { id in
                try await deleteBridge.invoke(id: Int32(id))
            }
        )
    }
}
