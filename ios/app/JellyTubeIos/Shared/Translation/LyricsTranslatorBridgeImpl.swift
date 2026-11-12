import Foundation
import SwiftUI
import Translation
import shared

enum LyricsTranslatorBridgeError: LocalizedError {
    case missingResponse

    var errorDescription: String? {
        switch self {
        case .missingResponse: return "번역 결과가 일부 비어 있습니다"
        }
    }
}

/// 가사를 Apple `Translation` 으로 번역한다. 줄 나누기와 빈 줄 판단은 Kotlin(`IosLyricsTranslator`)이 한다.
///
/// `TranslationSession` 은 `.translationTask` 가 붙은 뷰에서만 얻을 수 있다. 그래서 요청은
/// [LyricsTranslationHost] 에 쌓고, 루트 뷰에 붙은 [LyricsTranslationHostModifier] 가 세션을 받아 처리한다.
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

/// 번역 요청을 한 번에 하나씩 세션에 태운다.
///
/// [configuration] 이 바뀔 때마다 `.translationTask` 가 새 세션으로 [run] 을 부른다.
/// 언어 모델이 없으면 그 세션을 쓰는 순간 시스템이 다운로드 시트를 띄운다.
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
    private var current: Job?

    func enqueue(
        lines: [String],
        sourceLanguage: String,
        completion: @escaping (Result<[String], Error>) -> Void
    ) {
        guard !lines.isEmpty else {
            completion(.success([]))
            return
        }
        pending.append(Job(lines: lines, sourceLanguage: sourceLanguage, completion: completion))
        startNextIfIdle()
    }

    func run(_ session: TranslationSession) async {
        guard let job = current else { return }

        let result: Result<[String], Error>
        do {
            let requests = job.lines.enumerated().map { index, line in
                TranslationSession.Request(sourceText: line, clientIdentifier: String(index))
            }
            let responses = try await session.translations(from: requests)
            // 응답 순서를 믿지 않고 clientIdentifier 로 요청 자리에 되맞춘다.
            var translated = [String?](repeating: nil, count: job.lines.count)
            for response in responses {
                guard let id = response.clientIdentifier, let index = Int(id) else { continue }
                translated[index] = response.targetText
            }
            let lines = translated.compactMap { $0 }
            result = lines.count == job.lines.count
                ? .success(lines)
                : .failure(LyricsTranslatorBridgeError.missingResponse)
        } catch {
            result = .failure(error)
        }

        current = nil
        job.completion(result)
        startNextIfIdle()
    }

    private func startNextIfIdle() {
        guard current == nil, !pending.isEmpty else { return }
        let job = pending.removeFirst()
        current = job

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
}

/// 루트 뷰에 붙어 [LyricsTranslationHost] 에 세션을 건넨다. 루트 뷰는 내려가지 않으므로 세션 창구가 늘 열려 있다.
struct LyricsTranslationHostModifier: ViewModifier {

    @ObservedObject private var host = LyricsTranslationHost.shared

    func body(content: Content) -> some View {
        content.translationTask(host.configuration) { session in
            await host.run(session)
        }
    }
}
