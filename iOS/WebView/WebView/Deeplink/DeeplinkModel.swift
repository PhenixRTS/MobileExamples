//
//  Copyright 2020 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import Foundation

public struct DeeplinkModel: DeeplinkModelProvider {
    var url: String?

    public init?(components: URLComponents) {
        if let string = components.queryItems?.first(where: { $0.name == "url" })?.value {
            url = string

            if let fragment = components.fragment {
                // We need to append the fragment to the end of the received url, because the web view channel links look like:
                // https://phenixrts.com/webview/?url=https://phenixrts.com/channel/#test
                // and URLComponents thinks that the "#test" fragment belongs to the main url, not to the "url" query item.
                url?.append("#\(fragment)")
            }
        }
    }
}
