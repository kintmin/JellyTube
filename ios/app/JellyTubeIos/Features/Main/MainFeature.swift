import ComposableArchitecture
import Foundation

enum MainTab: Hashable {
    case playlist
    case youtubeSearch
}

@Reducer
struct MainFeature {
    @ObservableState
    struct State: Equatable {
        var selectedTab: MainTab = .playlist
        var playlist = PlaylistFeature.State()
        var youtubeSearch = YoutubeSearchFeature.State()
    }

    enum Action: BindableAction, Equatable {
        case binding(BindingAction<State>)
        case playlist(PlaylistFeature.Action)
        case youtubeSearch(YoutubeSearchFeature.Action)
    }

    var body: some ReducerOf<Self> {
        BindingReducer()
        Scope(state: \.playlist, action: \.playlist) {
            PlaylistFeature()
        }
        Scope(state: \.youtubeSearch, action: \.youtubeSearch) {
            YoutubeSearchFeature()
        }
        Reduce { state, action in
            switch action {
            case .binding, .playlist, .youtubeSearch:
                return .none
            }
        }
    }
}
