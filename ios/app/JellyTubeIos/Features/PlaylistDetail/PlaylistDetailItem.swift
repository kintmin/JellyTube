import Foundation

struct PlaylistDetailItem: Identifiable, Equatable, Sendable {
    let id: Int
    let title: String
    let artist: String
    let durationSeconds: Int?
    let coverImageURL: URL?
    let audioFileURL: URL
}
