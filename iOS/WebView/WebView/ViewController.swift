//
//  Copyright 2020 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import os.log
import UIKit
import WebKit

class ViewController: UIViewController, WKUIDelegate {
    @IBOutlet private var webView: WKWebView!
    @IBOutlet private var urlTextField: UITextField!

    private(set) var url: String? = "https://phenixrts.com/channel/#webViewDemo"

    override func viewDidLoad() {
        super.viewDidLoad()

        urlTextField.text = url

        webView.configuration.allowsInlineMediaPlayback = true
        webView.configuration.mediaTypesRequiringUserActionForPlayback = []
        webView.uiDelegate = self

        loadUrl()
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
