import SwiftUI

struct PlaylistCard: View {
    let item: PlaylistItem
    let onDelete: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            cover
                .aspectRatio(1, contentMode: .fit)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            HStack(alignment: .top, spacing: 4) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.title)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundStyle(.primary)
                        .lineLimit(1)
                    Text(subtitle)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
                Spacer(minLength: 0)
                if let onDelete {
                    Menu {
                        Button("삭제", role: .destructive, action: onDelete)
                    } label: {
                        Image(systemName: "ellipsis")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(.secondary)
                            .frame(width: 28, height: 28)
                            .contentShape(Rectangle())
                    }
                }
            }
        }
        .padding(10)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(.primary.opacity(0.1), lineWidth: 1)
        }
    }

    private var subtitle: String {
        "\(item.audioMediaCount)곡 · \(Self.formatDuration(seconds: item.totalDurationSeconds))"
    }

    @ViewBuilder
    private var cover: some View {
        if let url = item.coverImageURL {
            AsyncImage(url: url) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                coverPlaceholder
            }
        } else {
            coverPlaceholder
        }
    }

    private var coverPlaceholder: some View {
        RoundedRectangle(cornerRadius: 12, style: .continuous)
            .fill(.ultraThinMaterial)
            .overlay {
                Image(systemName: "music.note.list")
                    .font(.system(size: 32))
                    .foregroundStyle(.secondary)
            }
    }

    static func formatDuration(seconds: Int) -> String {
        let s = max(0, seconds)
        let h = s / 3600
        let m = (s % 3600) / 60
        let sec = s % 60
        if h > 0 {
            return String(format: "%d:%02d:%02d", h, m, sec)
        }
        return String(format: "%d:%02d", m, sec)
    }
}

#Preview {
    ZStack {
        GlassBackground()
        LazyVGrid(
            columns: [.init(.flexible(), spacing: 12), .init(.flexible(), spacing: 12)],
            spacing: 12
        ) {
            PlaylistCard(
                item: PlaylistItem(
                    id: 10,
                    title: "좋아요",
                    description: "",
                    audioMediaCount: 10,
                    coverImageURL: nil,
                    totalDurationSeconds: 4210
                ),
                onDelete: nil
            )
            PlaylistCard(
                item: PlaylistItem(
                    id: 11,
                    title: "새 플레이리스트",
                    description: "",
                    audioMediaCount: 20,
                    coverImageURL: nil,
                    totalDurationSeconds: 23630
                ),
                onDelete: {}
            )
            PlaylistCard(
                item: PlaylistItem(
                    id: 12,
                    title: "해외 록 아주 긴 제목의 플레이리스트",
                    description: "",
                    audioMediaCount: 20,
                    coverImageURL: nil,
                    totalDurationSeconds: 22810
                ),
                onDelete: {}
            )
        }
        .padding()
    }
}
