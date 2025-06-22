//
//  SampleApp.swift
//  Sample
//
//  Created by Brahyam Meneses on 20.06.25.
//

import SwiftUI
import Gateway

@main
struct SampleApp: App {
    init() {
        // Initialize the Gateway SDK
        Gateway.shared.configure(config: GatewayConfig(googleCloudProjectNumber: Int64(1235)))
    }
    var body: some Scene {
        WindowGroup {
            NavigationView {
                ContentView()
            }
        }
    }
}
