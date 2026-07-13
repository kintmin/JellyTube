import Foundation

struct MediaControllerItem: Identifiable, Equatable, Sendable {
    let id: Int
    let title: String
    let artist: String
    let fileURL: URL
    let artworkURL: URL?
    let durationSeconds: Int?
}

struct MediaControllerSnapshot: Equatable, Sendable {
    let currentPlaylistId: Int?
    let currentMediaId: Int?
    let title: String
    let artist: String
    let artworkURL: URL?
    let isPlaying: Bool
    let positionSeconds: Double
    let durationSeconds: Double
    let playbackSpeed: Float
    let playbackPitchSemitone: Int

    static let empty = MediaControllerSnapshot(
        currentPlaylistId: nil,
        currentMediaId: nil,
        title: "",
        artist: "",
        artworkURL: nil,
        isPlaying: false,
        positionSeconds: 0,
        durationSeconds: 0,
        playbackSpeed: 1.0,
        playbackPitchSemitone: 0
    )
}
