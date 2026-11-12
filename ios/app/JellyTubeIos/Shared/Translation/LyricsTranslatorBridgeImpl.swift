import Foundation
import SwiftUI
import Translation
import shared

enum LyricsTranslatorBridgeError: LocalizedError {
    case missingResponse
    case appInBackground

    var errorDescription: String? {
        switch self {
        case .missingResponse: return "번역 결과가 일부 비어 있습니다"
        case .appInBackground: return "앱이 백그라운드에 있어 번역할 수 없습니다"
        }
    }
}

/// `TranslationSession` 은 `.translationTask` 가 붙은 뷰에서만 나온다. 요청은 [LyricsTranslationHost] 에 쌓는다.
final class LyricsTranslatorBridgeImpl: IosLyricsTranslatorBridge {

    func __translateToKorean(
        lines: [String],
        sourceLanguage: String,
        completionHandler: @escaping @Sendable ([String]?, Error?) -> Void
    ) {
        Task { @MainActor in
            LyricsTranslationHost.shared.enqueue(lines: lines, sourceLanguage: sourceLanguage) { result in
                switch result {
                case .success(let translated): completionHandler(translated, nil)
                case .failure(let error): completionHandler(nil, error)
                }
            }
        }
    }
}

/// [configuration] 이 바뀔 때마다 `.translationTask` 가 새 세션으로 [run] 을 부른다.
/// 세션은 화면이 떠 있어야 나오므로 백그라운드에서는 기다리지 않고 실패시킨다.
@MainActor
final class LyricsTranslationHost: ObservableObject {

    static let shared = LyricsTranslationHost()

    @Published private(set) var configuration: TranslationSession.Configuration?

    private struct Job {
        let lines: [String]
        let sourceLanguage: String
        let completion: (Result<[String], Error>) -> Void
    }

    private var pending: [Job] = []
    /// configuration 을 걸어 두고 세션을 기다리는 작업.
    private var waiting: Job?
    private var isTranslating = false

    private init() {
        _ = NotificationCenter.default.addObserver(
            forName: UIApplication.didEnterBackgroundNotification,
            object: nil,
            queue: .main
        ) { _ in
            MainActor.assumeIsolated { LyricsTranslationHost.shared.failQueued() }
        }
    }

    func enqueue(
        lines: [String],
        sourceLanguage: String,
        completion: @escaping (Result<[String], Error>) -> Void
    ) {
        pending.append(Job(lines: lines, sourceLanguage: sourceLanguage, completion: completion))
        startNextIfIdle()
    }

    func run(_ session: TranslationSession) async {
        // 창이 여럿이면 창마다 붙은 task 가 같은 세션 신호를 받는다. 먼저 가져간 쪽만 처리한다.
        guard let job = waiting else { return }
        waiting = nil
        isTranslating = true

        let result: Result<[String], Error>
        do {
            let requests = job.lines.enumerated().map { index, line in
                TranslationSession.Request(sourceText: line, clientIdentifier: String(index))
            }
            let responses = try await session.translations(from: requests)
            // 응답 순서를 믿지 않고 clientIdentifier 로 요청 자리에 되맞춘다.
            var translated = [String?](repeating: nil, count: job.lines.count)
            for response in responses {
                guard let id = response.clientIdentifier, let index = Int(id), translated.indices.contains(index)
                else { continue }
                translated[index] = response.targetText
            }
            guard !translated.contains(nil) else { throw LyricsTranslatorBridgeError.missingResponse }
            result = .success(translated.compactMap { $0 })
        } catch {
            result = .failure(error)
        }

        isTranslating = false
        job.completion(result)
        startNextIfIdle()
    }

    private func startNextIfIdle() {
        guard waiting == nil, !isTranslating, !pending.isEmpty else { return }
        guard UIApplication.shared.applicationState != .background else {
            failQueued()
            return
        }
        let job = pending.removeFirst()
        waiting = job

        let next = TranslationSession.Configuration(
            source: Locale.Language(identifier: job.sourceLanguage),
            target: Locale.Language(identifier: "ko")
        )
        // 같은 언어쌍이면 값이 같아 `.translationTask` 가 다시 돌지 않는다. invalidate 로 판을 바꾼다.
        if configuration == next {
            configuration?.invalidate()
        } else {
            configuration = next
        }
    }

    /// 아직 세션을 받지 못한 작업을 전부 실패시킨다. 번역 중인 작업은 세션이 끝을 낸다.
    private func failQueued() {
        let jobs = (waiting.map { [$0] } ?? []) + pending
        waiting = nil
        pending.removeAll()
        jobs.forEach { $0.completion(.failure(LyricsTranslatorBridgeError.appInBackground)) }
    }
}

/// 루트 뷰에 붙인다. 붙은 뷰가 내려가면 쌓인 번역 요청이 끝나지 않는다.
struct LyricsTranslationHostModifier: ViewModifier {

    @ObservedObject private var host = LyricsTranslationHost.shared

    func body(content: Content) -> some View {
        content.translationTask(host.configuration) { session in
            await host.run(session)
        }
    }
}
