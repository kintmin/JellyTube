import AVFoundation
import Foundation
import os

private let mcLog = Logger(subsystem: "com.kintmin.JellyTubeIos", category: "MediaController")

@MainActor
final class MediaControllerEngine {
    private let player = AVQueuePlayer()
    private var queue: [MediaControllerItem] = []
    private var currentPlaylistId: Int?
    private var didConfigureAudioSession = false

    private var timeObserverToken: Any?
    private var currentItemObservation: NSKeyValueObservation?
    private var timeControlStatusObservation: NSKeyValueObservation?
    private var itemStatusObservation: NSKeyValueObservation?
    private var playerErrorObservation: NSKeyValueObservation?

    private var continuations: [UUID: AsyncStream<MediaControllerSnapshot>.Continuation] = [:]

    init() {
        player.actionAtItemEnd = .advance

        currentItemObservation = player.observe(\.currentItem, options: [.new]) { [weak self] _, change in
            guard let self else { return }
            Task { @MainActor in
                self.observeCurrentItem(change.newValue ?? nil)
                self.broadcast()
            }
        }
        timeControlStatusObservation = player.observe(\.timeControlStatus, options: [.new]) { [weak self] player, _ in
            guard let self else { return }
            let status = player.timeControlStatus
            Task { @MainActor in
                mcLog.info("timeControlStatus=\(status.debugName, privacy: .public) rate=\(player.rate) reason=\(player.reasonForWaitingToPlay?.rawValue ?? "nil", privacy: .public)")
                self.broadcast()
            }
        }
        playerErrorObservation = player.observe(\.error, options: [.new]) { [weak self] player, _ in
            guard let self, let error = player.error else { return }
            Task { @MainActor in
                mcLog.error("player.error: \(error.localizedDescription, privacy: .public)")
                self.broadcast()
            }
        }
        timeObserverToken = player.addPeriodicTimeObserver(
            forInterval: CMTime(seconds: 0.5, preferredTimescale: 600),
            queue: .main
        ) { [weak self] _ in
            guard let self else { return }
            Task { @MainActor in self.broadcast() }
        }
    }

    private func observeCurrentItem(_ item: AVPlayerItem?) {
        itemStatusObservation?.invalidate()
        itemStatusObservation = nil
        guard let item else { return }
        itemStatusObservation = item.observe(\.status, options: [.new]) { obsItem, _ in
            Task { @MainActor in
                switch obsItem.status {
                case .unknown:
                    mcLog.info("item.status=unknown")
                case .readyToPlay:
                    mcLog.info("item.status=readyToPlay duration=\(obsItem.duration.seconds)")
                case .failed:
                    let err = obsItem.error?.localizedDescription ?? "nil"
                    mcLog.error("item.status=failed error=\(err, privacy: .public)")
                @unknown default:
                    break
                }
            }
        }
    }

    private var currentIndex: Int {
        max(0, queue.count - player.items().count)
    }

    func play(playlistId: Int, startMediaId: Int?, items: [MediaControllerItem]) {
        configureAudioSessionIfNeeded()

        let targetIndex = startMediaId
            .flatMap { id in items.firstIndex(where: { $0.id == id }) }
            ?? 0
        let sameList = currentPlaylistId == playlistId && queue.map(\.id) == items.map(\.id)

        mcLog.info("play playlistId=\(playlistId) startMediaId=\(startMediaId ?? -1) items=\(items.count) targetIndex=\(targetIndex) sameList=\(sameList)")

        if targetIndex < items.count {
            let url = items[targetIndex].fileURL
            let exists = FileManager.default.fileExists(atPath: url.path)
            mcLog.info("startItem url=\(url.path, privacy: .public) exists=\(exists)")
        }

        if !sameList || currentIndex != targetIndex {
            currentPlaylistId = playlistId
            queue = items
            rebuildPlayerQueue(startingAt: targetIndex)
        }

        player.play()
        broadcast()
    }

    func pause() {
        player.pause()
        broadcast()
    }

    func resume() {
        player.play()
        broadcast()
    }

    func next() {
        guard player.items().count > 1 else { return }
        player.advanceToNextItem()
    }

    func previous() {
        let idx = currentIndex
        let position = player.currentTime().seconds
        if idx > 0, position.isFinite, position < 3 {
            rebuildPlayerQueue(startingAt: idx - 1)
            player.play()
        } else {
            player.seek(to: .zero)
        }
        broadcast()
    }

    func seek(seconds: Double) {
        player.seek(to: CMTime(seconds: seconds, preferredTimescale: 600))
        broadcast()
    }

    func snapshotStream() -> AsyncStream<MediaControllerSnapshot> {
        AsyncStream { continuation in
            let id = UUID()
            self.continuations[id] = continuation
            continuation.yield(self.currentSnapshot())
            continuation.onTermination = { [weak self] _ in
                Task { @MainActor [weak self] in
                    self?.continuations.removeValue(forKey: id)
                }
            }
        }
    }

    private func rebuildPlayerQueue(startingAt startIndex: Int) {
        player.removeAllItems()
        guard startIndex >= 0, startIndex < queue.count else { return }
        for i in startIndex..<queue.count {
            player.insert(AVPlayerItem(url: queue[i].fileURL), after: nil)
        }
    }

    private func configureAudioSessionIfNeeded() {
        guard !didConfigureAudioSession else { return }
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(.playback, mode: .default)
            try session.setActive(true)
            didConfigureAudioSession = true
            mcLog.info("audioSession activated category=\(session.category.rawValue, privacy: .public)")
        } catch {
            mcLog.error("audioSession setup failed: \(error.localizedDescription, privacy: .public)")
        }
    }

    private func broadcast() {
        let snapshot = currentSnapshot()
        for continuation in continuations.values {
            continuation.yield(snapshot)
        }
    }

    private func currentSnapshot() -> MediaControllerSnapshot {
        let idx = currentIndex
        let item: MediaControllerItem? = (0..<queue.count).contains(idx) ? queue[idx] : nil
        let isPlaying = player.timeControlStatus == .playing
        let position: Double = {
            let t = player.currentTime().seconds
            return t.isFinite ? t : 0
        }()
        let duration: Double = {
            if let d = player.currentItem?.duration.seconds, d.isFinite, d > 0 { return d }
            return Double(item?.durationSeconds ?? 0)
        }()
        return MediaControllerSnapshot(
            currentPlaylistId: currentPlaylistId,
            currentMediaId: item?.id,
            title: item?.title ?? "",
            artist: item?.artist ?? "",
            artworkURL: item?.artworkURL,
            isPlaying: isPlaying,
            positionSeconds: position,
            durationSeconds: duration
        )
    }
}

private extension AVPlayer.TimeControlStatus {
    var debugName: String {
        switch self {
        case .paused: return "paused"
        case .waitingToPlayAtSpecifiedRate: return "waiting"
        case .playing: return "playing"
        @unknown default: return "unknown"
        }
    }
}
