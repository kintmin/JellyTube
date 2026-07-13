import ComposableArchitecture
import SwiftUI

struct PlaylistDetailScreenView: View {
    @Bindable var store: StoreOf<PlaylistDetailFeature>

    var body: some View {
        ZStack {
            GlassBackground()
            content
        }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            store.send(.task)
        }
        .alert($store.scope(state: \.alert, action: \.alert))
    }

    @ViewBuilder
    private var content: some View {
        if store.isLoading && store.tracks.isEmpty {
            ProgressView("불러오는 중")
        } else if store.tracks.isEmpty {
            Text("트랙이 없습니다")
                .foregroundStyle(.secondary)
        } else {
            ScrollView {
                LazyVStack(spacing: 0) {
                    ForEach(store.tracks) { item in
                        PlaylistDetailRow(
                            item: item,
                            isNowPlaying: item.id == store.nowPlayingAudioMediaId,
                            onTap: { store.send(.rowTapped(item.id)) }
                        )
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
            }
            .contentMargins(.bottom, 160, for: .scrollContent)
        }
    }
}

#Preview("With data (Yunomix playing)") {
    NavigationStack {
        PlaylistDetailScreenView(
            store: Store(
                initialState: PlaylistDetailFeature.State(
                    playlistId: 3,
                    tracks: [
                        PlaylistDetailItem(id: 1, title: "추억속의 그대", artist: "dosii",
                                           durationSeconds: 233, coverImageURL: nil,
                                           audioFileURL: URL(fileURLWithPath: "/dev/null")),
                        PlaylistDetailItem(id: 2, title: "American Idiot", artist: "Green Day",
                                           durationSeconds: 233, coverImageURL: nil,
                                           audioFileURL: URL(fileURLWithPath: "/dev/null")),
                        PlaylistDetailItem(id: 3, title: "Sugar Honey ice & tea", artist: "BMTH",
                                           durationSeconds: 233, coverImageURL: nil,
                                           audioFileURL: URL(fileURLWithPath: "/dev/null")),
                        PlaylistDetailItem(id: 4, title: "스물 다섯, 스물하나", artist: "자우림",
                                           durationSeconds: 233, coverImageURL: nil,
                                           audioFileURL: URL(fileURLWithPath: "/dev/null")),
                        PlaylistDetailItem(id: 5, title: "Yunomix vol.4", artist: "Yunomi",
                                           durationSeconds: 233, coverImageURL: nil,
                                           audioFileURL: URL(fileURLWithPath: "/dev/null")),
                        PlaylistDetailItem(id: 6, title: "London Calling", artist: "The Clash",
                                           durationSeconds: 233, coverImageURL: nil,
                                           audioFileURL: URL(fileURLWithPath: "/dev/null")),
                        PlaylistDetailItem(id: 7, title: "Dani California", artist: "Red Hot Chili Peppers",
                                           durationSeconds: 233, coverImageURL: nil,
                                           audioFileURL: URL(fileURLWithPath: "/dev/null")),
                    ],
                    isLoading: false,
                    nowPlayingAudioMediaId: 5
                )
            ) {
                EmptyReducer()
            }
        )
    }
}

#Preview("Loading") {
    NavigationStack {
        PlaylistDetailScreenView(
            store: Store(
                initialState: PlaylistDetailFeature.State(
                    playlistId: 3,
                    isLoading: true
                )
            ) {
                EmptyReducer()
            }
        )
    }
}

#Preview("Empty") {
    NavigationStack {
        PlaylistDetailScreenView(
            store: Store(
                initialState: PlaylistDetailFeature.State(
                    playlistId: 3,
                    isLoading: false
                )
            ) {
                EmptyReducer()
            }
        )
    }
}
