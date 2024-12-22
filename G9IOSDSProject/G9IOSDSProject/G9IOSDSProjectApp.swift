//
//  G9IOSDSProjectApp.swift
//  G9IOSDSProject
//
//  Created by 彭宇承 on 2024/12/22.
//

import SwiftUI
import UIKit // 必須導入 UIKit，否則找不到 ViewController

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

    func updateUIViewController(_ uiViewController: SearchViewController, context: Context) {
        // 不需要額外操作
    }
}
