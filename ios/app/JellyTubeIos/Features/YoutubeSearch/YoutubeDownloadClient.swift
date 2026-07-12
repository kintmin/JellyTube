import ComposableArchitecture
import Foundation
import shared

struct YoutubeDownloadClient: Sendable {
    var download: @Sendable (_ url: String) async throws -> Void
}

extension YoutubeDownloadClient: DependencyKey {
    static let liveValue = Self.kmp()
}

extension DependencyValues {
    var youtubeDownloadClient: YoutubeDownloadClient {
        get { self[YoutubeDownloadClient.self] }
        set { self[YoutubeDownloadClient.self] = newValue }
    }
}

extension YoutubeDownloadClient {
    static func kmp(
        downloadBridge: IosDownloadAudioMediaUseCaseBridge =
            IosAudioMediaUseCaseBridgeKt.createIosDownloadAudioMediaUseCaseBridge()
    ) -> Self {
        Self(
            download: { url in
                try await downloadBridge.invoke(downloadUrl: url)
            }
        )
    }
}
