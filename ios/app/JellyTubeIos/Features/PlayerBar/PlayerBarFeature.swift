import ComposableArchitecture
import Foundation

@Reducer
struct PlayerBarFeature {
    @ObservableState
    struct State: Equatable {
        var title: String = "Yunomix vol.4"
        var thumbnailURL: URL? = nil
        var currentSeconds: Int = 22 * 60 + 30
        var totalSeconds: Int = 43 * 60 + 35
        var isPlaying: Bool = false

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
        case playPauseTapped
    }

    var body: some ReducerOf<Self> {
        BindingReducer()
        Reduce { state, action in
            switch action {
            case .binding:
                return .none
            case .playPauseTapped:
                state.isPlaying.toggle()
                return .none
            }
        }
    }
}
