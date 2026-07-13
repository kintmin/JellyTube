import AVFoundation
import Foundation
import os

private let mcLog = Logger(subsystem: "com.kintmin.JellyTubeIos", category: "MediaController")

@MainActor
final class MediaControllerEngine {
    private let engine = AVAudioEngine()
    private let playerNode = AVAudioPlayerNode()
    private let pitchUnit = AVAudioUnitTimePitch()
    private let nowPlaying = NowPlayingCoordinator()

    private var queue: [MediaControllerItem] = []
    private var currentPlaylistId: Int?
    private var currentIndex: Int = 0
    private var currentFile: AVAudioFile?
    private var currentItemBaseOffset: TimeInterval = 0
    private var isPlayingIntent = false
    private var didConfigureAudioSession = false
    private var didStartEngine = false

    private var currentSpeed: Float = 1.0
    private var currentPitchSemitone: Int = 0

    private var broadcastTimer: DispatchSourceTimer?
    private var continuations: [UUID: AsyncStream<MediaControllerSnapshot>.Continuation] = [:]

    private var interruptionObserver: NSObjectProtocol?
    private var routeChangeObserver: NSObjectProtocol?

    init() {
        engine.attach(playerNode)
        engine.attach(pitchUnit)
        engine.connect(playerNode, to: pitchUnit, format: nil)
        engine.connect(pitchUnit, to: engine.mainMixerNode, format: nil)

        pitchUnit.rate = currentSpeed
        pitchUnit.pitch = Float(currentPitchSemitone * 100)

        wireRemoteCommands()
        subscribeSessionNotifications()
        startBroadcastTimer()
    }

    // MARK: - Public API

    func play(playlistId: Int, startMediaId: Int?, items: [MediaControllerItem]) {
        configureAudioSessionIfNeeded()
        ensureEngineRunning()

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

        if !sameList {
            currentPlaylistId = playlistId
            queue = items
        }

        scheduleItem(at: targetIndex, startingSeconds: 0)
        playerNode.play()
        isPlayingIntent = true
        broadcast()
    }

    func pause() {
        guard playerNode.isPlaying else {
            isPlayingIntent = false
            broadcast()
            return
        }
        playerNode.pause()
        isPlayingIntent = false
        broadcast()
    }

    func resume() {
        ensureEngineRunning()
        if !playerNode.isPlaying {
            playerNode.play()
        }
        isPlayingIntent = true
        broadcast()
    }

    func next() {
        guard currentIndex + 1 < queue.count else { return }
        scheduleItem(at: currentIndex + 1, startingSeconds: 0)
        if isPlayingIntent {
            playerNode.play()
        }
        broadcast()
    }

    func previous() {
        let position = currentTimeInItem
        if currentIndex > 0, position < 3 {
            scheduleItem(at: currentIndex - 1, startingSeconds: 0)
            if isPlayingIntent {
                playerNode.play()
            }
        } else {
            scheduleItem(at: currentIndex, startingSeconds: 0)
            if isPlayingIntent {
                playerNode.play()
            }
        }
        broadcast()
    }

    func seek(seconds: Double) {
        guard currentFile != nil else { return }
        scheduleItem(at: currentIndex, startingSeconds: max(0, seconds))
        if isPlayingIntent {
            playerNode.play()
        }
        broadcast()
    }

    func setSpeed(_ speed: Float) {
        let clamped = min(max(speed, 0.25), 4.0)
        currentSpeed = clamped
        pitchUnit.rate = clamped
        mcLog.info("applied speed=\(clamped)")
        broadcast()
    }

