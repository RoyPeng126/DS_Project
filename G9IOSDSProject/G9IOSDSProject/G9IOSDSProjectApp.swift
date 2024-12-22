//
//  G9IOSDSProjectApp.swift
//  G9IOSDSProject
//
//  Created by 彭宇承 on 2024/12/22.
//

import SwiftUI

@main
struct G9IOSDSProjectApp: App {
    var body: some Scene {
        WindowGroup {
            SearchViewControllerWrapper()
        }
    }
}

struct SearchViewControllerWrapper: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> SearchViewController {
        return SearchViewController()
    }

    func updateUIViewController(_ uiViewController: SearchViewController, context: Context) {}
}
