import ComposableArchitecture
import SwiftUI

struct PlayerBarScreenView: View {
    @Bindable var store: StoreOf<PlayerBarFeature>

    var body: some View {
        VStack(spacing: 0) {
            progressLine
            HStack(spacing: 12) {
                thumbnail
                VStack(alignment: .leading, spacing: 4) {
                    Text(store.title)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .lineLimit(1)
                    Text("\(store.currentDurationText) / \(store.totalDurationText)")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer(minLength: 8)
                Button {
                    store.send(.playPauseTapped)
                } label: {
                    Image(systemName: store.isPlaying ? "pause.fill" : "play.fill")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundStyle(.primary)
                        .frame(width: 40, height: 40)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        }
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(.primary.opacity(0.12), lineWidth: 1)
        }
    }

    private var progressLine: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Rectangle()
                    .fill(.primary.opacity(0.08))
                Rectangle()
                    .fill(Color.blue)
                    .frame(width: geo.size.width * store.progress)
            }
        }
        .frame(height: 3)
        .clipShape(
            UnevenRoundedRectangle(
                topLeadingRadius: 18,
                bottomLeadingRadius: 0,
                bottomTrailingRadius: 0,
                topTrailingRadius: 18,
                style: .continuous
            )
        )
    }

    @ViewBuilder
    private var thumbnail: some View {
        Group {
            if let url = store.thumbnailURL {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    thumbnailPlaceholder
                }
            } else {
                thumbnailPlaceholder
            }
        }
        .frame(width: 44, height: 44)
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
    }

    private var thumbnailPlaceholder: some View {
        RoundedRectangle(cornerRadius: 8, style: .continuous)
            .fill(.ultraThinMaterial)
            .overlay {
                Image(systemName: "music.note")
                    .foregroundStyle(.secondary)
            }
    }
}

#Preview("Paused") {
    ZStack {
        GlassBackground()
        PlayerBarScreenView(
            store: Store(initialState: PlayerBarFeature.State()) {
                PlayerBarFeature()
            }
        )
        .padding(.horizontal, 12)
    }
}

#Preview("Playing") {
    ZStack {
        GlassBackground()
        PlayerBarScreenView(
            store: Store(
                initialState: PlayerBarFeature.State(
                    title: "긴 곡 제목이 한 줄에 안 들어가는 경우 예시",
                    currentSeconds: 45,
                    totalSeconds: 210,
                    isPlaying: true
                )
            ) {
                PlayerBarFeature()
            }
        )
        .padding(.horizontal, 12)
    }
}
