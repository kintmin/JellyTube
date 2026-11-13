import XCTest
@testable import JellyTubeIos

final class PythonExecutorBridgeImplTests: XCTestCase {

    func test_여러_스레드에서_동시에_음차해도_단일_호출과_같은_결과를_돌려준다() throws {
        let lyrics = Array(repeating: "夜に駆ける きっと大丈夫", count: 50).joined(separator: "\n")
        let expected = try PythonRuntime().callFunctionReturningString(
            moduleName: "transliterate_lyrics",
            functionName: "transliterate_to_korean",
            arguments: [lyrics]
        )
        let bridge = PythonExecutorBridgeImpl()
        let callCount = 8
        let finished = expectation(description: "음차 완료")
        finished.expectedFulfillmentCount = callCount
        let results = LockedArray<String>()

        for _ in 0..<callCount {
            bridge.__transliterateToKorean(text: lyrics) { result, error in
                XCTAssertNil(error)
                results.append(result ?? "")
                finished.fulfill()
            }
        }

        wait(for: [finished], timeout: 60)
        XCTAssertEqual(results.values, Array(repeating: expected, count: callCount))
    }
}
