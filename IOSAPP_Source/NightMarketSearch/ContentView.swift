import SwiftUI

extension UIApplication {
    func endEditing() {
        sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }
}

struct ContentView: View {
    @StateObject private var viewModel = SearchViewModel()
    @State private var currentPage: Int = 1 // 當前頁數
    private let resultsPerPage: Int = 10 // 每頁顯示的結果數

    var body: some View {
        NavigationView {
            VStack(spacing: 16) {
                // Title + Subtitle
                Text("NMSL")
                    .font(.system(size: 48, weight: .bold, design: .serif))
                    .foregroundColor(.black)
                    .padding(.top, 32)

                Text("Night Market Search List")
                    .font(.headline)
                    .foregroundColor(.gray)

                // 搜尋欄位
                TextField("Enter your query", text: $viewModel.query)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(.horizontal, 16)

                // 搜尋按鈕
                Button(action: {
                    currentPage = 1 // 每次搜尋重置為第 1 頁
                    viewModel.search()
                    UIApplication.shared.endEditing() // 隱藏鍵盤
                }) {
                    Text("Search")
                        .foregroundColor(.white)
                        .padding()
                        .frame(maxWidth: .infinity)
                        .background(Color.blue)
                        .cornerRadius(8)
                }
                .padding(.horizontal, 16)

                // 搜尋結果與分頁
                if !viewModel.results.isEmpty {
                    ScrollView {
                        VStack(spacing: 16) {
                            // 分頁結果
                            let paginatedResults = getResultsForPage()
                            ForEach(paginatedResults) { result in
                                VStack(alignment: .leading, spacing: 8) {
                                    if let url = URL(string: result.url) {
                                        Link(destination: url) {
                                            Text(result.title)
                                                .font(.headline)
                                                .foregroundColor(.blue) // 明確設定為藍色
                                        }
                                    } else {
                                        Text(result.title)
                                            .font(.headline)
                                            .foregroundColor(.blue) // 明確設定為藍色
                                    }
                                    Text(result.snippet)
                                        .font(.subheadline)
                                        .foregroundColor(.gray) // 明確設定為次要文字顏色
                                    Text("Score: \(result.aggregatedScore)")
                                        .font(.footnote)
                                        .foregroundColor(.black) // 明確設定為黑色
                                        .padding(4)
                                        .background(Color.blue.opacity(0.1))
                                        .cornerRadius(8)
                                }
                                .padding()
                                .background(Color.white.opacity(0.7))
                                .cornerRadius(8)
                                .shadow(radius: 2)
                            }

                            // 分頁按鈕
                            PaginationView(
                                currentPage: $currentPage,
                                totalResults: viewModel.results.count,
                                resultsPerPage: resultsPerPage
                            )
                            
                            // Related Keywords - 放在分頁按鈕下方
                            if !viewModel.resultTexts.isEmpty {
                                VStack(alignment: .leading, spacing: 8) {
                                    Text("Related Keywords")
                                        .font(.headline)
                                        .foregroundColor(.black) // 明確設定標題顏色
                                        .padding(.top, 8)

                                    ForEach(viewModel.resultTexts, id: \.self) { text in
                                        Text(text)
                                            .foregroundColor(.blue)
                                            .padding(2)
                                    }
                                }
                                .padding(.horizontal, 16)
                            } else {
                                Text("No related keywords available.")
                                    .foregroundColor(.gray)
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                } else {
                    Text("Search for night market delicacies~")
                        .font(.subheadline)
                        .foregroundColor(.gray)
                }

                Spacer()
            }
            .navigationBarHidden(true)
            .background(Color(white: 0.95))
        }
    }

    // 根據目前頁數取得對應的結果
    private func getResultsForPage() -> [SearchResult] {
        let startIndex = (currentPage - 1) * resultsPerPage
        let endIndex = min(startIndex + resultsPerPage, viewModel.results.count)
        return Array(viewModel.results[startIndex..<endIndex])
    }
}

struct PaginationView: View {
    @Binding var currentPage: Int
    let totalResults: Int
    let resultsPerPage: Int

    var totalPages: Int {
        (totalResults + resultsPerPage - 1) / resultsPerPage
    }

    var body: some View {
        HStack(spacing: 8) {
            // 上一頁按鈕
            Button(action: {
                if currentPage > 1 {
                    currentPage -= 1
                }
            }) {
                Text("Previous")
                    .padding(8)
                    .background(currentPage > 1 ? Color.blue : Color.gray)
                    .foregroundColor(.white)
                    .cornerRadius(8)
            }
            .disabled(currentPage <= 1)

            // 分頁按鈕
            ForEach(1...totalPages, id: \.self) { page in
                Button(action: {
                    currentPage = page
                }) {
                    Text("\(page)")
                        .padding(8)
                        .background(page == currentPage ? Color.blue : Color.gray.opacity(0.2))
                        .foregroundColor(page == currentPage ? .white : .black)
                        .cornerRadius(8)
                }
            }

            // 下一頁按鈕
            Button(action: {
                if currentPage < totalPages {
                    currentPage += 1
                }
            }) {
                Text("Next")
                    .padding(8)
                    .background(currentPage < totalPages ? Color.blue : Color.gray)
                    .foregroundColor(.white)
                    .cornerRadius(8)
            }
            .disabled(currentPage >= totalPages)
        }
        .padding(.top, 16)
    }
}
 
