import SwiftUI

struct ContentView: View {
    var body: some View {
        VStack(spacing: 12) {
            Text("JellyTube iOS")
                .font(.title)
                .fontWeight(.semibold)
            Text("Build-only iOS app")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .padding()
    }
}
