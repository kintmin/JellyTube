import XCTest
@testable import JellyTubeIos

final class PythonExecutorBridgeImplTests: XCTestCase {

    func test_여러_스레드에서_동시에_음차해도_모두_같은_결과를_돌려준다() {
        let bridge = PythonExecutorBridgeImpl()
        let lyrics = Array(repeating: "夜に駆ける きっと大丈夫", count: 50).joined(separator: "\n")
        let callCount = 8
        let finished = expectation(description: "음차 완료")
        finished.expectedFulfillmentCount = callCount
        let lock = NSLock()
        var results: [String] = []

        for _ in 0..<callCount {
            bridge.__transliterateToKorean(text: lyrics) { result, error in
                XCTAssertNil(error)
                lock.lock()
                results.append(result ?? "")
                lock.unlock()
                finished.fulfill()
            }
        }

        wait(for: [finished], timeout: 60)
        XCTAssertEqual(Set(results).count, 1)
        XCTAssertTrue(results[0].hasPrefix("요루 니 카케 루 킷토 다이죠우부\n"))
    }
}
