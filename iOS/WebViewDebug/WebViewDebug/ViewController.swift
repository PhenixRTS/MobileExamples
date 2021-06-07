//
//  Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import os.log
import UIKit
import WebKit

class ViewController: UIViewController, WKUIDelegate {
    @IBOutlet private var webView: WKWebView!
    @IBOutlet private var urlTextField: UITextField!
    @IBOutlet private var consoleLogsLabel: UITextView!
    @IBOutlet private var streamIdLabel: UILabel!

    // This is url with token that will point to local environment. The toke should expires at 31 May 2031
    private(set) var url: String? = "https://dl.phenixrts.com/JsSDK/2021.0.30/examples/channel-viewer-plain.html?token=DIGEST:eyJhcHBsaWNhdGlvbklkIjoidGVzdCIsImRpZ2VzdCI6Ik1jT0s3TjNoMC81QTF2NFN4OHYraGd5NmFWeDlSUHZZOVl4OHIyam1qWlRhN3BHdDJFN1BYM2xaSkpPR20yQmNFODFIeWlxaUVDN09PTnczcEZmazJBPT0iLCJ0b2tlbiI6IntcInVyaVwiOlwiaHR0cHM6Ly9sb2NhbC5waGVuaXhydHMuY29tOjg0NDNcIixcImV4cGlyZXNcIjoxOTM4MDA1MjYxMzUxLFwicmVxdWlyZWRUYWdcIjpcImNoYW5uZWxBbGlhczpjaGFubmVsXCJ9In0="

    override func viewDidLoad() {
        super.viewDidLoad()

        urlTextField.text = url

        webView.configuration.allowsInlineMediaPlayback = true
        webView.configuration.mediaTypesRequiringUserActionForPlayback = []
        webView.uiDelegate = self
        webView.navigationDelegate = self

        // Inject JS to capture web console.log output
        let source = "function captureLog(msg) { window.webkit.messageHandlers.logHandler.postMessage(msg); } window.console.log = captureLog;"
        let script = WKUserScript(source: source, injectionTime: .atDocumentEnd, forMainFrameOnly: false)
        webView.configuration.preferences.javaScriptEnabled = true
        webView.configuration.userContentController.addUserScript(script)
        webView.configuration.userContentController.add(self, name: "logHandler")
    }

    func set(url: String) {
        self.url = url
        urlTextField?.text = url
    }

    func loadUrl() {
        guard let urlText = url, let url = URL(string: urlText) else {
            os_log(.debug, log: .ui, "Incorrect url: %{PRIVATE}s", self.url ?? "-")
            return
        }

        os_log(.debug, log: .ui, "Loading url: %{PRIVATE}s", url.absoluteString)
        webView.load(URLRequest(url: url))
    }

    @IBAction
    private func loadUrlButtonTapped(_ sender: Any) {
        guard let urlText = urlTextField.text else {
            return
        }

        url = urlText
        loadUrl()
        view.endEditing(true)
    }
}

extension OSLog {
    private static var subsystem = Bundle.main.bundleIdentifier!

    // MARK: - Application components
    static let ui = OSLog(subsystem: subsystem, category: "Phenix.App.UserInterface")
}

extension ViewController: WKScriptMessageHandler {
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        if message.name == "logHandler", let log = message.body as? String {
            os_log(.debug, log: .ui, "Console log: %{public}s", log)

            consoleLogsLabel.text?.append("\(log)\n")
            extractStreamIdFrom(log)
        }
    }

    func extractStreamIdFrom(_ log: String) {
        if log.contains("Got subscribe response") {
            let logString = log.replacingOccurrences(of: "[EndPoint] [Debug] Got subscribe response", with: "")

            guard let openingRange = logString.ranges(of: "[").first else {
                return
            }
            let closingRange = logString.ranges(of: "]")

            let subscribeResponseString = String(
                // We need to go not by the last one "]" but the second from the end becouse we have end of the log loking like "in [${time}] ms"
                describing: logString[openingRange.upperBound ..< closingRange[closingRange.count - 2].lowerBound]
            )

            guard let data = subscribeResponseString.data(using: .utf8) else {
                return
            }

            if let responseJson = try? JSONSerialization.jsonObject(with: data, options: .allowFragments) as? [String: Any],
               let stream = responseJson["stream"] as? [String: Any],
               let streamId = stream["streamId"] as? String {
                        streamIdLabel.text = streamId
            }
        }
    }
}

extension ViewController: WKNavigationDelegate {
    func webView(_ webView: WKWebView, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        guard let serverTrust = challenge.protectionSpace.serverTrust else {
            completionHandler(.performDefaultHandling, nil)
            return
        }

        let cred = URLCredential(trust: serverTrust)
        completionHandler(.useCredential, cred)
    }
}
