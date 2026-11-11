import Foundation
import Network
import shared

/// 같은 Wi-Fi 에서 데이터를 보내는 기기를 찾는다. 무엇을 찾을지와 찾은 뒤의 판단은 Kotlin 이 한다.
final class MigrationSourceBrowserBridgeImpl: IosMigrationSourceBrowserBridge {

    func __findService(
        serviceType: String,
        timeoutMillis: Int64,
        completionHandler: @escaping @Sendable (IosResolvedService?, Error?) -> Void
    ) {
        ServiceSearch(serviceType: serviceType) { completionHandler($0, nil) }
            .start(timeout: .milliseconds(Int(timeoutMillis)))
    }
}

/// 한 번의 탐색. 처음 풀린 기기 하나로 끝낸다.
///
/// 끝날 때까지 핸들러가 이 객체를 붙잡아 살려 둔다. [finish] 가 핸들러를 걷어 순환을 끊는다.
private final class ServiceSearch {

    private let queue = DispatchQueue(label: "com.kintmin.JellyTubeIos.migration-browse")
    private let browser: NWBrowser
    private var connections: [NWConnection] = []
    /// 결과 핸들러는 바뀔 때마다 전체 목록을 다시 준다. 같은 기기에 연결을 겹쳐 열지 않는다.
    private var resolving: Set<NWEndpoint> = []
    private var completion: ((IosResolvedService?) -> Void)?

    init(serviceType: String, completion: @escaping (IosResolvedService?) -> Void) {
        // Android NSD 는 끝에 점을 붙인 형태("_jellytube._tcp.")를 쓴다. NWBrowser 는 점 없는 형태만 받는다.
        let type = serviceType.hasSuffix(".") ? String(serviceType.dropLast()) : serviceType
        browser = NWBrowser(for: .bonjour(type: type, domain: nil), using: .tcp)
        self.completion = completion
    }

    func start(timeout: DispatchTimeInterval) {
        browser.browseResultsChangedHandler = { results, _ in
            results.forEach { self.resolve($0.endpoint) }
        }
        browser.stateUpdateHandler = { state in
            if case .failed = state { self.finish(nil) }
        }
        browser.start(queue: queue)
        queue.asyncAfter(deadline: .now() + timeout) { self.finish(nil) }
    }

    /// 서비스 이름을 주소로 푸는 공개 API 가 연결뿐이다. 연결이 서면 상대 주소만 읽고 끊는다.
    private func resolve(_ endpoint: NWEndpoint) {
        guard resolving.insert(endpoint).inserted else { return }
        let parameters = NWParameters.tcp
        // Ktor 에 넘길 주소다. IPv6 는 범위 접미사(%en0)와 괄호 처리가 따라와 IPv4 로 고정한다.
        (parameters.defaultProtocolStack.internetProtocol as? NWProtocolIP.Options)?.version = .v4
        let connection = NWConnection(to: endpoint, using: parameters)
        connections.append(connection)
        connection.stateUpdateHandler = { [weak connection] state in
            guard case .ready = state,
                  case let .hostPort(host, port)? = connection?.currentPath?.remoteEndpoint,
                  case let .ipv4(address) = host
            else { return }
            let text = "\(address)".split(separator: "%").first.map(String.init) ?? "\(address)"
            self.finish(IosResolvedService(host: text, port: Int32(port.rawValue)))
        }
        connection.start(queue: queue)
    }

    private func finish(_ service: IosResolvedService?) {
        guard let completion else { return }
        self.completion = nil
        browser.browseResultsChangedHandler = nil
        browser.stateUpdateHandler = nil
        browser.cancel()
        connections.forEach {
            $0.stateUpdateHandler = nil
            $0.cancel()
        }
        connections.removeAll()
        completion(service)
    }
}
