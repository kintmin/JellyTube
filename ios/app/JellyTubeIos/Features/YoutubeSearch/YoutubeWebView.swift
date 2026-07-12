import SwiftUI
import WebKit

struct YoutubeWebView: UIViewRepresentable {
    let url: URL
    let onCurrentUrlChanged: (URL) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(onCurrentUrlChanged: onCurrentUrlChanged)
    }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        context.coordinator.attach(to: webView)
        webView.load(URLRequest(url: url))
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        context.coordinator.onCurrentUrlChanged = onCurrentUrlChanged
        guard webView.url != url else { return }
        webView.load(URLRequest(url: url))
    }

    static func dismantleUIView(_ webView: WKWebView, coordinator: Coordinator) {
        coordinator.detach()
    }

    final class Coordinator {
        var onCurrentUrlChanged: (URL) -> Void
        private var urlObservation: NSKeyValueObservation?

        init(onCurrentUrlChanged: @escaping (URL) -> Void) {
            self.onCurrentUrlChanged = onCurrentUrlChanged
        }

        func attach(to webView: WKWebView) {
            urlObservation = webView.observe(\.url, options: [.new]) { [weak self] _, change in
                guard let self, let newUrl = change.newValue, let url = newUrl else { return }
                self.onCurrentUrlChanged(url)
            }
        }

        func detach() {
            urlObservation?.invalidate()
            urlObservation = nil
        }
    }
}
