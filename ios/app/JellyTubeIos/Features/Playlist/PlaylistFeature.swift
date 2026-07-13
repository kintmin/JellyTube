import ComposableArchitecture
import Foundation

@Reducer
struct PlaylistFeature {
    enum PlaylistTopTab: Hashable, CaseIterable, Sendable {
        case myPlaylists
        case all
        case uncategorized
    }

    @ObservableState
    struct State: Equatable {
        var playlists: [PlaylistItem] = []
        var selectedTopTab: PlaylistTopTab = .myPlaylists
        var newPlaylistTitle = ""
        var isAddSheetPresented = false
        var isLoading = false
        @Presents var alert: AlertState<Action.Alert>?
        var isPlaylistSubscriptionActive = false

        var trimmedNewPlaylistTitle: String {
            newPlaylistTitle.trimmingCharacters(in: .whitespacesAndNewlines)
        }

        var isAddPlaylistButtonDisabled: Bool {
            trimmedNewPlaylistTitle.isEmpty
        }

        var deletablePlaylistIDs: Set<Int> {
            Set(playlists.filter { Self.isPlaylistDeletable(id: $0.id) }.map(\.id))
        }

        var displayedPlaylists: [PlaylistItem] {
            switch selectedTopTab {
            case .myPlaylists:
                return playlists
            case .all, .uncategorized:
                return []
            }
        }

        static func isPlaylistDeletable(id: Int) -> Bool {
            id > 2
        }
    }

    enum Action: BindableAction, Equatable {
        case alert(PresentationAction<Alert>)
        case binding(BindingAction<State>)
        case task
        case playlistSubscriptionFinished
        case playlistsUpdated([PlaylistItem])
        case addButtonTapped
        case addSheetDismissed
        case addPlaylistConfirmed
        case deletePlaylist(Int)
        case operationFailed(String)
        case topTabSelected(PlaylistTopTab)
        case settingsTapped

        enum Alert: Equatable {}
    }

    @Dependency(\.playlistClient) var playlistClient

    var body: some ReducerOf<Self> {
        BindingReducer()
        Reduce { state, action in
            switch action {
            case .alert, .binding:
                return .none

            case .task:
                guard !state.isPlaylistSubscriptionActive else { return .none }
                state.isPlaylistSubscriptionActive = true
                state.isLoading = true
                return .run { send in
                    do {
                        for try await playlists in playlistClient.fetchAll() {
                            await send(.playlistsUpdated(playlists))
                        }
                    } catch is CancellationError {
                    } catch {
                        await send(.operationFailed(error.localizedDescription))
                    }
                    await send(.playlistSubscriptionFinished)
                }
                .cancellable(id: CancelID.playlists, cancelInFlight: true)

            case .playlistSubscriptionFinished:
                state.isPlaylistSubscriptionActive = false
                return .none

            case let .playlistsUpdated(playlists):
                state.playlists = playlists
                state.isLoading = false
                return .none

            case .addButtonTapped:
                state.newPlaylistTitle = ""
                state.isAddSheetPresented = true
                return .none

            case .addSheetDismissed:
                state.isAddSheetPresented = false
                return .none

            case .addPlaylistConfirmed:
                let title = state.trimmedNewPlaylistTitle
                guard !title.isEmpty else { return .none }
                state.isAddSheetPresented = false
                state.newPlaylistTitle = ""
                return .run { send in
                    do {
                        try await playlistClient.add(title)
                    } catch {
                        await send(.operationFailed(error.localizedDescription))
                    }
                }

            case let .deletePlaylist(id):
                guard State.isPlaylistDeletable(id: id) else { return .none }
                return .run { send in
                    do {
                        try await playlistClient.delete(id)
                    } catch {
                        await send(.operationFailed(error.localizedDescription))
                    }
                }

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

            case let .topTabSelected(tab):
                state.selectedTopTab = tab
                return .none

            case .settingsTapped:
                return .none
            }
        }
        .ifLet(\.$alert, action: \.alert)
    }

    private enum CancelID {
        case playlists
    }
}
