import Foundation
import Network
import shared

/// 같은 Wi-Fi 에서 데이터를 보내는 기기를 찾는다. 무엇을 찾을지와 찾은 뒤의 판단은 Kotlin 이 한다.
final class MigrationSourceBrowserBridgeImpl: IosMigrationSourceBrowserBridge {

    func __browse(
        serviceType: String,
        settleMillis: Int64,
        timeoutMillis: Int64,
        completionHandler: @escaping @Sendable ([IosBrowsedService]?, Error?) -> Void
    ) {
        ServiceBrowse(type: bonjourType(serviceType)) { completionHandler($0, nil) }
            .start(settle: .milliseconds(Int(settleMillis)), timeout: .milliseconds(Int(timeoutMillis)))
    }

    func __resolve(
        serviceType: String,
        name: String,
        timeoutMillis: Int64,
        completionHandler: @escaping @Sendable (IosResolvedService?, Error?) -> Void
    ) {
        let endpoint = NWEndpoint.service(name: name, type: bonjourType(serviceType), domain: "local.", interface: nil)
        ServiceResolve(endpoint: endpoint) { completionHandler($0, nil) }
            .start(timeout: .milliseconds(Int(timeoutMillis)))
    }
}

/// Android NSD 는 끝에 점을 붙인 형태("_jellytube-mig._tcp.")를 쓴다. NWBrowser 는 점 없는 형태만 받는다.
private func bonjourType(_ serviceType: String) -> String {
    serviceType.hasSuffix(".") ? String(serviceType.dropLast()) : serviceType
}

/// 한 번의 탐색. 끝날 때까지 핸들러가 이 객체를 붙잡아 살려 둔다. [finish] 가 핸들러를 걷어 순환을 끊는다.
private final class ServiceBrowse {

    private let queue = DispatchQueue(label: "com.kintmin.JellyTubeIos.migration-browse")
    private let browser: NWBrowser
    private var latest: [IosBrowsedService] = []
    private var settleWork: DispatchWorkItem?
    private var completion: (([IosBrowsedService]) -> Void)?

    init(type: String, completion: @escaping ([IosBrowsedService]) -> Void) {
        browser = NWBrowser(for: .bonjourWithTXTRecord(type: type, domain: nil), using: .tcp)
        self.completion = completion
    }

    func start(settle: DispatchTimeInterval, timeout: DispatchTimeInterval) {
        // 결과 핸들러는 바뀔 때마다 전체 목록을 다시 준다. TXT 가 늦게 풀려도 바뀐 것으로 온다.
        browser.browseResultsChangedHandler = { results, _ in
            self.latest = Self.services(from: results)
            self.settleWork?.cancel()
            guard !self.latest.isEmpty else { return }
            let work = DispatchWorkItem { self.finish(self.latest) }
            self.settleWork = work
            self.queue.asyncAfter(deadline: .now() + settle, execute: work)
        }
        browser.stateUpdateHandler = { state in
            if case .failed = state { self.finish([]) }
        }
        browser.start(queue: queue)
        queue.asyncAfter(deadline: .now() + timeout) { self.finish(self.latest) }
    }

    private static func services(from results: Set<NWBrowser.Result>) -> [IosBrowsedService] {
        results.compactMap { result in
            guard case let .service(name, _, _, _) = result.endpoint else { return nil }
            guard case let .bonjour(record) = result.metadata else { return IosBrowsedService(name: name, txtRecord: [:]) }
            return IosBrowsedService(name: name, txtRecord: record.dictionary)
        }
    }

    private func finish(_ services: [IosBrowsedService]) {
        guard let completion else { return }
        self.completion = nil
        settleWork?.cancel()
        settleWork = nil
        browser.browseResultsChangedHandler = nil
        browser.stateUpdateHandler = nil
        browser.cancel()
        completion(services)
    }
}

/// 서비스 이름을 주소로 푸는 공개 API 가 연결뿐이다. 연결이 서면 상대 주소만 읽고 끊는다.
private final class ServiceResolve {

    private let queue = DispatchQueue(label: "com.kintmin.JellyTubeIos.migration-resolve")
    private let connection: NWConnection
    private var completion: ((IosResolvedService?) -> Void)?

    init(endpoint: NWEndpoint, completion: @escaping (IosResolvedService?) -> Void) {
        let parameters = NWParameters.tcp
        // Ktor 에 넘길 주소다. IPv6 는 범위 접미사(%en0)와 괄호 처리가 따라와 IPv4 로 고정한다.
        (parameters.defaultProtocolStack.internetProtocol as? NWProtocolIP.Options)?.version = .v4
        connection = NWConnection(to: endpoint, using: parameters)
        self.completion = completion
    }

    func start(timeout: DispatchTimeInterval) {
        connection.stateUpdateHandler = { state in
            switch state {
            case .ready:
                guard case let .hostPort(host, port)? = self.connection.currentPath?.remoteEndpoint,
                      case let .ipv4(address) = host
                else { return self.finish(nil) }
                let text = "\(address)".split(separator: "%").first.map(String.init) ?? "\(address)"
                self.finish(IosResolvedService(host: text, port: Int32(port.rawValue)))
            case .failed:
                self.finish(nil)
            default:
                break
            }
        }
        connection.start(queue: queue)
        queue.asyncAfter(deadline: .now() + timeout) { self.finish(nil) }
    }

    private func finish(_ service: IosResolvedService?) {
        guard let completion else { return }
        self.completion = nil
        connection.stateUpdateHandler = nil
        connection.cancel()
        completion(service)
    }
}
