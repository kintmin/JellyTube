import MediaPlayer
import UIKit
import os

private let npLog = Logger(subsystem: "com.kintmin.JellyTubeIos", category: "NowPlaying")

@MainActor
final class NowPlayingCoordinator {
    var onPlay: (() -> Void)?
    var onPause: (() -> Void)?
    var onTogglePlayPause: (() -> Void)?
    var onNext: (() -> Void)?
    var onPrevious: (() -> Void)?
    var onSeek: ((Double) -> Void)?

    private var didConfigureCommands = false
    private var cachedArtworkURL: URL?
    private var cachedArtwork: MPMediaItemArtwork?

    func configureCommandsIfNeeded() {
        guard !didConfigureCommands else { return }
        didConfigureCommands = true

        let center = MPRemoteCommandCenter.shared()

        center.playCommand.isEnabled = true
        center.playCommand.addTarget { [weak self] _ in
            npLog.info("remote: play")
            self?.onPlay?()
            return .success
        }
        center.pauseCommand.isEnabled = true
        center.pauseCommand.addTarget { [weak self] _ in
            npLog.info("remote: pause")
            self?.onPause?()
            return .success
        }
        center.togglePlayPauseCommand.isEnabled = true
        center.togglePlayPauseCommand.addTarget { [weak self] _ in
            npLog.info("remote: toggle")
            self?.onTogglePlayPause?()
            return .success
        }
        center.nextTrackCommand.isEnabled = true
        center.nextTrackCommand.addTarget { [weak self] _ in
            npLog.info("remote: next")
            self?.onNext?()
            return .success
        }
        center.previousTrackCommand.isEnabled = true
        center.previousTrackCommand.addTarget { [weak self] _ in
            npLog.info("remote: previous")
            self?.onPrevious?()
            return .success
        }
        center.changePlaybackPositionCommand.isEnabled = true
        center.changePlaybackPositionCommand.addTarget { [weak self] event in
            guard let event = event as? MPChangePlaybackPositionCommandEvent else {
                return .commandFailed
            }
            npLog.info("remote: seek \(event.positionTime)")
            self?.onSeek?(event.positionTime)
            return .success
        }

        npLog.info("remote commands registered")
    }

    func update(snapshot: MediaControllerSnapshot) {
        guard snapshot.currentMediaId != nil else {
            clear()
            return
        }

        var info: [String: Any] = [
            MPMediaItemPropertyTitle: snapshot.title,
            MPMediaItemPropertyArtist: snapshot.artist,
            MPNowPlayingInfoPropertyElapsedPlaybackTime: snapshot.positionSeconds,
            MPMediaItemPropertyPlaybackDuration: snapshot.durationSeconds,
            MPNowPlayingInfoPropertyPlaybackRate: snapshot.isPlaying ? 1.0 : 0.0,
            MPNowPlayingInfoPropertyMediaType: MPNowPlayingInfoMediaType.audio.rawValue,
        ]

        if let artwork = resolveArtwork(url: snapshot.artworkURL) {
            info[MPMediaItemPropertyArtwork] = artwork
        }

        MPNowPlayingInfoCenter.default().nowPlayingInfo = info

        if !didLogFirstUpdate {
            didLogFirstUpdate = true
            npLog.info("nowPlaying set title=\(snapshot.title, privacy: .public) artist=\(snapshot.artist, privacy: .public) duration=\(snapshot.durationSeconds) rate=\(snapshot.isPlaying ? 1.0 : 0.0)")
        }
    }

    private var didLogFirstUpdate = false

    func clear() {
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
        cachedArtworkURL = nil
        cachedArtwork = nil
    }

    // 앨범아트는 매 브로드캐스트(0.5s)마다 요청되므로 URL이 바뀌지 않으면 재사용한다.
    private func resolveArtwork(url: URL?) -> MPMediaItemArtwork? {
        guard let url else {
            cachedArtworkURL = nil
            cachedArtwork = nil
            return nil
        }
        if cachedArtworkURL == url, let cached = cachedArtwork {
            return cached
        }
        guard let image = UIImage(contentsOfFile: url.path) else {
            cachedArtworkURL = nil
            cachedArtwork = nil
            return nil
        }
        let artwork = MPMediaItemArtwork(boundsSize: image.size) { _ in image }
        cachedArtworkURL = url
        cachedArtwork = artwork
        return artwork
    }
}
