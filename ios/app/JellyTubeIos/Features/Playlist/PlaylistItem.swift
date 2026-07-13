import Foundation

struct PlaylistItem: Identifiable, Equatable, Sendable {
    let id: Int
    let title: String
    let description: String
    let audioMediaCount: Int
    let coverImageURL: URL?
    let totalDurationSeconds: Int
}
