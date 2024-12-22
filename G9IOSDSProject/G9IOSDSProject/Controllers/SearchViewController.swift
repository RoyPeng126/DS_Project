import UIKit

class SearchViewController: UIViewController {
    private let searchTextField = UITextField()
    private let searchButton = UIButton()
    private let resultTextView = UITextView()
    private let queryService = GoogleQueryService()

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .white

        let testLabel = UILabel()
        testLabel.text = "App Loaded Successfully"
        testLabel.textAlignment = .center
        testLabel.frame = view.bounds
        view.addSubview(testLabel)
    }


    private func setupUI() {
        view.backgroundColor = .white

        searchTextField.frame = CGRect(x: 20, y: 100, width: view.frame.width - 40, height: 40)
        searchTextField.borderStyle = .roundedRect
        searchTextField.placeholder = "Enter keyword"
        view.addSubview(searchTextField)

        searchButton.frame = CGRect(x: 20, y: 160, width: view.frame.width - 40, height: 40)
        searchButton.setTitle("Search", for: .normal)
        searchButton.setTitleColor(.white, for: .normal)
        searchButton.backgroundColor = .systemBlue
        searchButton.layer.cornerRadius = 5
        searchButton.addTarget(self, action: #selector(onSearchButtonTapped), for: .touchUpInside)
        view.addSubview(searchButton)

        resultTextView.frame = CGRect(x: 20, y: 220, width: view.frame.width - 40, height: view.frame.height - 240)
        resultTextView.isEditable = false
        resultTextView.layer.borderColor = UIColor.lightGray.cgColor
        resultTextView.layer.borderWidth = 1
        resultTextView.layer.cornerRadius = 5
        view.addSubview(resultTextView)
    }

    @objc private func onSearchButtonTapped() {
        guard let query = searchTextField.text, !query.isEmpty else {
            displayAlert(message: "Please enter a keyword.")
            return
        }

        queryService.performSearch(with: query) { [weak self] result in
            switch result {
            case .success(let pages):
                self?.displayResults(pages)
            case .failure(let error):
                self?.displayAlert(message: "Error: \(error.localizedDescription)")
            }
        }
    }

    private func displayResults(_ pages: [SearchResultPage]) {
        let results = pages.map { "\($0.title)\n\($0.url)\n" }.joined(separator: "\n")
        resultTextView.text = results
    }

    private func displayAlert(message: String) {
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }
}
