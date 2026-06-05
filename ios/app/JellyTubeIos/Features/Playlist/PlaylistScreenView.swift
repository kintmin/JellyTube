import ComposableArchitecture
import SwiftUI

struct PlaylistScreenView: View {
    @Bindable var store: StoreOf<PlaylistFeature>

    var body: some View {
        NavigationStack {
            ZStack {
                GlassBackground()
                content
            }
            .navigationTitle("Playlists")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        store.send(.addButtonTapped)
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $store.isAddSheetPresented) {
                addPlaylistSheet
                    .presentationDetents([.height(220)])
                    .presentationBackground(.ultraThinMaterial)
            }
            .alert($store.scope(state: \.alert, action: \.alert))
            .task {
                store.send(.task)
            }
        }
    }

    private var content: some View {
        Group {
            if store.isLoading && store.playlists.isEmpty {
                ProgressView("플레이리스트를 불러오는 중")
            } else {
                List {
                    ForEach(store.playlists) { playlist in
                        PlaylistRow(playlist: playlist)
                            .listRowBackground(Color.clear)
                            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                if store.deletablePlaylistIDs.contains(playlist.id) {
                                    Button("삭제", role: .destructive) {
                                        store.send(.deletePlaylist(playlist.id))
                                    }
                                }
                            }
                    }
                }
                .scrollContentBackground(.hidden)
            }
        }
    }

    private var addPlaylistSheet: some View {
        NavigationStack {
            Form {
                TextField("플레이리스트 이름", text: $store.newPlaylistTitle)
                    .textInputAutocapitalization(.never)
            }
            .navigationTitle("새 플레이리스트")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("취소") {
                        store.send(.addSheetDismissed)
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("추가") {
                        store.send(.addPlaylistConfirmed)
                    }
                    .disabled(store.isAddPlaylistButtonDisabled)
                }
            }
        }
    }
}

private struct PlaylistRow: View {
    let playlist: PlaylistItem

    var body: some View {
        GlassCard {
            HStack(spacing: 14) {
                Image(systemName: "music.note.list")
                    .font(.title2)
                    .foregroundStyle(.purple)
                    .frame(width: 44, height: 44)
                    .background(.ultraThinMaterial, in: Circle())

                VStack(alignment: .leading, spacing: 4) {
                    Text(playlist.title)
                        .font(.headline)
                    Text("\(playlist.audioMediaCount) tracks")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
            }
        }
    }
}

#Preview {
    PlaylistScreenView(
        store: Store(
            initialState: PlaylistFeature.State(
                playlists: [
                    PlaylistItem(id: 1, title: "전체", description: "All tracks", audioMediaCount: 12),
                    PlaylistItem(id: 2, title: "분류 없음", description: "Uncategorized tracks", audioMediaCount: 3),
                ],
                isLoading: false
            )
        ) {
            EmptyReducer()
        }
    )
}
