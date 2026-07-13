import ComposableArchitecture
import Foundation

@Reducer
struct PlaylistDetailFeature {
    @ObservableState
    struct State: Equatable {
        let playlistId: Int
        var tracks: [PlaylistDetailItem] = []
        var isLoading = false
        var nowPlayingAudioMediaId: Int? = nil
        var isSubscriptionActive = false
        @Presents var alert: AlertState<Action.Alert>?
    }

    enum Action: BindableAction, Equatable {
        case alert(PresentationAction<Alert>)
        case binding(BindingAction<State>)
        case task
        case subscriptionFinished
        case tracksUpdated([PlaylistDetailItem])
        case operationFailed(String)

        enum Alert: Equatable {}
    }

    @Dependency(\.playlistDetailClient) var playlistDetailClient

    var body: some ReducerOf<Self> {
        BindingReducer()
        Reduce { state, action in
            switch action {
            case .alert, .binding:
                return .none

            case .task:
                guard !state.isSubscriptionActive else { return .none }
                state.isSubscriptionActive = true
                state.isLoading = true
                let playlistId = state.playlistId
                return .run { send in
                    do {
                        for try await tracks in playlistDetailClient.fetch(playlistId) {
                            await send(.tracksUpdated(tracks))
                        }
                    } catch is CancellationError {
                    } catch {
                        await send(.operationFailed(error.localizedDescription))
                    }
                    await send(.subscriptionFinished)
                }
                .cancellable(id: CancelID.tracks, cancelInFlight: true)

            case .subscriptionFinished:
                state.isSubscriptionActive = false
                return .none

            case let .tracksUpdated(tracks):
                state.tracks = tracks
                state.isLoading = false
                return .none

            case let .operationFailed(message):
                state.alert = AlertState {
                    TextState("문제가 발생했어요")
                } actions: {
                    ButtonState(role: .cancel) {
                        TextState("확인")
                    }
                } message: {
                    TextState(message)
                }
                state.isLoading = false
                return .none
            }
        }
        .ifLet(\.$alert, action: \.alert)
    }

    private enum CancelID {
        case tracks
    }
}
