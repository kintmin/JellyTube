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
            .toolbar(.hidden, for: .navigationBar)
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
        VStack(spacing: 12) {
            header
                .padding(.horizontal, 16)
                .padding(.top, 8)
            PlaylistTopSegment(
                selection: Binding(
                    get: { store.selectedTopTab },
                    set: { store.send(.topTabSelected($0)) }
                )
            )
            .padding(.horizontal, 16)
            grid
        }
    }

    private var header: some View {
        HStack(alignment: .center) {
            Text("플레이리스트")
                .font(.largeTitle)
                .fontWeight(.bold)
            Spacer()
            Button {
                store.send(.settingsTapped)
            } label: {
                Image(systemName: "gearshape")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(.primary)
                    .frame(width: 36, height: 36)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
    }

    @ViewBuilder
    private var grid: some View {
        if store.isLoading && store.playlists.isEmpty {
            Spacer()
            ProgressView("플레이리스트를 불러오는 중")
            Spacer()
        } else {
            ScrollView {
                LazyVGrid(
                    columns: [
                        GridItem(.flexible(), spacing: 12),
                        GridItem(.flexible(), spacing: 12)
                    ],
                    spacing: 12
                ) {
                    ForEach(store.displayedPlaylists) { item in
                        let canDelete = store.deletablePlaylistIDs.contains(item.id)
                        PlaylistCard(
                            item: item,
                            onDelete: canDelete
                                ? { store.send(.deletePlaylist(item.id)) }
                                : nil
                        )
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 4)
                .padding(.bottom, 12)
            }
            .contentMargins(.bottom, 160, for: .scrollContent)
            .scrollDismissesKeyboard(.interactively)
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

#Preview("My playlists with data") {
    PlaylistScreenView(
        store: Store(
            initialState: PlaylistFeature.State(
                playlists: [
                    PlaylistItem(
                        id: 1, title: "전체", description: "", audioMediaCount: 12,
                        coverImageURL: nil, totalDurationSeconds: 3600
                    ),
                    PlaylistItem(
                        id: 3, title: "좋아요", description: "", audioMediaCount: 10,
                        coverImageURL: nil, totalDurationSeconds: 4210
                    ),
                    PlaylistItem(
                        id: 4, title: "새 플레이리스트", description: "", audioMediaCount: 20,
                        coverImageURL: nil, totalDurationSeconds: 23630
                    ),
                    PlaylistItem(
                        id: 5, title: "해외 록", description: "", audioMediaCount: 20,
                        coverImageURL: nil, totalDurationSeconds: 22810
                    ),
                ],
                selectedTopTab: .myPlaylists,
                isLoading: false
            )
        ) {
            EmptyReducer()
        }
    )
}

#Preview("All tab (empty placeholder)") {
    PlaylistScreenView(
        store: Store(
            initialState: PlaylistFeature.State(
                playlists: [],
                selectedTopTab: .all,
                isLoading: false
            )
        ) {
            EmptyReducer()
        }
    )
}
