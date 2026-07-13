import ComposableArchitecture
import Foundation
import shared

struct AudioPlaySettingClient: Sendable {
    var speedFlow: @Sendable () -> AsyncStream<Float>
    var pitchSemitoneFlow: @Sendable () -> AsyncStream<Int>
    var updateSpeed: @Sendable (_ speed: Float) async throws -> Void
    var updatePitchSemitone: @Sendable (_ semitone: Int) async throws -> Void
}

extension AudioPlaySettingClient: DependencyKey {
    static let liveValue = Self.kmp()
}

extension DependencyValues {
    var audioPlaySettingClient: AudioPlaySettingClient {
        get { self[AudioPlaySettingClient.self] }
        set { self[AudioPlaySettingClient.self] = newValue }
    }
}

extension AudioPlaySettingClient {
    static func kmp(
        speedFlowBridge: IosFetchPlaybackSpeedFlowUseCaseBridge = IosAudioPlaySettingUseCaseBridgeKt
            .createIosFetchPlaybackSpeedFlowUseCaseBridge(),
        pitchFlowBridge: IosFetchPlaybackPitchSemitoneFlowUseCaseBridge = IosAudioPlaySettingUseCaseBridgeKt
            .createIosFetchPlaybackPitchSemitoneFlowUseCaseBridge(),
        updateSpeedBridge: IosUpdatePlaybackSpeedUseCaseBridge = IosAudioPlaySettingUseCaseBridgeKt
            .createIosUpdatePlaybackSpeedUseCaseBridge(),
        updatePitchBridge: IosUpdatePlaybackPitchSemitoneUseCaseBridge = IosAudioPlaySettingUseCaseBridgeKt
            .createIosUpdatePlaybackPitchSemitoneUseCaseBridge()
    ) -> Self {
        Self(
            speedFlow: {
                AsyncStream { continuation in
                    let task = Task {
                        do {
                            for try await value in speedFlowBridge.invoke() {
                                continuation.yield(value.floatValue)
                            }
                        } catch {}
                        continuation.finish()
                    }
                    continuation.onTermination = { _ in task.cancel() }
                }
            },
            pitchSemitoneFlow: {
                AsyncStream { continuation in
                    let task = Task {
                        do {
                            for try await value in pitchFlowBridge.invoke() {
                                continuation.yield(Int(truncating: value))
                            }
                        } catch {}
                        continuation.finish()
                    }
                    continuation.onTermination = { _ in task.cancel() }
                }
            },
            updateSpeed: { speed in
                _ = try await updateSpeedBridge.invoke(speed: speed)
            },
            updatePitchSemitone: { semitone in
                _ = try await updatePitchBridge.invoke(semitone: Int32(semitone))
            }
        )
    }
}
