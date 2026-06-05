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
        TabView(selection: $store.selectedTab) {
            PlaylistScreenView(
                store: store.scope(state: \.playlist, action: \.playlist)
            )
            .tabItem {
                Label("Playlist", systemImage: "music.note.list")
            }
            .tag(MainTab.playlist)

            YoutubeSearchView(
                store: store.scope(state: \.youtubeSearch, action: \.youtubeSearch)
            )
            .tabItem {
                Label("YouTube", systemImage: "play.rectangle")
            }
            .tag(MainTab.youtubeSearch)
        }
    }
}

#Preview {
    MainScreenView()
}
