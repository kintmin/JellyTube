import AVFoundation
import Foundation
import os

private enum MediaControllerEngineError: Error {
    case emptyFile
}

private enum ScheduleResult {
    case scheduled
    /// 파일이 없거나 열 수 없거나 비어 있다.
    case unreadable
    /// 곡 길이 이상으로 탐색했다.
    case pastEnd
}

private let mcLog = Logger(subsystem: "com.kintmin.JellyTubeIos", category: "MediaController")

@MainActor
final class MediaControllerEngine {
    private let engine = AVAudioEngine()
    private let playerNode = AVAudioPlayerNode()
    private let pitchUnit = AVAudioUnitTimePitch()
    private let nowPlaying = NowPlayingCoordinator()

    private var queue: [MediaControllerItem] = []
    /// `play(...)` 가 받은 원래 순서. 셔플을 끌 때 되돌릴 기준이다.
    private var originalQueue: [MediaControllerItem] = []
    private var isShuffleEnabled = false
    private var isRepeatAllEnabled = false
    private var currentPlaylistId: Int?
    private var currentIndex: Int = 0
    private var currentFile: AVAudioFile?
    private var currentItemBaseOffset: TimeInterval = 0
    /// 구간을 걸 때마다 올린다. `stop()` 도 걸려 있던 구간의 완료 콜백을 부르므로, 콜백이 걸린 번호와
    /// 지금 번호가 다르면 이미 다른 곡으로 넘어간 뒤에 늦게 온 콜백이다. 그대로 따르면 한 곡을 더 건너뛴다.
    private var scheduleGeneration = 0
    /// 엔진이 멈추면 재생 위치를 읽을 수 없다. 엔진이 돌 때 마지막으로 읽은 위치로 이어서 건다.
    private var lastKnownPosition: TimeInterval = 0
    private var isPlayingIntent = false
    private var didConfigureAudioSession = false
    private var didStartEngine = false

    private var currentSpeed: Float = 1.0
    private var currentPitchSemitone: Int = 0

    private var broadcastTimer: DispatchSourceTimer?
    private var continuations: [UUID: AsyncStream<MediaControllerSnapshot>.Continuation] = [:]

    private var interruptionObserver: NSObjectProtocol?
    private var routeChangeObserver: NSObjectProtocol?
    private var configurationChangeObserver: NSObjectProtocol?

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
        // 셔플 중에는 queue 가 섞여 있으므로 원래 순서와 비교해야 한다.
        let sameList = currentPlaylistId == playlistId && originalQueue.map(\.id) == items.map(\.id)

        mcLog.info("play playlistId=\(playlistId) startMediaId=\(startMediaId ?? -1) items=\(items.count) targetIndex=\(targetIndex) sameList=\(sameList)")

        if targetIndex < items.count {
            let url = items[targetIndex].fileURL
            let exists = FileManager.default.fileExists(atPath: url.path)
            mcLog.info("startItem url=\(url.path, privacy: .public) exists=\(exists)")
        }

        if !sameList {
            currentPlaylistId = playlistId
            originalQueue = items
            queue = isShuffleEnabled ? items.shuffled() : items
        }

