import UIKit

class SearchViewController: UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .white
        
        let searchTextField = UITextField(frame: CGRect(x: 20, y: 100, width: 300, height: 40))
        searchTextField.borderStyle = .roundedRect
        searchTextField.placeholder = "Enter keyword"
        view.addSubview(searchTextField)
        
        let searchButton = UIButton(frame: CGRect(x: 20, y: 160, width: 100, height: 40))
        searchButton.setTitle("Search", for: .normal)
        searchButton.setTitleColor(.blue, for: .normal)
        searchButton.addTarget(self, action: #selector(onSearchButtonTapped), for: .touchUpInside)
        view.addSubview(searchButton)
    }

    @objc func onSearchButtonTapped() {
        print("Search button tapped")
        // Connect this action to call search logic
    }


    }

