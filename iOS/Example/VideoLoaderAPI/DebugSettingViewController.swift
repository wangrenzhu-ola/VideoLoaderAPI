//
//  DebugSettingViewController.swift
//  TestVideoLoader
//
//  Created by wushengtao on 2023/10/12.
//

import UIKit


struct DebugSettingInfo {
    var title: String = ""
    var details: [String] = []
    var defaultSelectedIdxs: [Int] = []
    func selectedIdx() -> Int {
        let idx = UserDefaults.standard.value(forKey: title) as? Int ?? (defaultSelectedIdxs.first ?? 0)
        return idx
    }
    
    func selectedValue() -> Int {
        return defaultSelectedIdxs[selectedIdx()]
    }
}

private let kCellWithIdentifier = "DebugSettingCellIdentifier"

let settingInfoList: [DebugSettingInfo] = [
    DebugSettingInfo(title: "需要秒切加速", 
                     details: ["是", "否"],
                     defaultSelectedIdxs: [0, 1]),
    DebugSettingInfo(title: "视频出图时机",
                     details: ["直接出图", "滑动放手时", "滑动停止时"],
                     defaultSelectedIdxs: [0, 1, 2]),
    DebugSettingInfo(title: "音频出图时机",
                     details: ["直接出声", "滑动放手时", "滑动停止时", "永不"],
                     defaultSelectedIdxs: [0, 1, 2, 100])
]

class DebugSettingViewController: UITableViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .white
        tableView.delegate = self
        tableView.dataSource = self
        self.navigationItem.leftBarButtonItem = UIBarButtonItem(title: "back", style: .done, target: self, action: #selector(onBackAction))
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(false, animated: false)
    }
    
    @objc func onBackAction() {
        navigationController?.popToRootViewController(animated: true)
    }
}

extension DebugSettingViewController {
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return settingInfoList.count
    }
    
    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 60
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        var cell = tableView.dequeueReusableCell(withIdentifier: kCellWithIdentifier)
        if cell == nil {
            cell = UITableViewCell(style: .value1, reuseIdentifier: kCellWithIdentifier)
        }
        let info = settingInfoList[indexPath.row]
        cell?.backgroundColor = .white
        cell?.textLabel?.textColor = .black
        cell?.textLabel?.text = info.title
        cell?.detailTextLabel?.textColor = .gray
        cell?.detailTextLabel?.text = info.details[info.selectedIdx()]
        return cell!
    }
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let info = settingInfoList[indexPath.row]
        let selectedIdx = (info.selectedIdx() + 1) % info.details.count
        UserDefaults.standard.setValue(selectedIdx, forKey: info.title)
        tableView.reloadData()
    }
}
