import SwiftUI

struct PlaylistDetailRow: View {
    let item: PlaylistDetailItem
    let isNowPlaying: Bool

    var body: some View {
        HStack(spacing: 12) {
            cover
                .frame(width: 44, height: 44)
                .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text(item.title)
                    .font(.body)
                    .fontWeight(.medium)
                    .foregroundStyle(isNowPlaying ? Color.blue : Color.primary)
                    .lineLimit(1)
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }

            Spacer(minLength: 8)

            if isNowPlaying {
                Image(systemName: "waveform")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(Color.blue)
            }
        }
        .padding(.vertical, 10)
        .contentShape(Rectangle())
    }

    private var subtitle: String {
        "\(item.artist) · \(Self.formatDuration(seconds: item.durationSeconds))"
    }

    private var cover: some View {
        Group {
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
    }

    private var coverPlaceholder: some View {
        RoundedRectangle(cornerRadius: 6, style: .continuous)
            .fill(.ultraThinMaterial)
            .overlay {
                Image(systemName: "music.note")
                    .foregroundStyle(.secondary)
            }
    }

    static func formatDuration(seconds: Int?) -> String {
        guard let seconds, seconds >= 0 else { return "--:--" }
        let h = seconds / 3600
        let m = (seconds % 3600) / 60
        let s = seconds % 60
        if h > 0 {
            return String(format: "%d:%02d:%02d", h, m, s)
        }
        return String(format: "%d:%02d", m, s)
    }
}

#Preview("Row states") {
    ZStack {
        GlassBackground()
        VStack(spacing: 0) {
            PlaylistDetailRow(
                item: PlaylistDetailItem(
                    id: 1, title: "추억속의 그대", artist: "dosii",
                    durationSeconds: 233, coverImageURL: nil
                ),
                isNowPlaying: false
            )
            PlaylistDetailRow(
                item: PlaylistDetailItem(
                    id: 5, title: "Yunomix vol.4", artist: "Yunomi",
                    durationSeconds: 233, coverImageURL: nil
                ),
                isNowPlaying: true
            )
            PlaylistDetailRow(
                item: PlaylistDetailItem(
                    id: 6, title: "아주 긴 곡 제목이 한 줄에 안 들어가는 예시입니다",
                    artist: "긴 아티스트 이름", durationSeconds: nil, coverImageURL: nil
                ),
                isNowPlaying: false
            )
        }
        .padding(.horizontal, 16)
    }
}
