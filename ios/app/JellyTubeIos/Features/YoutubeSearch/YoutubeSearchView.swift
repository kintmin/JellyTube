import ComposableArchitecture
import SwiftUI

struct YoutubeSearchView: View {
    @Bindable var store: StoreOf<YoutubeSearchFeature>

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottomTrailing) {
                YoutubeWebView(
                    url: store.url,
                    onCurrentUrlChanged: { store.send(.currentUrlChanged($0)) }
                )
                .ignoresSafeArea(edges: .bottom)

                GlassFloatingButton(systemName: "arrow.down.circle.fill") {
                    store.send(.downloadTapped)
                }
                .padding(24)
                .disabled(store.isDownloading)
            }
            .navigationTitle("YouTube")
            .navigationBarTitleDisplayMode(.inline)
        }
        .alert($store.scope(state: \.alert, action: \.alert))
    }
}

#Preview {
    YoutubeSearchView(
        store: Store(initialState: YoutubeSearchFeature.State()) {
            EmptyReducer()
        }
    )
}
