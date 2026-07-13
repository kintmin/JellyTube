import ComposableArchitecture
import Foundation

@Reducer
struct PlayerBarFeature {
    @ObservableState
    struct State: Equatable {
        var title: String = ""
        var thumbnailURL: URL? = nil
        var currentSeconds: Int = 0
        var totalSeconds: Int = 0
        var isPlaying: Bool = false
        var nowPlayingAudioMediaId: Int? = nil
        var isSubscriptionActive: Bool = false

        var progress: Double {
            guard totalSeconds > 0 else { return 0 }
            return min(1, max(0, Double(currentSeconds) / Double(totalSeconds)))
        }

        var currentDurationText: String { Self.format(seconds: currentSeconds) }
        var totalDurationText: String { Self.format(seconds: totalSeconds) }

        static func format(seconds: Int) -> String {
            let s = max(0, seconds)
            let h = s / 3600
            let m = (s % 3600) / 60
            let sec = s % 60
            if h > 0 {
                return String(format: "%d:%02d:%02d", h, m, sec)
            }
            return String(format: "%d:%02d", m, sec)
        }
    }

    enum Action: BindableAction, Equatable {
        case binding(BindingAction<State>)
        case task
        case snapshotUpdated(MediaControllerSnapshot)
        case subscriptionFinished
        case playPauseTapped
    }

    @Dependency(\.mediaControllerClient) var mediaControllerClient

    var body: some ReducerOf<Self> {
        BindingReducer()
        Reduce { state, action in
            switch action {
            case .binding:
                return .none

            case .task:
                guard !state.isSubscriptionActive else { return .none }
                state.isSubscriptionActive = true
                return .run { [mediaControllerClient] send in
                    for await snapshot in await mediaControllerClient.snapshots() {
                        await send(.snapshotUpdated(snapshot))
                    }
                    await send(.subscriptionFinished)
                }
                .cancellable(id: CancelID.snapshots, cancelInFlight: true)

            case let .snapshotUpdated(snapshot):
                state.title = snapshot.title
                state.thumbnailURL = snapshot.artworkURL
                state.currentSeconds = Int(snapshot.positionSeconds)
                state.totalSeconds = Int(snapshot.durationSeconds)
                state.isPlaying = snapshot.isPlaying
                state.nowPlayingAudioMediaId = snapshot.currentMediaId
                return .none

            case .subscriptionFinished:
                state.isSubscriptionActive = false
                return .none

            case .playPauseTapped:
                let wasPlaying = state.isPlaying
                return .run { [mediaControllerClient] _ in
                    if wasPlaying {
                        await mediaControllerClient.pause()
                    } else {
                        await mediaControllerClient.resume()
                    }
                }
            }
        }
    }

    private enum CancelID {
        case snapshots
    }
}
