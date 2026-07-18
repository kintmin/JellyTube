import Foundation

/// SKIE가 만든 Flow(AsyncSequence)를 취소 처리까지 포함해 AsyncThrowingStream으로 감싼다.
///
/// 각 `~Client`가 반복하던 Task / continuation / onTermination 보일러플레이트를 한 곳으로 모은다.
/// 원소 매핑(KMP 도메인 타입 → Swift `~Item`)은 호출부가 `map`으로 넘긴다.
enum KmpStream {
    static func of<Source: AsyncSequence, Element>(
        _ source: @escaping () -> Source,
        map: @escaping (Source.Element) -> Element
    ) -> AsyncThrowingStream<Element, Error> {
        AsyncThrowingStream { continuation in
            let task = Task {
                do {
                    for try await value in source() {
                        continuation.yield(map(value))
                    }
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }
}
