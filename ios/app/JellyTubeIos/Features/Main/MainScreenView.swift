import ComposableArchitecture
import SwiftUI

struct MainScreenView: View {
    @Bindable var store: StoreOf<MainFeature>

    init(store: StoreOf<MainFeature> = Store(initialState: MainFeature.State()) {
        MainFeature()
    }) {
        self.store = store
    }

    var body: some View {
        ZStack {
            GlassBackground()
            currentScreen
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            VStack(spacing: 8) {
                PlayerBarScreenView(
                    store: store.scope(state: \.playerBar, action: \.playerBar)
                )
                CustomBottomBar(
                    selection: Binding(
                        get: { store.selectedTab },
                        set: { store.send(.binding(.set(\.selectedTab, $0))) }
                    )
                )
            }
            .padding(.horizontal, 12)
            .padding(.bottom, 4)
        }
    }

    @ViewBuilder
    private var currentScreen: some View {
        switch store.selectedTab {
        case .playlist:
            PlaylistScreenView(
                store: store.scope(state: \.playlist, action: \.playlist)
            )
        case .youtubeSearch:
            YoutubeSearchView(
                store: store.scope(state: \.youtubeSearch, action: \.youtubeSearch)
            )
        }
    }
}

#Preview {
    MainScreenView()
}
