import SwiftUI

struct PlaylistTopSegment: View {
    @Binding var selection: PlaylistFeature.PlaylistTopTab
    @Namespace private var underlineNamespace

    var body: some View {
        HStack(spacing: 0) {
            ForEach(PlaylistFeature.PlaylistTopTab.allCases, id: \.self) { tab in
                let isSelected = selection == tab
                VStack(spacing: 8) {
                    Text(tab.title)
                        .font(.subheadline)
                        .fontWeight(isSelected ? .semibold : .regular)
                        .foregroundStyle(isSelected ? .primary : .secondary)
                    ZStack {
                        Rectangle()
                            .fill(.clear)
                            .frame(height: 2)
                        if isSelected {
                            Rectangle()
                                .fill(Color.blue)
                                .frame(height: 2)
                                .matchedGeometryEffect(id: "underline", in: underlineNamespace)
                        }
                    }
                }
                .frame(maxWidth: .infinity)
                .contentShape(Rectangle())
                .onTapGesture {
                    withAnimation(.snappy(duration: 0.2)) {
                        selection = tab
                    }
                }
            }
        }
    }
}

extension PlaylistFeature.PlaylistTopTab {
    var title: String {
        switch self {
        case .myPlaylists: return "내 플레이리스트"
        case .all: return "전체"
        case .uncategorized: return "미분류"
        }
    }
}

#Preview("My Playlists") {
    struct PreviewHost: View {
        @State private var selection: PlaylistFeature.PlaylistTopTab = .myPlaylists
        var body: some View {
            ZStack {
                GlassBackground()
                PlaylistTopSegment(selection: $selection)
                    .padding()
            }
        }
    }
    return PreviewHost()
}

#Preview("All") {
    struct PreviewHost: View {
        @State private var selection: PlaylistFeature.PlaylistTopTab = .all
        var body: some View {
            ZStack {
                GlassBackground()
                PlaylistTopSegment(selection: $selection)
                    .padding()
            }
        }
    }
    return PreviewHost()
}
