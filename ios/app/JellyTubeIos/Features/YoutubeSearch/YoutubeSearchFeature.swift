import ComposableArchitecture
import Foundation

@Reducer
struct YoutubeSearchFeature {
    @ObservableState
    struct State: Equatable {
        var url = URL(string: "https://m.youtube.com/")!
        var currentPageUrl: URL?
        var isDownloading = false
        @Presents var alert: AlertState<Action.Alert>?
    }

    enum Action: BindableAction, Equatable {
        case binding(BindingAction<State>)
        case downloadTapped
        case currentUrlChanged(URL)
        case downloadRequestSucceeded
        case downloadRequestFailed(String)
        case alert(PresentationAction<Alert>)

        enum Alert: Equatable {}
    }

    @Dependency(\.youtubeDownloadClient) var youtubeDownloadClient

    private enum CancelId: Hashable { case download }

    var body: some ReducerOf<Self> {
        BindingReducer()
        Reduce { state, action in
            switch action {
            case .binding:
                return .none

            case .currentUrlChanged(let url):
                state.currentPageUrl = url
                return .none

            case .downloadTapped:
                guard !state.isDownloading else { return .none }
                guard let pageUrl = state.currentPageUrl,
                      isYoutubeWatchUrl(pageUrl) else {
                    state.alert = AlertState {
                        TextState("다운로드 실패")
                    } message: {
                        TextState("유튜브 영상 페이지에서 눌러주세요.")
                    }
                    return .none
                }
                state.isDownloading = true
                let downloadUrl = pageUrl.absoluteString
                return .run { send in
                    do {
                        try await youtubeDownloadClient.download(downloadUrl)
                        await send(.downloadRequestSucceeded)
                    } catch {
                        await send(.downloadRequestFailed(error.localizedDescription))
                    }
                }
                .cancellable(id: CancelId.download, cancelInFlight: true)

            case .downloadRequestSucceeded:
                state.isDownloading = false
                state.alert = AlertState {
                    TextState("다운로드 완료")
                } message: {
                    TextState("플레이리스트에 추가했습니다.")
                }
                return .none

            case .downloadRequestFailed(let message):
                state.isDownloading = false
                state.alert = AlertState {
                    TextState("다운로드 실패")
                } message: {
                    TextState(message)
                }
                return .none

            case .alert:
                return .none
            }
        }
        .ifLet(\.$alert, action: \.alert)
    }

    private func isYoutubeWatchUrl(_ url: URL) -> Bool {
        guard let host = url.host?.lowercased() else { return false }
        let isYoutubeHost = host.contains("youtube.com") || host == "youtu.be"
        guard isYoutubeHost else { return false }
        if host == "youtu.be" { return !url.path.isEmpty && url.path != "/" }
        return url.path.contains("/watch") || url.path.contains("/shorts/")
    }
}
