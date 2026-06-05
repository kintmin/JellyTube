import ComposableArchitecture
import SwiftUI

struct YoutubeSearchView: View {
    let store: StoreOf<YoutubeSearchFeature>

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottomTrailing) {
                YoutubeWebView(url: store.url)
                    .ignoresSafeArea(edges: .bottom)

                GlassFloatingButton(systemName: "arrow.down.circle.fill") {
                    store.send(.downloadTapped)
                }
                .padding(24)
            }
            .navigationTitle("YouTube")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

#Preview {
    YoutubeSearchView(
        store: Store(initialState: YoutubeSearchFeature.State()) {
            YoutubeSearchFeature()
        }
    )
}
