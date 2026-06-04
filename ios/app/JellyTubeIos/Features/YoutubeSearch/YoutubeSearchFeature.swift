import ComposableArchitecture
import Foundation

@Reducer
struct YoutubeSearchFeature {
    @ObservableState
    struct State: Equatable {
        var url = URL(string: "https://m.youtube.com/")!
        var downloadTapCount = 0
    }

    enum Action: Equatable {
        case downloadTapped
    }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .downloadTapped:
                state.downloadTapCount += 1
                return .none
            }
        }
    }
}