        let startId = (targetIndex < items.count) ? items[targetIndex].id : nil
        let scheduleIndex = startId.flatMap { id in queue.firstIndex(where: { $0.id == id }) } ?? 0
        isPlayingIntent = true
        if schedulePlayable(from: scheduleIndex, startingSeconds: 0) {
            playerNode.play()
        }
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
        let hasNext = currentIndex + 1 < queue.count
        guard hasNext || (isRepeatAllEnabled && !queue.isEmpty) else { return }
        playIfIntended(schedulePlayable(from: currentIndex + 1, startingSeconds: 0))
        broadcast()
    }

    func previous() {
        let target = (currentIndex > 0 && currentTimeInItem < 3) ? currentIndex - 1 : currentIndex
        playIfIntended(schedulePlayable(from: target, startingSeconds: 0))
        broadcast()
    }

    func seek(seconds: Double) {
        guard currentFile != nil else { return }
        playIfIntended(schedulePlayable(from: currentIndex, startingSeconds: max(0, seconds)))
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

    /// 지금 듣고 있는 곡은 자리를 지킨 채 나머지만 섞는다. 끄면 원래 순서로 되돌린다.
    func setShuffle(_ isShuffle: Bool) {
        guard isShuffleEnabled != isShuffle else { return }
        isShuffleEnabled = isShuffle

        let playingId = currentItem?.id
        if isShuffle {
            var shuffled = originalQueue.shuffled()
            if let playingId, let index = shuffled.firstIndex(where: { $0.id == playingId }) {
                shuffled.swapAt(0, index)
            }
            queue = shuffled
        } else {
            queue = originalQueue
        }
        currentIndex = playingId.flatMap { id in queue.firstIndex(where: { $0.id == id }) } ?? 0
        broadcast()
    }

    func setRepeatAll(_ isRepeat: Bool) {
        isRepeatAllEnabled = isRepeat
    }

    func addItem(_ item: MediaControllerItem, atStart: Bool) {
        originalQueue = atStart ? [item] + originalQueue : originalQueue + [item]

        if atStart {
            // 듣고 있던 곡이 한 칸 뒤로 밀린다. 큐가 비어 있었으면 밀릴 곡이 없다.
            let hadItems = !queue.isEmpty
            queue.insert(item, at: 0)
            if hadItems { currentIndex += 1 }
        } else {
            queue.append(item)
        }
        broadcast()
    }

    func removeItem(mediaId: Int) {
        originalQueue.removeAll { $0.id == mediaId }
        guard let index = queue.firstIndex(where: { $0.id == mediaId }) else { return }

        let wasCurrent = index == currentIndex
        queue.remove(at: index)

        if queue.isEmpty {
            stopPlayer()
            currentFile = nil
            currentIndex = 0
            isPlayingIntent = false
        } else if wasCurrent {
            // 빠진 자리를 이어받은 곡으로 넘어간다. 마지막 곡이었으면 한 칸 앞으로.
            let nextIndex = min(index, queue.count - 1)
            playIfIntended(schedulePlayable(from: nextIndex, startingSeconds: 0))
        } else if index < currentIndex {
            currentIndex -= 1
        }
        broadcast()
    }

    /// 같은 자리에 다른 파일이 들어온다(음원 편집 결과 등). 듣고 있던 곡이면 처음부터 다시 건다.
    func replaceItem(mediaId: Int, with item: MediaControllerItem) {
        if let index = originalQueue.firstIndex(where: { $0.id == mediaId }) {
            originalQueue[index] = item
        }
        guard let index = queue.firstIndex(where: { $0.id == mediaId }) else { return }
        queue[index] = item

        if index == currentIndex {
            playIfIntended(schedulePlayable(from: index, startingSeconds: 0))
        }
        broadcast()
    }

    var currentItem: MediaControllerItem? {
        (0..<queue.count).contains(currentIndex) ? queue[currentIndex] : nil
    }

    var isPlayingNow: Bool { playerNode.isPlaying && isPlayingIntent }

    var positionMillis: Int64 { Int64(currentTimeInItem * 1000) }

    var durationMillis: Int64 {
        if let file = currentFile, file.processingFormat.sampleRate > 0 {
            return Int64(Double(file.length) / file.processingFormat.sampleRate * 1000)
        }
        return Int64((currentItem?.durationSeconds ?? 0) * 1000)
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

    /// [index] 부터 걸 수 있는 곡을 찾아 건다. 열 수 없는 파일과 끝을 넘긴 탐색은 다음 곡으로 넘긴다.
    /// 걸 곡이 없으면 재생을 멈추고 false 다.
    private func schedulePlayable(from index: Int, startingSeconds: TimeInterval) -> Bool {
        var candidate = index
        var seconds = startingSeconds
        for _ in 0..<queue.count {
            if candidate >= queue.count {
                guard isRepeatAllEnabled else { break }
                candidate = 0
            }
            switch scheduleItem(at: candidate, startingSeconds: seconds) {
            case .scheduled:
                return true
            case .unreadable, .pastEnd:
                candidate += 1
                seconds = 0
            }
        }
        isPlayingIntent = false
        stopPlayer()
        return false
    }

    private func playIfIntended(_ scheduled: Bool) {
        if scheduled, isPlayingIntent { playerNode.play() }
    }

    /// `stop()` 은 걸려 있던 구간의 완료 콜백을 부른다. 세대를 함께 올려야 그 콜백이 다음 곡으로 넘기지 않는다.
    private func stopPlayer() {
        scheduleGeneration += 1
        playerNode.stop()
    }

    private func scheduleItem(at index: Int, startingSeconds: TimeInterval) -> ScheduleResult {
        guard queue.indices.contains(index) else { return .unreadable }
        stopPlayer()
        let generation = scheduleGeneration

        let item = queue[index]
        let file: AVAudioFile
        do {
            file = try AVAudioFile(forReading: item.fileURL)
            guard file.length > 0 else { throw MediaControllerEngineError.emptyFile }
        } catch {
            mcLog.error("failed to open file: \(error.localizedDescription, privacy: .public)")
            currentFile = nil
            currentIndex = index
            currentItemBaseOffset = 0
            return .unreadable
        }

        currentFile = file
        currentIndex = index
        currentItemBaseOffset = startingSeconds

        let sampleRate = file.processingFormat.sampleRate
        let startFrame = AVAudioFramePosition(startingSeconds * sampleRate)
        let totalFrames = file.length
        guard startFrame < totalFrames else { return .pastEnd }
        let remaining = AVAudioFrameCount(totalFrames - startFrame)

        // 기본 콜백은 데이터를 렌더 스레드에 넘긴 시점(곡이 실제로 끝나기 전)에 불린다. 다 재생된 뒤에 넘어간다.
        playerNode.scheduleSegment(
            file,
            startingFrame: startFrame,
            frameCount: remaining,
            at: nil,
            completionCallbackType: .dataPlayedBack
        ) { [weak self] _ in
            Task { @MainActor in
                guard let self, self.scheduleGeneration == generation else { return }
                self.advanceOnCompletion()
            }
        }
        return .scheduled
    }

    private func advanceOnCompletion() {
        guard isPlayingIntent else { return }
        playIfIntended(schedulePlayable(from: currentIndex + 1, startingSeconds: 0))
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
        // 출력 장치의 채널 수·샘플레이트가 바뀌면 엔진이 스스로 멈춘다. 다시 켜는 것은 앱의 몫이다.
        configurationChangeObserver = NotificationCenter.default.addObserver(
            forName: .AVAudioEngineConfigurationChange,
            object: engine,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in self?.handleConfigurationChange() }
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

    private func handleConfigurationChange() {
        mcLog.info("engine configuration changed → restart at \(self.lastKnownPosition)")
        ensureEngineRunning()
        guard currentFile != nil else { return }
        playIfIntended(schedulePlayable(from: currentIndex, startingSeconds: lastKnownPosition))
        broadcast()
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
        if engine.isRunning { lastKnownPosition = snapshot.positionSeconds }
        nowPlaying.update(snapshot: snapshot)
        for continuation in continuations.values {
            continuation.yield(snapshot)
        }
    }

    private func currentSnapshot() -> MediaControllerSnapshot {
        let item = currentItem
        let isPlaying = isPlayingNow
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
