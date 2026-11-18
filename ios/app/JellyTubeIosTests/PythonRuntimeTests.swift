import XCTest
@testable import JellyTubeIos

final class PythonRuntimeTests: XCTestCase {

    func test_여러_스레드에서_동시에_시퀀스를_돌려받아도_단일_호출과_같다() throws {
        let runtime = PythonRuntime()
        let words = (0..<2000).map { "w\($0)" }.joined(separator: " ")
        let call = { try runtime.callFunction(moduleName: "shlex", functionName: "split", arguments: [words]) }
        let expected = try call()
        let callCount = 8
        let results = LockedArray<[String]>()

        DispatchQueue.concurrentPerform(iterations: callCount) { _ in
            results.append((try? call()) ?? [])
        }

        XCTAssertEqual(expected.count, 2000)
        XCTAssertEqual(results.values, Array(repeating: expected, count: callCount))
    }
}

final class LockedArray<Element>: @unchecked Sendable {
    private let lock = NSLock()
    private var elements: [Element] = []

    func append(_ element: Element) {
        lock.lock()
        defer { lock.unlock() }
        elements.append(element)
    }

    var values: [Element] {
        lock.lock()
        defer { lock.unlock() }
        return elements
    }
}
