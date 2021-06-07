//
//  Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import UIKit

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Setup deeplink
        let deeplink = makeDeeplinkIfNeeded(launchOptions)

        // Setup view controller
        if let url = deeplink?.url, let vc = window?.rootViewController as? ViewController {
            vc.set(url: url)
        }

        return true
    }

    func application(_ application: UIApplication, continue userActivity: NSUserActivity, restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void) -> Bool {
        guard let deeplink = makeDeeplinkIfNeeded(userActivity) else {
            return false
        }

        if let url = deeplink.url, let vc = window?.rootViewController as? ViewController {
            vc.set(url: url)
            vc.loadUrl()
            return true
        }

        return false
    }

    private func makeDeeplinkIfNeeded(_ launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> DeeplinkModel? {
        if let options = launchOptions?[.userActivityDictionary] as? [AnyHashable: Any] {
            if let userActivity = options[UIApplication.LaunchOptionsKey.userActivityKey] as? NSUserActivity {
                return makeDeeplinkIfNeeded(userActivity)
            }
        }

        return nil
    }

    private func makeDeeplinkIfNeeded(_ userActivity: NSUserActivity) -> DeeplinkModel? {
        if userActivity.activityType == NSUserActivityTypeBrowsingWeb {
            if let url = userActivity.webpageURL {
                let service = DeeplinkService<DeeplinkModel>(url: url)
                let deeplink = service?.decode()
                return deeplink
            }
        }

        return nil
    }
}

extension UIApplication.LaunchOptionsKey {
    static let userActivityKey = "UIApplicationLaunchOptionsUserActivityKey"
}