    func setPitchSemitone(_ semitone: Int) {
        let clamped = min(max(semitone, -24), 24)
        currentPitchSemitone = clamped
        pitchUnit.pitch = Float(clamped * 100)
        mcLog.info("applied pitchSemitone=\(clamped)")
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

    // MARK: - Scheduling

    private func scheduleItem(at index: Int, startingSeconds: TimeInterval) {
        guard index >= 0, index < queue.count else { return }

        playerNode.stop()

        let item = queue[index]
        let file: AVAudioFile
        do {
            file = try AVAudioFile(forReading: item.fileURL)
        } catch {
            mcLog.error("failed to open file: \(error.localizedDescription, privacy: .public)")
            currentFile = nil
            currentIndex = index
            currentItemBaseOffset = 0
            return
        }

        currentFile = file
        currentIndex = index
        currentItemBaseOffset = startingSeconds

        let sampleRate = file.processingFormat.sampleRate
        let startFrame = AVAudioFramePosition(startingSeconds * sampleRate)
        let totalFrames = file.length
        guard startFrame < totalFrames else {
            advanceOnCompletion()
            return
        }
        let remaining = AVAudioFrameCount(totalFrames - startFrame)

        playerNode.scheduleSegment(
            file,
            startingFrame: startFrame,
            frameCount: remaining,
            at: nil
        ) { [weak self] in
            Task { @MainActor in self?.advanceOnCompletion() }
        }
    }

    private func advanceOnCompletion() {
        guard isPlayingIntent else { return }
        guard currentIndex + 1 < queue.count else {
            isPlayingIntent = false
            playerNode.stop()
            broadcast()
            return
        }
        scheduleItem(at: currentIndex + 1, startingSeconds: 0)
        playerNode.play()
        broadcast()
    }

    // MARK: - Engine / session

    private func ensureEngineRunning() {
        if !engine.isRunning {
            do {
                try engine.start()
                didStartEngine = true
            } catch {
                mcLog.error("engine.start failed: \(error.localizedDescription, privacy: .public)")
            }
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
        nowPlaying.configureCommandsIfNeeded()
    }

    private func subscribeSessionNotifications() {
        interruptionObserver = NotificationCenter.default.addObserver(
            forName: AVAudioSession.interruptionNotification,
            object: nil,
            queue: .main
        ) { [weak self] note in
            Task { @MainActor in self?.handleInterruption(note) }
        }
        routeChangeObserver = NotificationCenter.default.addObserver(
            forName: AVAudioSession.routeChangeNotification,
            object: nil,
            queue: .main
        ) { [weak self] note in
            Task { @MainActor in self?.handleRouteChange(note) }
        }
    }

    private func handleInterruption(_ note: Notification) {
        guard let userInfo = note.userInfo,
              let typeValue = userInfo[AVAudioSessionInterruptionTypeKey] as? UInt,
              let type = AVAudioSession.InterruptionType(rawValue: typeValue) else { return }
        switch type {
        case .began:
            mcLog.info("interruption began")
            pause()
        case .ended:
            let options = (userInfo[AVAudioSessionInterruptionOptionKey] as? UInt).map {
                AVAudioSession.InterruptionOptions(rawValue: $0)
            }
            if options?.contains(.shouldResume) == true {
                mcLog.info("interruption ended: resuming")
                resume()
            } else {
                mcLog.info("interruption ended: not resuming")
            }
        @unknown default:
            break
        }
    }

    private func handleRouteChange(_ note: Notification) {
        guard let userInfo = note.userInfo,
              let reasonValue = userInfo[AVAudioSessionRouteChangeReasonKey] as? UInt,
              let reason = AVAudioSession.RouteChangeReason(rawValue: reasonValue) else { return }
        if reason == .oldDeviceUnavailable {
            mcLog.info("route: old device unavailable → pause")
            pause()
        }
    }

    // MARK: - Remote commands

    private func wireRemoteCommands() {
        nowPlaying.onPlay = { [weak self] in self?.resume() }
        nowPlaying.onPause = { [weak self] in self?.pause() }
        nowPlaying.onTogglePlayPause = { [weak self] in
            guard let self else { return }
            if self.playerNode.isPlaying {
                self.pause()
            } else {
                self.resume()
            }
        }
        nowPlaying.onNext = { [weak self] in self?.next() }
        nowPlaying.onPrevious = { [weak self] in self?.previous() }
        nowPlaying.onSeek = { [weak self] seconds in self?.seek(seconds: seconds) }
    }

    // MARK: - Time tracking

    private var currentTimeInItem: TimeInterval {
        guard let file = currentFile else { return currentItemBaseOffset }
        guard let nodeTime = playerNode.lastRenderTime,
              let playerTime = playerNode.playerTime(forNodeTime: nodeTime),
              playerTime.sampleRate > 0 else {
            return currentItemBaseOffset
        }
        let elapsedInSegment = Double(playerTime.sampleTime) / playerTime.sampleRate
        let total = currentItemBaseOffset + max(0, elapsedInSegment)
        let duration = Double(file.length) / file.processingFormat.sampleRate
        return min(total, duration)
    }

    // MARK: - Broadcast

    private func startBroadcastTimer() {
        let timer = DispatchSource.makeTimerSource(queue: .main)
        timer.schedule(deadline: .now() + 0.5, repeating: 0.5)
        timer.setEventHandler { [weak self] in
            self?.broadcast()
        }
        timer.resume()
        broadcastTimer = timer
    }

    private func broadcast() {
        let snapshot = currentSnapshot()
        nowPlaying.update(snapshot: snapshot)
        for continuation in continuations.values {
            continuation.yield(snapshot)
        }
    }

    private func currentSnapshot() -> MediaControllerSnapshot {
        let item: MediaControllerItem? = (0..<queue.count).contains(currentIndex) ? queue[currentIndex] : nil
        let isPlaying = playerNode.isPlaying && isPlayingIntent
        let position = currentTimeInItem
        let duration: Double = {
            if let file = currentFile, file.processingFormat.sampleRate > 0 {
                return Double(file.length) / file.processingFormat.sampleRate
            }
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
            durationSeconds: duration,
            playbackSpeed: currentSpeed,
            playbackPitchSemitone: currentPitchSemitone
        )
    }
}
