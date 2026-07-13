import SwiftUI

struct CustomBottomBar: View {
    @Binding var selection: MainTab

    var body: some View {
        HStack(spacing: 0) {
            tabButton(tab: .youtubeSearch, icon: "magnifyingglass", title: "음원추가")
            tabButton(tab: .playlist, icon: "play.square.stack", title: "플레이리스트")
        }
        .padding(.vertical, 10)
        .padding(.horizontal, 8)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .stroke(.primary.opacity(0.12), lineWidth: 1)
        }
    }

    @ViewBuilder
    private func tabButton(tab: MainTab, icon: String, title: String) -> some View {
        let isSelected = selection == tab
        Button {
            withAnimation(.easeInOut(duration: 0.15)) {
                selection = tab
            }
        } label: {
            VStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 20, weight: .semibold))
                Text(title)
                    .font(.caption2)
                    .fontWeight(isSelected ? .semibold : .regular)
            }
            .foregroundStyle(isSelected ? .primary : .secondary)
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

#Preview("Playlist selected") {
    struct PreviewHost: View {
        @State private var selection: MainTab = .playlist
        var body: some View {
            ZStack {
                GlassBackground()
                CustomBottomBar(selection: $selection)
                    .padding(.horizontal, 12)
            }
        }
    }
    return PreviewHost()
}

#Preview("Search selected") {
    struct PreviewHost: View {
        @State private var selection: MainTab = .youtubeSearch
        var body: some View {
            ZStack {
                GlassBackground()
                CustomBottomBar(selection: $selection)
                    .padding(.horizontal, 12)
            }
        }
    }
    return PreviewHost()
}
